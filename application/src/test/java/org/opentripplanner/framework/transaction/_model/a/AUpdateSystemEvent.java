package org.opentripplanner.framework.transaction._model.a;

import javax.annotation.Nullable;

/**
 * This event is used o demonstrate that a change can propagate from one repository to another
 * as part of the source repository task/transaction. There are two alternative ways to propagate
 * changes:
 * <ul>
 * <li>Using a {@link org.opentripplanner.framework.event.DomainEvent}</li>
 * <li>Using a system event</li>
 * </ul>
 * The advantage of the domain event is that the repositories are decoupled, but might not be
 * possible, if one of the repositories depend on the created state of the other. For example
 * new transfers must be generated if a new stops are added.
 */
public record AUpdateSystemEvent(A newA, @Nullable A oldA) {}
