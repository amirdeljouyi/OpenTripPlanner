package org.opentripplanner.framework.transaction;

import java.util.function.Consumer;
import org.opentripplanner.framework.transaction.api.TransactionScope;

/**
 * An opaque identity token representing a single committed state of the repository set.
 *
 * <p>A new token is minted on every {@link UpdateManager#submit(Consumer)} call. Readers that
 * captured an older token continue to see the state as of that commit; readers that create a new
 * {@link TransactionScope} after the commit see the updated state.
 */
public interface Transaction {}
