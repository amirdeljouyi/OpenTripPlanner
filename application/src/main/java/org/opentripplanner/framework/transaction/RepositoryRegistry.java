package org.opentripplanner.framework.transaction;

import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;
import org.opentripplanner.framework.transaction.api.TransactionScope;

/**
 * Application-scoped registry for transactional repositories.
 *
 * <p>This is the entry point for wiring the transaction framework. Typical usage:
 *
 * <ol>
 *   <li>Create one {@code RepositoryRegistry} for the application lifetime.
 *   <li>For each domain repository, call {@link #registerSnapshot(Object, RepositoryLifecycle)}
 *       during wiring (e.g. in a Dagger module). Keep the returned {@link RepositoryHandle} for
 *       injection into services and updaters.
 *   <li>At the start of each request, call {@link #scope()} to obtain a {@link TransactionScope}
 *       that captures a consistent snapshot of all repositories at that point in time.
 *   <li>To perform writes, use the {@link UpdateManager}, which commits changes automatically
 *       after each submitted task.
 * </ol>
 */
public interface RepositoryRegistry {
  /**
   * Register a new transactional repository and return a typed handle for it.
   *
   * <p>The handle is application-scoped and should be kept for the lifetime of the application,
   * typically by injecting it via Dagger.
   *
   * @param initialSnapshot the initial read-only snapshot for the repository
   * @param lifecycle       the copy-on-write / freeze strategy for this repository's snapshot types
   * @param <S>             the read-only repository snapshot type
   * @param <M>             the mutable repository type
   * @return an application-scoped handle for accessing this repository
   */
  <S, M> RepositoryHandle<S, M> registerSnapshot(
    S initialSnapshot,
    RepositoryLifecycle<S, M> lifecycle
  );

  /**
   * This has the same semantics as {@link #registerSnapshot(Object, RepositoryLifecycle)}, but
   * creates the initial snapshot from the mutable repository, using the provided lifecycle.
   */
  <S, M> RepositoryHandle<S, M> registerRepository(
    M repository,
    RepositoryLifecycle<S, M> lifecycle
  );

  /**
   * Create a new {@link TransactionScope} capturing the current transaction.
   *
   * <p>All calls to {@link RepositoryHandle#snapshotRepository(TransactionScope)} on the returned
   * scope will resolve against the same transaction, guaranteeing a consistent read view across
   * all repositories for the duration of the request.
   *
   * <p>In a Dagger setup this method would be called from a request-scoped {@code @Provides}
   * method.
   */
  TransactionScope scope();
}
