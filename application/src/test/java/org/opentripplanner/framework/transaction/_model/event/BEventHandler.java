package org.opentripplanner.framework.transaction._model.event;

import org.opentripplanner.framework.event.EventHandler;
import org.opentripplanner.framework.transaction._model.b.B;
import org.opentripplanner.framework.transaction._model.b.BRepository;

public class BEventHandler implements EventHandler<CreateNewThingDomainEvent, BRepository> {

  @Override
  public Class<CreateNewThingDomainEvent> eventType() {
    return CreateNewThingDomainEvent.class;
  }

  @Override
  public void handle(CreateNewThingDomainEvent event, BRepository bRepository) {
    bRepository.add(new B(10 + event.id(), event.name()));
  }
}
