package com.agnitio.domain.event;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(EventId id) {
        super("Event not found with id: " + id);
    }
}
