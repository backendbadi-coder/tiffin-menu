package com.backendbadi.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.backendbadi.menu.tool.MenuMcpTools;
import com.backendbadi.menu.model.MenuItemResponse;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** The MCP tools are normal Spring beans, so we can test them like any service. */
@SpringBootTest
@Transactional
class MenuMcpToolsTest {

  @Autowired
  private MenuMcpTools tools;

  @Test
  void getMenuCanFilterAvailableItems() {
    assertThat(tools.getMenu(null)).hasSize(5);
    assertThat(tools.getMenu(true)).hasSize(4)
        .allMatch(MenuItemResponse::available);
  }

  @Test
  void findsCheapItemsSortedByPrice() {
    List<MenuItemResponse> cheap = tools.findItemsUnderPrice(50);

    assertThat(cheap).extracting(MenuItemResponse::name).containsExactly("Vada", "Idli");
  }

  @Test
  void addsAnItem() {
    MenuItemResponse added = tools.addMenuItem("Punugulu", 30);

    assertThat(added.id()).isNotNull();
    assertThat(added.available()).isTrue();
  }

  @Test
  void rejectsInvalidPrice() {
    assertThatThrownBy(() -> tools.addMenuItem("Punugulu", 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("price must be greater than 0");
  }

  @Test
  void changesAvailability() {
    assertThat(tools.setItemAvailability(5L, true).available()).isTrue();
  }
}
