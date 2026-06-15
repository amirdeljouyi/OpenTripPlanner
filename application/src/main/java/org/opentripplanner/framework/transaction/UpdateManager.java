package org.opentripplanner.framework.transaction;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.opentripplanner.framework.event.DomainEvent;
import org.opentripplanner.framework.event.EventHandler;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.api.WriteContext;

/**
 * Application-scoped manager for write operations against transactional repositories.
 * <p>
 * {@code UpdateManager} is the single public entry point for all writes. It owns a single-threaded
 * executor that serialises tasks, preventing concurrent mutation of mutable snapshots. Each task
 * is put on a queue. All task in the queue is processed in FIFO order when the commit is performed.
 * <p>
 * Typical usage:
 * <ol>
 *   <li>At startup, register {@link EventHandler}s via {@link #register(EventHandler, RepositoryHandle)}.
 *   <li>When an updater has work to do, call {@link #submit(Consumer)} with a lambda that receives
 *       a fresh {@link WriteContext}. Construct the updater service inside the lambda.
 *   <li>The {@code WriteContext} provides repository snapshot access and event publication.
 *   <li>Either the caller needs to commit or the manager can be set up to auto-commit periodically.
 * </ol>
 */
public interface UpdateManager {
  /**
   * Register an event handler at startup.
   * <p>
   * When a {@link org.opentripplanner.framework.event.DomainEvent} matching
   * {@code handler.eventType()} is published, the {@link WriteContext} will call the
   * given event handler, injecting the mutable repository for {@code repoHandle} at dispatch time.
   *
   * @param <E> the domain event type
   * @param <M> the repository type
   */
  <E extends DomainEvent, M> void register(
    EventHandler<E, M> handler,
    RepositoryHandle<?, M> repoHandle
  );

  /**
   * Submit an update task for execution on the single writer thread.
   * <p>
   * The task receives a fresh {@link WriteContext} scoped to this invocation. All writes and event
   * publications must go through the context. After the task returns, it is put on a queue and
   * commited with all pending tasks at commit time.
   *
   * @param task the task to execute
   * @return a {@link Future} that completes after the task has run and changes have been committed
   */
  Future<Void> submit(Consumer<WriteContext> task);

  /**
   * Shut down the writer thread and (if configured) the periodic commit scheduler.
   * <p>
   * Pending submitted tasks are allowed to complete; no new tasks are accepted after this call.
   */
  void shutdown();
}
