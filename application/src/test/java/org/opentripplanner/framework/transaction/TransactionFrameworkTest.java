package org.opentripplanner.framework.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.transaction._model.a.A;
import org.opentripplanner.framework.transaction._model.a.ARepository;
import org.opentripplanner.framework.transaction._model.a.ARepositoryLifecycle;
import org.opentripplanner.framework.transaction._model.a.ASnapshot;
import org.opentripplanner.framework.transaction._model.b.B;
import org.opentripplanner.framework.transaction._model.b.BRepository;
import org.opentripplanner.framework.transaction._model.b.BSnapshot;
import org.opentripplanner.framework.transaction._model.base.AbstractRepository;
import org.opentripplanner.framework.transaction._model.base.Entity;
import org.opentripplanner.framework.transaction._model.event.AEventHandler;
import org.opentripplanner.framework.transaction._model.event.BEventHandler;
import org.opentripplanner.framework.transaction._model.event.CreateNewThingDomainEvent;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.api.TransactionScope;
import org.opentripplanner.framework.transaction.api.WriteContext;
import org.opentripplanner.framework.transaction.internal.TransactionFactory;

public class TransactionFrameworkTest {

  public static final String PUBLISH_DOMAIN_EVENT =
    "Create new A & B with id 7 using a domain event";
  private RepositoryRegistry registry;
  private UpdateManager updateManager;
  private RepositoryHandle<ASnapshot, ARepository> aRepoHandler;
  private RepositoryHandle<BSnapshot, BRepository> bRepoHandler;
  private final List<String> eventLog = new ArrayList<>();

  @BeforeEach
  public void setUp() throws Exception {
    // Create 2 repositories that use a slightly different strategy for the life-cycle.
    // - ARepository uses a life-cycle manager and copies everything from the snapshot to the
    //   repository, and from the repository to the snapshot - enabling rollback and true atomic
    //   commits.
    // - BRepository does not use a life-cycle manager and returns the same instance for each
    //   transaction and freezes it into a snapshot when the transaction is committed. This does
    //   not support atomic commits and rollback in case a task fails, but is more memory efficient
    //   since only the freeze action triggers copying the internal data structure.
    ARepository aRepository = new ARepository();
    BRepository bRepository = new BRepository();

    // Add some static data
    A a = new A(1, "A1");
    B b = new B(10, "B1");
    aRepository.add(a);
    bRepository.add(b);

    this.registry = TransactionFactory.createRepositoryRegistry();
    this.aRepoHandler = registry.registerRepository(aRepository, new ARepositoryLifecycle());
    this.bRepoHandler = registry.registerRepositorySnapshot(
      bRepository.freeze(bRepository),
      bRepository
    );
  }

  private void setupUpdateManagerWithAutoCommits() {
    var threadFactory = new ThreadFactoryBuilder().setNameFormat("autoCommit").build();
    updateManager = TransactionFactory.createUpdateManagerWithAtomicCommits(
      getClass().getSimpleName(),
      registry,
      threadFactory
    );
    updateManager.register(new AEventHandler(), aRepoHandler);
    updateManager.register(new BEventHandler(), bRepoHandler);
  }

  /**
   * Use auto-commit and a CountDownLatch to deterministically verify that state is not visible
   * until after the task (and its atomic commit) completes.
   */
  @Test
  public void testHappyDayScenario() throws ExecutionException, InterruptedException {
    setupUpdateManagerWithAutoCommits();
    assertState("Scope(TXN-1)", List.of(1), List.of(10));

    // Block the task via a latch so the main thread can assert the "before" state without racing.
    var blockTask = new CountDownLatch(1);
    var f = updateManager.submit(writeContext -> {
      eventLog.add("Add A3 & B13 using repo");
      writeContext.repository(aRepoHandler).add(new A(3, "Add A3 using repo"));
      writeContext.repository(bRepoHandler).add(new B(13, "Add B13 using repo"));
      awaitUninterruptibly(blockTask);
    });
    // Task is blocked on the latch – state is guaranteed to be unchanged.
    assertState("Scope(TXN-1)", List.of(1), List.of(10));
    blockTask.countDown();
    f.get();
    assertState("Scope(TXN-2)", List.of(1, 3), List.of(10, 13));

    f = processTask(updateManager, publishNewAUsingADomainEvent());
    f.get();
    assertState("Scope(TXN-3)", List.of(1, 3, 7), List.of(10, 13, 17));

    updateManager.shutdown();

    assertEquals(
      """
      Add A3 & B13 using repo
      Create new A & B with id 7 using a domain event""",
      String.join("\n", eventLog)
    );
  }

  private void setupUpdateManagerWithPeriodicCommits() {
    var threadFactory = new ThreadFactoryBuilder().setNameFormat("autoCommit").build();
    updateManager = TransactionFactory.createUpdateManagerWithPeriodicCommits(
      getClass().getSimpleName(),
      registry,
      threadFactory,
      Duration.ofMillis(40)
    );
    updateManager.register(new AEventHandler(), aRepoHandler);
    updateManager.register(new BEventHandler(), bRepoHandler);
  }

