package org.opentripplanner.framework.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
  public static final String WRITE_A_3_B_13_TO_REPOSITORY = "Create A3 & B13 using repository";
  private RepositoryRegistry registry;
  private UpdateManager updateManager;
  private RepositoryHandle<ASnapshot, ARepository> aRepoHandler;
  private RepositoryHandle<BSnapshot, BRepository> bRepoHandler;
  private final List<String> eventLog = new ArrayList<>();

  @BeforeEach
  public void setUp() throws Exception {
    // Create 2 repositories that uses a slightly different strategy for the life-cycle.
    // - ARepository uses a life-cycle manager and copy everything from the snapshot to the
    //   repository, and from the repository to the snapshot - enabling rollback and true atomic
    //   commits.
    // - BRepository does not use a life-cycle manager, and return the same instance for each
    //   transaction, and freeze it into a snapshot when the transaction is committed. This does
    //   not support atomic commits and rollback in case a task fails, but is more memory efficient
    //   since only the freeze action trigger copying the internal data structure.
    ARepository aRepository = new ARepository();
    BRepository bRepository = new BRepository();

    // Add some static data
    A a = new A(1, "A1");
    B b = new B(10, "B1");
    aRepository.add(a);
    bRepository.add(b);

    // set up a system event from AReop -> BRepo
    aRepository.addUpdateListener(bRepository::aUpdatedHandler);

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
   * Use auto-commit and a small thread-sleep to demonstrate that the task is processed
   * asynchronically.
   */
  @Test
  public void testHappyDayScenario() throws ExecutionException, InterruptedException {
    setupUpdateManagerWithAutoCommits();
    // Process a request
    assertState("Scope(TXN-1)", List.of(1), List.of(10));

    var f = processTask(updateManager, publishNewAUsingARepo());
    // We expect no change in the repository, because the task take a little time to finish. There
    // is a small sleep inside the task.
    assertState("Scope(TXN-1)", List.of(1), List.of(10));
    f.get();
    assertState("Scope(TXN-2)", List.of(1, 3), List.of(10, 13, 1003));

    f = processTask(updateManager, publishNewAUsingADomainEvent());
    assertState("Scope(TXN-2)", List.of(1, 3), List.of(10, 13, 1003));
    f.get();
    assertState("Scope(TXN-3)", List.of(1, 3, 7), List.of(10, 13, 17, 1003, 1007));

    updateManager.shutdown();

    assertEquals("""
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

    // We do not expect any changes in the repository before the scheduler commits the transaction.
    // This happens the first time after ca 40ms, then every 40ms. The feature take 20ms.
    var f = processTask(updateManager, publishNewAUsingARepo());

    // Future sleep 20m - but scheduler sleeps longer
    f.get();
    assertState("Scope(TXN-1)", List.of(1), List.of(10));
    sleep50ms();
    assertState("Scope(TXN-2)", List.of(1, 3), List.of(10, 13, 1003));

    f = processTask(updateManager, publishNewAUsingADomainEvent());
    f.get();
    assertState("Scope(TXN-2)", List.of(1, 3), List.of(10, 13, 1003));
    sleep50ms();
    assertState("Scope(TXN-3)", List.of(1, 3, 7), List.of(10, 13, 17, 1003, 1007));

    updateManager.shutdown();
    assertEquals("""
        Add A3 & B13 using repo
        Create new A & B with id 7 using a domain event""",
      String.join("\n", eventLog)
    );
  }

  private static Future<Void> processTask(UpdateManager updateManager, Consumer<WriteContext> task) {
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
      sleep20ms();
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


  private void sleep50ms() {
    sleep(50);
  }

  private void sleep20ms() {
    sleep(20);
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException _) {
    }
  }
}
