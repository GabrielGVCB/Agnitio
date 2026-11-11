package com.agnitio.domain.photo;

public class InvalidPhotoStateTransitionException extends RuntimeException {
    public InvalidPhotoStateTransitionException(PhotoStatus from, PhotoStatus to) {
        super("Invalid photo state transition from " + from + " to " + to);
    }
}
