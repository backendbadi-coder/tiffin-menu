package com.backendbadi.menu.handler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import com.backendbadi.menu.exception.MenuItemNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Errors are returned as ProblemDetail (RFC 9457), content type application/problem+json:
 * { "type": "about:blank", "title": "Menu item not found", "status": 404,
 *   "detail": "Menu item 999 not found", "instance": "/api/menu-items/999" }
 *
 * NOTE for the 4.2 upgrade: we do NOT set "instance" ourselves. Check whether it still
 * appears after upgrading (see UPGRADE-4.1-to-4.2.md, step 12).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MenuItemNotFoundException.class)
    public ProblemDetail handleNotFound(MenuItemNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Menu item not found");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Invalid menu item");
        problem.setProperty("errors", errors);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
