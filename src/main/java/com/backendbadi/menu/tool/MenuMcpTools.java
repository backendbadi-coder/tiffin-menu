package com.backendbadi.menu.tool;

import java.util.List;

import com.backendbadi.menu.model.MenuItemRequest;
import com.backendbadi.menu.model.MenuItemResponse;
import com.backendbadi.menu.service.MenuItemService;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class MenuMcpTools {

  private final MenuItemService menuItemService;

  public MenuMcpTools(MenuItemService menuItemService) {
    this.menuItemService = menuItemService;
  }

  @McpTool(name = "get_menu",
      description = "Get the tiffin center menu. Each item has id, name, price in rupees and availability.",
      annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
  public List<MenuItemResponse> getMenu(
      @McpToolParam(description = "true = only items available today; false or empty = all items",
          required = false) Boolean onlyAvailable) {
    List<MenuItemResponse> all = menuItemService.findAll();
    if (Boolean.TRUE.equals(onlyAvailable)) {
      return all.stream().filter(MenuItemResponse::available).toList();
    }
    return all;
  }

  @McpTool(name = "find_items_under_price",
      description = "Find menu items available today that cost at most the given price in rupees, cheapest first.",
      annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
  public List<MenuItemResponse> findItemsUnderPrice(
      @McpToolParam(description = "Maximum price in rupees, for example 50") int maxPrice) {
    return menuItemService.findAvailableUnderPrice(maxPrice);
  }

  @McpTool(name = "get_menu_item",
      description = "Get one menu item by its id.",
      annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
  public MenuItemResponse getMenuItem(
      @McpToolParam(description = "The menu item id, for example 1") Long id) {
    return menuItemService.findById(id);
  }

  @McpTool(name = "add_menu_item",
      description = "Add a new item to the menu. Only use this when the user explicitly asks to add an item.",
      annotations = @McpTool.McpAnnotations(destructiveHint = false))
  public MenuItemResponse addMenuItem(
      @McpToolParam(description = "Item name, for example 'Punugulu'") String name,
      @McpToolParam(description = "Price in rupees, must be greater than 0") int price) {
    // MCP calls do not go through @Valid, so we validate here ourselves.
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }
    if (name.trim().length() > 50) {
      throw new IllegalArgumentException("name must be at most 50 characters");
    }
    if (price <= 0) {
      throw new IllegalArgumentException("price must be greater than 0");
    }
    return menuItemService.create(new MenuItemRequest(name, price, true));
  }

  @McpTool(name = "set_item_availability",
      description = "Mark a menu item as available or sold out for today.",
      annotations = @McpTool.McpAnnotations(destructiveHint = false, idempotentHint = true))
  public MenuItemResponse setItemAvailability(
      @McpToolParam(description = "The menu item id") Long id,
      @McpToolParam(description = "true = available, false = sold out") boolean available) {
    return menuItemService.setAvailability(id, available);
  }
}
