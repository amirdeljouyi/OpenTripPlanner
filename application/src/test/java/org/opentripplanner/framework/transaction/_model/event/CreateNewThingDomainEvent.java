package org.opentripplanner.framework.transaction._model.event;

import org.opentripplanner.framework.event.DomainEvent;
import org.opentripplanner.framework.transaction._model.b.B;

public class CreateNewThingDomainEvent implements DomainEvent {

  private final int id;
  private final String name;

  public CreateNewThingDomainEvent(int id, String name) {
    this.id = id;
    this.name = name;
  }

  public int id() {
    return id;
  }

  public String name() {
    return name;
  }

  public B b() {
    return new B(id, name);
  }

  @Override
  public String toString() {
    return "AUpdateEvent{" + "id=" + id + ", name='" + name + '\'' + '}';
  }
}
