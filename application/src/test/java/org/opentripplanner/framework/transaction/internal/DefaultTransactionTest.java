package org.opentripplanner.framework.transaction.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;

class DefaultTransactionTest {

  @Test
  void testEqualsAndHashCode() {
    var subject = new DefaultTransaction(1);
    var same = new DefaultTransaction(1);
    var other = new DefaultTransaction(2);
    AssertEqualsAndHashCode.verify(subject).sameAs(same).differentFrom(other);
  }

  @Test
  void testToString() {
    var subject = new DefaultTransaction(1);
    assertEquals("TXN-1", subject.toString());
  }
}
