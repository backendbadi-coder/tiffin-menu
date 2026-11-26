# Tiffin Menu API: Spring Boot 4.2 / Java 25

A small, complete CRUD REST API for tiffin menu items (Idli, Dosa, Vada…), built with **Spring Boot 4.2** and **Java 25**.
Made for the **Backend Badi** YouTube channel (Java, Spring Boot & Kotlin in Telugu).

| | |
|---|---|
| Spring Boot | **4.2.0** (Spring Framework 7.1) |
| Java | **25** |
| Build | Maven |
| Database | H2 in-memory (data resets on every restart) |
| Tests | 11 (MockMvc + real HTTP with RestTestClient) |

---

## 1. Project structure

```
src/main/java/com/backendbadi/menu
├── MenuApplication.java                    main method
├── controller/MenuItemController.java      REST endpoints
├── service/MenuItemService.java            business logic, transactions
├── repository/MenuItemRepository.java      Spring Data JPA repository
├── entity/MenuItem.java                    JPA entity (jakarta.persistence)
├── model/MenuItemRequest.java              record: request body + validation
├── model/MenuItemResponse.java             record: response body
├── exception/MenuItemNotFoundException.java
└── handler/GlobalExceptionHandler.java     errors as ProblemDetail (RFC 9457)

src/main/resources
├── application.properties
└── data.sql                                5 sample menu items

src/test/java/com/backendbadi/menu
├── MenuItemControllerTest.java             8 tests: MockMvc (@SpringBootTest + @AutoConfigureMockMvc)
└── MenuItemApiIntegrationTest.java         3 tests: real HTTP with RestTestClient
```

---

## 2. Run it

### Requirements
- **JDK 25** (for example Eclipse Temurin 25)
- IntelliJ IDEA (or any IDE), Maven (IntelliJ's bundled Maven is fine)

### In IntelliJ IDEA
1. *File → Open…* → select this folder → *Open as Project*.
2. *File → Project Structure → Project*: **SDK = 25**, **Language level = 25**.
   No JDK 25 yet? *SDK → Download JDK… → Version 25 → Eclipse Temurin*.
3. *Settings → Build, Execution, Deployment → Build Tools → Maven → Runner → JRE* = **25**.
4. Wait for Maven to finish importing.
5. Run `MenuApplication`. You should see `Tomcat started on port 8080`.
6. Open http://localhost:8080/api/menu-items. You should see 5 items.

### From the terminal
```bash
mvn spring-boot:run      # start the app
mvn clean verify         # build + run all 11 tests
```

---

## 3. API endpoints

| Method | URL | Body | Success | Errors |
|---|---|---|---|---|
| GET | `/api/menu-items` | | 200 | |
| GET | `/api/menu-items/{id}` | | 200 | 404 |
| POST | `/api/menu-items` | `{"name":"Poori","price":50}` | 201 + `Location` header | 400 |
| PUT | `/api/menu-items/{id}` | `{"name":"Ghee Idli","price":55,"available":true}` | 200 | 400, 404 |
| DELETE | `/api/menu-items/{id}` | | 204 | 404 |
| GET | `/actuator/health`, `/actuator/info` | | 200 | |

`available` is optional and defaults to `true`.

### Try it with curl (Linux / macOS / Git Bash)
```bash
curl http://localhost:8080/api/menu-items
curl http://localhost:8080/api/menu-items/2
curl -i -X POST http://localhost:8080/api/menu-items -H "Content-Type: application/json" -d '{"name":"Poori","price":50}'
curl -i -X PUT http://localhost:8080/api/menu-items/1 -H "Content-Type: application/json" -d '{"name":"Ghee Idli","price":55,"available":true}'
curl -i -X DELETE http://localhost:8080/api/menu-items/4
curl -i http://localhost:8080/api/menu-items/999                                                                  # 404
curl -i -X POST http://localhost:8080/api/menu-items -H "Content-Type: application/json" -d '{"name":"","price":0}'   # 400
curl http://localhost:8080/actuator/info
```

> **Windows PowerShell:** type `curl.exe` instead of `curl`, and quote Maven `-D` arguments (`"-Dname=value"`). For POST/PUT with JSON, Postman is easier.

### Error responses (ProblemDetail, `application/problem+json`)
404, when an item doesn't exist:
```json
{
   "type": "about:blank",
   "title": "Menu item not found",
   "status": 404,
   "detail": "Menu item 999 not found",
   "instance": "/api/menu-items/999"
}
```
400, when validation fails:
```json
{
   "type": "about:blank",
   "title": "Invalid menu item",
   "status": 400,
   "detail": "Validation failed",
   "instance": "/api/menu-items",
   "errors": {
      "name": "name is required",
      "price": "price must be greater than 0"
   }
}
```
The exact field order may differ.

### H2 database console
http://localhost:8080/h2-console. JDBC URL: `jdbc:h2:mem:menudb`, user `sa`, empty password.
Try: `SELECT * FROM MENU_ITEMS;`

---

## 4. Spring Boot 4.x notes (good to know)
- The web starter is **`spring-boot-starter-webmvc`** (it was `spring-boot-starter-web` in Boot 2/3).
- Test starters are **per technology**: `spring-boot-starter-webmvc-test` (it brings JUnit, AssertJ, Mockito and MockMvc).
- `@SpringBootTest` no longer configures `MockMvc` by itself, so **`@AutoConfigureMockMvc`** is required.
- HTTP tests use **`RestTestClient`** with `@AutoConfigureRestTestClient` (`TestRestTemplate` is deprecated in Boot 4.2).
- The H2 console has its own module: **`spring-boot-h2console`**.
- JSON is handled by **Jackson 3**. Java records work as request and response bodies out of the box.
- `ProblemDetail.instance` is set **explicitly** in `GlobalExceptionHandler`, because Spring Framework 7.1 no longer fills it in automatically.

---

## 5. Version history

| Version | Commit | What changed |
|---|---|---|
| Spring Boot 4.1.3 / Java 21 | `86e0686` | First version: CRUD API, validation, ProblemDetail, tests |
| **Spring Boot 4.2.0 / Java 25** | `cdf9c45` | Upgrade: `TestRestTemplate` → `RestTestClient`; `server.forward-headers-strategy` → `server.request.header.forwarded.strategy`; `ProblemDetail.instance` set explicitly; Java 25 |

To see the code before the upgrade:
```bash
git checkout 86e0686
```

📘 **Want to do this upgrade yourself?** Follow the step-by-step guide: **[UPGRADE-4.1-to-4.2](UPGRADE-4.1-to-4.2.md)**
(preparation, 16 steps with commands and expected output, troubleshooting and a checklist).

---

## 6. Watch the videos
📺 **Backend Badi** on YouTube: Spring Boot, Java & Kotlin in Telugu
- Upgrade Spring Boot 4.1 → 4.2: *[Watch this YouTube video](https://youtu.be/k3eMzRon5o0)*, with the written guide in [UPGRADE-4.1-to-4.2](UPGRADE-4.1-to-4.2.md)