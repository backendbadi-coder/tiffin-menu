package com.backendbadi.menu;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Whole app + MockMvc (no real HTTP server).
 * Boot 4: @SpringBootTest no longer sets up MockMvc on its own, so @AutoConfigureMockMvc is required.
 * @Transactional rolls the database back after every test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MenuItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAllReturnsTheSampleMenu() throws Exception {
        mockMvc.perform(get("/api/menu-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].name").value("Idli"));
    }

    @Test
    void getByIdReturnsOneItem() throws Exception {
        mockMvc.perform(get("/api/menu-items/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Masala Dosa"))
                .andExpect(jsonPath("$.price").value(60));
    }

    @Test
    void unknownIdReturnsProblemDetail404() throws Exception {
        mockMvc.perform(get("/api/menu-items/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Menu item not found"))
                .andExpect(jsonPath("$.detail").value("Menu item 999 not found"));
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        mockMvc.perform(post("/api/menu-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Poori", "price": 50}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Poori"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void invalidItemReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/menu-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "price": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("name is required"))
                .andExpect(jsonPath("$.errors.price").value("price must be greater than 0"));
    }

    @Test
    void updateChangesTheItem() throws Exception {
        mockMvc.perform(put("/api/menu-items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ghee Idli", "price": 55, "available": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ghee Idli"))
                .andExpect(jsonPath("$.price").value(55));
    }

    @Test
    void deleteRemovesTheItem() throws Exception {
        mockMvc.perform(delete("/api/menu-items/4"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/menu-items/4"))
                .andExpect(status().isNotFound());
    }

    @Test
    void healthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
