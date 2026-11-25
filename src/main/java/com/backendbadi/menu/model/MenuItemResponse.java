package com.backendbadi.menu.model;

import com.backendbadi.menu.entity.MenuItem;

/** JSON the API returns. Keeps the JPA entity out of the API. */
public record MenuItemResponse(Long id, String name, int price, boolean available) {

    public static MenuItemResponse from(MenuItem item) {
        return new MenuItemResponse(item.getId(), item.getName(), item.getPrice(), item.isAvailable());
    }
}
