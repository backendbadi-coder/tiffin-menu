package com.backendbadi.menu.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * JSON the client sends to create or update an item.
 * A Java record: constructor, getters, equals/hashCode and toString are generated.
 */
public record MenuItemRequest(

        @NotBlank(message = "name is required")
        @Size(max = 50, message = "name must be at most 50 characters")
        String name,

        @NotNull(message = "price is required")
        @Positive(message = "price must be greater than 0")
        Integer price,

        /* Optional: when it is missing, the item is available. */
        Boolean available) {

    public boolean availableOrDefault() {
        return available == null || available;
    }
}
