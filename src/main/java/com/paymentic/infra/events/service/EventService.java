package com.paymentic.infra.events.service;

import com.paymentic.infra.events.Event;
import com.paymentic.infra.events.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {
  private final EventRepository eventRepository;
  public EventService(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }
  @Transactional
  public boolean shouldHandle(Event event){
    try {
      this.eventRepository.insert(event);
    }catch (Exception e){
      return false;
    }
    return true;
  }

}

