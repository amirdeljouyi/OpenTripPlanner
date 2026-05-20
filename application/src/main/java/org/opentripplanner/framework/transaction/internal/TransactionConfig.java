package org.opentripplanner.framework.transaction.internal;

import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.UpdateManager;

public class TransactionConfig {

  public static RepositoryRegistry createRepositoryRegistry() {
    return new DefaultRepositoryRegistry();
  }

  public static UpdateManager createUpdateManager(RepositoryRegistry registry) {
    return new DefaultUpdateManager(((DefaultRepositoryRegistry) registry).transactionManager());
  }
}
