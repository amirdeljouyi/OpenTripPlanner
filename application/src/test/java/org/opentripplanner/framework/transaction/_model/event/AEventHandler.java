package org.opentripplanner.framework.transaction._model.event;

import org.opentripplanner.framework.transaction._model.a.A;
import org.opentripplanner.framework.transaction._model.a.ARepository;

public class AEventHandler
  implements
    org.opentripplanner.framework.event.EventHandler<CreateNewThingDomainEvent, ARepository> {

  @Override
  public Class<CreateNewThingDomainEvent> eventType() {
    return CreateNewThingDomainEvent.class;
  }

  @Override
  public void handle(CreateNewThingDomainEvent event, ARepository aRepository) {
    aRepository.add(new A(event.id(), event.name()));
  }
}
