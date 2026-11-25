package com.backendbadi.menu.controller;

import java.net.URI;
import java.util.List;

import com.backendbadi.menu.model.MenuItemRequest;
import com.backendbadi.menu.model.MenuItemResponse;
import com.backendbadi.menu.service.MenuItemService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 *   Controller for managing menu items.
 *   Provides endpoints for CRUD operations on menu items.
 *   All endpoints are prefixed with /api/menu-items.
 */
@RestController
@RequestMapping("/api/menu-items")
public class MenuItemController {

    private final MenuItemService service;

    public MenuItemController(MenuItemService service) {
        this.service = service;
    }

    /*
      * GET /api/menu-items
     */
    @GetMapping
    public List<MenuItemResponse> getAll() {
        return service.findAll();
    }

    /*
      * GET /api/menu-items/{id}
     */
    @GetMapping("/{id}")
    public MenuItemResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    /*
      * POST /api/menu-items
     */
    @PostMapping
    public ResponseEntity<MenuItemResponse> create(@Valid @RequestBody MenuItemRequest request) {
        MenuItemResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/menu-items/" + created.id())).body(created);
    }

    /*
      * PUT /api/menu-items/{id}
     */
    @PutMapping("/{id}")
    public MenuItemResponse update(@PathVariable Long id, @Valid @RequestBody MenuItemRequest request) {
        return service.update(id, request);
    }

    /*
      * DELETE /api/menu-items/{id}
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
