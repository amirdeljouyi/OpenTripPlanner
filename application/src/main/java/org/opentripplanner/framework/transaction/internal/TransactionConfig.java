package org.opentripplanner.framework.transaction.internal;

import java.time.Duration;
import java.util.concurrent.ThreadFactory;
import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.UpdateManager;

public class TransactionConfig {

  public static RepositoryRegistry createRepositoryRegistry() {
    return new DefaultRepositoryRegistry();
  }

  public static UpdateManager createUpdateManager(
    String name,
    RepositoryRegistry registry,
    ThreadFactory threadFactory,
    Duration commitInterval
  ) {
    return new DefaultUpdateManager(
      name,
      ((DefaultRepositoryRegistry) registry).transactionManager(),
      threadFactory,
      commitInterval
    );
  }
}
