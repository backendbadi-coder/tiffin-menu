# Tiffin Menu API: Spring Boot 4.1 / Java 21

A small, complete CRUD REST API for tiffin menu items, built on **Spring Boot 4.1**.

| | |
|---|---|
| Spring Boot | 4.1.x (`4.1.3` in `pom.xml`) |
| Java | 21 (Boot 4.1 supports 17 – 25) |
| Build | Maven |
| Database | H2 in-memory (resets on every restart) |

---

## 1. What's inside (8 Java classes)

| File | Purpose |
|---|---|
| `MenuApplication` | `main` method |
| `MenuItem` | JPA entity (`jakarta.persistence`) |
| `MenuItemRepository` | Spring Data JPA repository |
| `MenuItemRequest` | **record**: request body, with validation |
| `MenuItemResponse` | **record**: response body |
| `MenuItemService` | business logic, transactions |
| `MenuItemController` | REST endpoints |
| `MenuItemNotFoundException` + `GlobalExceptionHandler` | errors as **ProblemDetail** (RFC 9457) |

Tests:
- `MenuItemControllerTest`: 8 tests with **MockMvc** (`@SpringBootTest` + `@AutoConfigureMockMvc`)
- `MenuItemApiIntegrationTest`: 3 tests over real HTTP with **TestRestTemplate**. TestRestTemplate is deprecated in Boot 4.2, and the upgrade guide replaces it.

### Things that are new compared with Boot 2/3
- The starter is **`spring-boot-starter-webmvc`** (it was `spring-boot-starter-web`).
- Test starters are **per technology**: `spring-boot-starter-webmvc-test` (it brings `spring-boot-starter-test` with it).
- `@AutoConfigureMockMvc` now lives in `org.springframework.boot.webmvc.test.autoconfigure`, and `@SpringBootTest` needs it to get `MockMvc`.
- `TestRestTemplate` moved to `org.springframework.boot.resttestclient` and needs `@AutoConfigureTestRestTemplate`.
- The H2 console has its own module: `spring-boot-h2console`.
- JSON is handled by **Jackson 3**. Records work out of the box.

---

## 2. Run it in IntelliJ IDEA
1. *File → Open…* → select this folder → *Open as Project*.
2. *File → Project Structure → Project*: **SDK = 21** (download *Eclipse Temurin 21* there if needed), **Language level = 21**.
3. *Settings → Build, Execution, Deployment → Build Tools → Maven → Runner → JRE* = **21**.
4. Wait for Maven to finish importing.
5. Run `MenuApplication`. You should see `Tomcat started on port 8080`.
6. Open http://localhost:8080/api/menu-items. You should see 5 items.

**Tests:** right-click `src/test/java` → *Run 'All Tests'*, or `mvn clean verify` (11 tests).

> If Maven reports that `spring-boot-starter-parent:4.1.3` can't be found, change the version in `pom.xml` to the newest **4.1.x** listed on https://central.sonatype.com/artifact/org.springframework.boot/spring-boot.

---

## 3. Endpoints

| Method | URL | Body | Success | Errors |
|---|---|---|---|---|
| GET | `/api/menu-items` | | 200 | |
| GET | `/api/menu-items/{id}` | | 200 | 404 |
| POST | `/api/menu-items` | `{"name":"Poori","price":50}` | 201 + `Location` | 400 |
| PUT | `/api/menu-items/{id}` | `{"name":"Ghee Idli","price":55,"available":true}` | 200 | 400, 404 |
| DELETE | `/api/menu-items/{id}` | | 204 | 404 |
| GET | `/actuator/health`, `/actuator/info` | | 200 | |
| | `/h2-console` | JDBC URL `jdbc:h2:mem:menudb`, user `sa`, empty password | | |

```bash
curl http://localhost:8080/api/menu-items
curl http://localhost:8080/api/menu-items/2
curl -i -X POST http://localhost:8080/api/menu-items -H "Content-Type: application/json" -d '{"name":"Poori","price":50}'
curl -i -X PUT http://localhost:8080/api/menu-items/1 -H "Content-Type: application/json" -d '{"name":"Ghee Idli","price":55,"available":true}'
curl -i -X DELETE http://localhost:8080/api/menu-items/4
curl -i http://localhost:8080/api/menu-items/999          # 404 ProblemDetail
curl -i -X POST http://localhost:8080/api/menu-items -H "Content-Type: application/json" -d '{"name":"","price":0}'   # 400
curl http://localhost:8080/actuator/info
```

Example 404 response (`application/problem+json`):
```json
{
  "type": "about:blank",
  "title": "Menu item not found",
  "status": 404,
  "detail": "Menu item 999 not found",
  "instance": "/api/menu-items/999"
}
```
Save the exact output you get on 4.1.
---

