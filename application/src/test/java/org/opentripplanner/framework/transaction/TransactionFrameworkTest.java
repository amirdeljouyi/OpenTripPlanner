package org.opentripplanner.framework.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.transaction._model.a.A;
import org.opentripplanner.framework.transaction._model.a.ARepository;
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

///
/// What do we need?
///
/// GRAPH BUILD
/// - Use the mutable repository to add entities.
///   1. Parse an input source
///   2. Map into domain builders/objects(none indexed)
///   3. Validate the domain objects.
///   4. Add to the **mutable repository**. The repository is responsible for indexing and
///      assigning unique internal sequence IDs. _These sequence IDs should not be used by
///      x-referencing domain objects across repositories._
///
/// APPLICATION CONTEXT
///  1. Create the RepositoryRegistry and register repositories with the initialized
///     snapshot.
///  2. Bind the RepositoryRegistry and the UpdateManager to the application context.
///  2. (Create a repository handle for each repository.)
///  3. Use the repository handle to access the repository within a transaction scope.
///
/// DAGGER REQUEST SCOPE
///   1) Get a scope-object or transaction object from the registry.
///   2) Use the scope to inject a snapshot repository into services.
///
///
///
///
/// ### Important notes
/// - Use public IDs to reference entities across agregates/repositories.
/// -
///
///
///
///
public class TransactionFrameworkTest {

  public static final String PUBLISH_DOMAIN_EVENT =
    "Create new A & B with id 7 using a domain event";
  public static final String WRITE_A_3_B_13_TO_REPOSITORY = "Create A3 & B13 using repository";
  public static final Duration COMMIT_INTERVAL = Duration.ofMillis(200);
  private RepositoryRegistry registry;
  private UpdateManager updateManager;
  private RepositoryHandle<ASnapshot, ARepository> aRepoHandler;
  private RepositoryHandle<BSnapshot, BRepository> bRepoHandler;
  private List<String> eventLog = new ArrayList<>();

  @BeforeEach
  public void setUp() throws Exception {
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
    this.aRepoHandler = registry.registerRepository(aRepository, aRepository);
    this.bRepoHandler = registry.registerSnapshot(bRepository.freeze(bRepository), bRepository);

    var threadFactory = new ThreadFactoryBuilder().setNameFormat("autoCommit").build();
    updateManager = TransactionFactory.createUpdateManager(
      getClass().getSimpleName(),
      registry,
      threadFactory,
      null
    );
    updateManager.register(new AEventHandler(), aRepoHandler);
    updateManager.register(new BEventHandler(), bRepoHandler);
  }

  @Test
  public void runTest() throws ExecutionException, InterruptedException {
    // Process a request
    assertState("Scope(TXN-1)", List.of(1), List.of(10));

    processTask(updateManager, publishNewAUsingARepo());
    assertState("Scope(TXN-1)", List.of(1), List.of(10));

    commit();
    assertState("Scope(TXN-2)", List.of(1, 3), List.of(10, 13, 1000));

    processTask(updateManager, publishNewAUsingADomainEvent());
    assertState("Scope(TXN-2)", List.of(1, 3), List.of(10, 13, 1000));

    commit();
    assertState("Scope(TXN-3)", List.of(1, 3, 7), List.of(10, 13, 17, 1000));

    updateManager.shutdown();
  }

  private static void processTask(UpdateManager updateManager, Consumer<WriteContext> task)
    throws InterruptedException, ExecutionException {
    var f = updateManager.submit(task);
    f.get();
  }

  private Consumer<WriteContext> publishNewAUsingADomainEvent() {
    return writeContext -> {
      eventLog.add(PUBLISH_DOMAIN_EVENT);
      writeContext.publish(new CreateNewThingDomainEvent(7, PUBLISH_DOMAIN_EVENT));
    };
  }

  private Consumer<WriteContext> publishNewAUsingARepo() {
    return writeContext -> {
      System.out.println(WRITE_A_3_B_13_TO_REPOSITORY);
      writeContext.mutableRepository(aRepoHandler).add(new A(3, "Add A3 using repo"));
      writeContext.mutableRepository(bRepoHandler).add(new B(13, "Add B13 using repo"));
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
    S snapshot = handle.snapshot(scope);
    var ids = snapshot.listIds().stream().sorted().toList();
    assertEquals(expIds, ids);
  }

  private void commit() throws ExecutionException, InterruptedException {
    updateManager.commit().get();
  }
}
