package com.backendbadi.menu;

import static org.assertj.core.api.Assertions.assertThat;

import com.backendbadi.menu.model.MenuItemRequest;
import com.backendbadi.menu.model.MenuItemResponse;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Starts the app on a random port and calls it over real HTTP.
 *
 * UPGRADE NOTE: TestRestTemplate is deprecated in Spring Boot 4.2.
 * The upgrade guide (step 11) rewrites this class with RestTestClient.
 *
 * Uses its own in-memory database so the items created here do not affect other test classes.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:api-test-db")
class MenuItemApiIntegrationTest {

    @Autowired
    private RestTestClient client;

    @Test
    void readsASampleItemOverHttp() {
        RestTestClient.BodySpec<MenuItemResponse, ?> response =
            client.get().uri("/api/menu-items/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(MenuItemResponse.class)
                .value(item -> assertThat(item.name()).isEqualTo("Idli"));
    }

    @Test
    void createsAndReadsBackAnItem() {
        MenuItemResponse created = client.post()
            .uri("/api/menu-items").contentType(MediaType.APPLICATION_JSON)
            .body(new MenuItemRequest("Mysore Bonda", 40, true))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(MenuItemResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(created).isNotNull();
        Long id = created.id();

        MenuItemResponse fetched =
                client.get().uri("/api/menu-items/" + id)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(MenuItemResponse.class)
                    .returnResult()
                    .getResponseBody();
        assertThat(fetched).isNotNull();
        assertThat(fetched.name()).isEqualTo("Mysore Bonda");
    }

    @Test
    void unknownIdReturns404() {
        String response = client.get().uri("/api/menu-items/999")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(response).isNotBlank();
    }
}
