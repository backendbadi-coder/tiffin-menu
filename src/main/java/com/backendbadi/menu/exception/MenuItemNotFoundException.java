package com.backendbadi.menu.exception;

/** Thrown when an id does not exist. GlobalExceptionHandler turns it into HTTP 404. */
public class MenuItemNotFoundException extends RuntimeException {

    public MenuItemNotFoundException(Long id) {
        super("Menu item " + id + " not found");
    }
}