  @Test
  public void testPeriodicCommits() throws ExecutionException, InterruptedException {
    setupUpdateManagerWithPeriodicCommits();
    assertState("Scope(TXN-1)", List.of(1), List.of(10));

    // Task completes before the periodic scheduler fires its first commit.
    var f = processTask(updateManager, publishNewAUsingARepo());
    f.get();
    assertState("Scope(TXN-1)", List.of(1), List.of(10));
    awaitState("Scope(TXN-2)", List.of(1, 3), List.of(10, 13));

    f = processTask(updateManager, publishNewAUsingADomainEvent());
    f.get();
    assertState("Scope(TXN-2)", List.of(1, 3), List.of(10, 13));
    awaitState("Scope(TXN-3)", List.of(1, 3, 7), List.of(10, 13, 17));

    updateManager.shutdown();
    assertEquals(
      """
      Add A3 & B13 using repo
      Create new A & B with id 7 using a domain event""",
      String.join("\n", eventLog)
    );
  }

  /**
   * Demonstrates the rollback contract difference between the two lifecycle strategies:
   * <ul>
   *   <li>ARepository uses copy-on-write via {@code ARepositoryLifecycle}: rollback discards the
   *       in-progress copy, so the next task starts from the last committed snapshot.</li>
   *   <li>BRepository returns {@code this} from {@code copyOnWrite}, so mutations written before
   *       the failure are NOT discarded by rollback and leak into the next committed snapshot.</li>
   * </ul>
   */
  @Test
  public void testRollback() throws ExecutionException, InterruptedException {
    setupUpdateManagerWithAutoCommits();
    assertState("Scope(TXN-1)", List.of(1), List.of(10));

    // Task writes to both repos and then throws; the framework rolls back and propagates the error.
    var failing = updateManager.submit(writeContext -> {
      writeContext.repository(aRepoHandler).add(new A(3, "A3 failing"));
      writeContext.repository(bRepoHandler).add(new B(13, "B13 failing"));
      throw new RuntimeException("task failed");
    });

    var ex = assertThrows(ExecutionException.class, failing::get);
    assertInstanceOf(RuntimeException.class, ex.getCause());

    // No commit happened – published snapshot is still TXN-1 for both repos.
    assertState("Scope(TXN-1)", List.of(1), List.of(10));

    // Now submit a succeeding task to reveal the lifecycle difference.
    var succeeding = updateManager.submit(writeContext -> {
      writeContext.repository(aRepoHandler).add(new A(5, "A5 success"));
      writeContext.repository(bRepoHandler).add(new B(15, "B15 success"));
    });
    succeeding.get();

    // A rolled back cleanly: only the initial A(1) + new A(5).
    // B did NOT roll back: BRepository.copyOnWrite returns the same mutable instance, so its state
    // survives reset().  Both the explicit add (B13) and the domain-event-driven add (B1003, fired
    // when A3 was added before the failure) leaked into the committed snapshot.
    assertState("Scope(TXN-2)", List.of(1, 5), List.of(10, 13, 15));

    updateManager.shutdown();
  }

  private static Future<Void> processTask(
    UpdateManager updateManager,
    Consumer<WriteContext> task
  ) {
    return updateManager.submit(task);
  }

  private Consumer<WriteContext> publishNewAUsingADomainEvent() {
    return writeContext -> {
      eventLog.add(PUBLISH_DOMAIN_EVENT);
      writeContext.publish(new CreateNewThingDomainEvent(7, PUBLISH_DOMAIN_EVENT));
    };
  }

  private Consumer<WriteContext> publishNewAUsingARepo() {
    return writeContext -> {
      eventLog.add("Add A3 & B13 using repo");
      writeContext.repository(aRepoHandler).add(new A(3, "Add A3 using repo"));
      writeContext.repository(bRepoHandler).add(new B(13, "Add B13 using repo"));
    };
  }

  private void assertState(String expScope, List<Integer> expAIds, List<Integer> expBIds) {
    TransactionScope scope = registry.scope();
    assertEquals(expScope, scope.toString());

    assertEntities(expAIds, scope, aRepoHandler);
    assertEntities(expBIds, scope, bRepoHandler);
  }

  private <E extends Entity, S extends AbstractRepository<E>> void assertEntities(
    List<Integer> expIds,
    TransactionScope scope,
    RepositoryHandle<S, ?> handle
  ) {
    S snapshot = handle.repositorySnapshot(scope);
    var ids = snapshot.listIds().stream().sorted().toList();
    assertEquals(expIds, ids);
  }

  @SuppressWarnings("BusyWait")
  private void awaitState(String expScope, List<Integer> expAIds, List<Integer> expBIds)
    throws InterruptedException {
    long deadline = System.currentTimeMillis() + 2_000;
    while (System.currentTimeMillis() < deadline) {
      try {
        assertState(expScope, expAIds, expBIds);
        return;
      } catch (AssertionError ignored) {
        Thread.sleep(10);
      }
    }
    assertState(expScope, expAIds, expBIds);
  }

  private static void awaitUninterruptibly(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
