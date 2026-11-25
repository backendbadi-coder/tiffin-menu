package com.backendbadi.menu;

import static org.assertj.core.api.Assertions.assertThat;

import com.backendbadi.menu.model.MenuItemRequest;
import com.backendbadi.menu.model.MenuItemResponse;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/**
 * Starts the app on a random port and calls it over real HTTP.
 *
 * UPGRADE NOTE: TestRestTemplate is deprecated in Spring Boot 4.2.
 * The upgrade guide (step 11) rewrites this class with RestTestClient.
 *
 * Uses its own in-memory database so the items created here do not affect other test classes.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:api-test-db")
class MenuItemApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void readsASampleItemOverHttp() {
        ResponseEntity<MenuItemResponse> response =
                restTemplate.getForEntity("/api/menu-items/1", MenuItemResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Idli");
    }

    @Test
    void createsAndReadsBackAnItem() {
        ResponseEntity<MenuItemResponse> created = restTemplate.postForEntity(
                "/api/menu-items", new MenuItemRequest("Mysore Bonda", 40, true), MenuItemResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        Long id = created.getBody().id();

        ResponseEntity<MenuItemResponse> fetched =
                restTemplate.getForEntity("/api/menu-items/" + id, MenuItemResponse.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().name()).isEqualTo("Mysore Bonda");
    }

    @Test
    void unknownIdReturns404() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/menu-items/999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
