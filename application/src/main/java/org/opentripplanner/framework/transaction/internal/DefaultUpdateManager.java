package org.opentripplanner.framework.transaction.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.framework.event.DomainEvent;
import org.opentripplanner.framework.event.EventHandler;
import org.opentripplanner.framework.transaction.RepositoryHandle;
import org.opentripplanner.framework.transaction.UpdateManager;
import org.opentripplanner.framework.transaction.WriteContext;

/**
 * Default implementation of {@link UpdateManager}.
 *
 * <p>Owns a single-threaded {@link ExecutorService} that serialises all submitted tasks.
 * After each task completes, changes are committed via the
 * {@link TransactionManager}. The transaction manager is package-private and
 * never exposed to callers — commit is an internal implementation detail.
 */
class DefaultUpdateManager implements UpdateManager {

  private final TransactionManager transactionManager;
  private final ExecutorService executor;
  private final Map<Class<?>, List<DefaultWriteContext.HandlerEntry<?, ?>>> eventHandlers =
    new HashMap<>();
  private final PeriodicCommitScheduler periodicCommitScheduler;

  DefaultUpdateManager(
    String name,
    TransactionManager transactionManager,
    ThreadFactory threadFactory,
    @Nullable Duration commitInterval
  ) {
    this.transactionManager = transactionManager;
    this.executor = Executors.newSingleThreadExecutor(threadFactory);
    this.periodicCommitScheduler = commitInterval != null
      ? new PeriodicCommitScheduler(name, commitInterval, threadFactory, this::performCommit)
      : null;
  }

  @Override
  public <E extends DomainEvent, M> void register(
    EventHandler<E, M> handler,
    RepositoryHandle<?, M> repoHandle
  ) {
    eventHandlers
      .computeIfAbsent(handler.eventType(), k -> new ArrayList<>())
      .add(new DefaultWriteContext.HandlerEntry<>(handler, repoHandle));
  }

  @Override
  public Future<Void> submit(Consumer<WriteContext> task) {
    return executor.submit(() -> {
      task.accept(new DefaultWriteContext(eventHandlers));
      return null;
    });
  }

  @Override
  public boolean autoCommitEnabled() {
    return periodicCommitScheduler != null;
  }

  @Override
  public Future<Void> commit() {
    if (autoCommitEnabled()) {
      throw new IllegalStateException("Auto-commit is enabled");
    }
    return performCommit();
  }

  @Override
  public void shutdown() {
    if (periodicCommitScheduler != null) {
      periodicCommitScheduler.shutdown();
    }
    executor.shutdown();
  }

  private Future<Void> performCommit() {
    return executor.submit(() -> {
      transactionManager.commit();
      return null;
    });
  }
}
