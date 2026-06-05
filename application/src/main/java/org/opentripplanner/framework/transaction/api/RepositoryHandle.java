package org.opentripplanner.framework.transaction.api;

import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.Transaction;
import org.opentripplanner.framework.transaction.UpdateManager;

/**
 * Application-scoped typed access point for a single transactional repository.
 *
 * <p>A handle is obtained once at wiring time via
 * {@link RepositoryRegistry#register(Object, RepositoryLifecycle)} and then injected wherever
 * repository access is needed.
 *
 * <ul>
 *   <li>Request-scoped <em>services</em> should not call this directly. Instead they receive a
 *       {@link TransactionScope} from the framework and call
 *       {@link TransactionScope#snapshot(RepositoryHandle)} on it, which guarantees that all
 *       repositories in one request are resolved against the same transaction.
 *   <li><em>Updaters</em> obtain write access exclusively through a {@link WriteContext} provided
 *       by the {@link UpdateManager}. Handles are read-only from the public API.
 * </ul>
 *
 * @param <S> the read-only snapshot type
 * @param <M> the mutable snapshot type
 */
public interface RepositoryHandle<S, M> {
  /**
   * Resolve a read-only snapshot for the given transaction.
   *
   * <p>This is an internal method called by {@link TransactionScope}. Application code should use
   * the scope instead of calling this directly.
   */
  S readOnlySnapshot(Transaction transaction);
}
