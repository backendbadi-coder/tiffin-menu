# Tiffin Menu API: Spring Boot 4.2 / Java 25

A small, complete CRUD REST API for tiffin menu items (Idli, Dosa, Vada…), with an MCP tool server, built with **Spring Boot 4.2** and **Java 25**.
Made for the **Backend Badi** YouTube channel (Java, Spring Boot & Kotlin in Telugu).

| | |
|---|---|
| Spring Boot | **4.2.0** (Spring Framework 7.1) |
| Java | **25** |
| Build | Maven |
| Database | H2 in-memory (data resets on every restart) |
| MCP | Spring AI 2.1, Streamable HTTP at `/mcp` |
| Tests | REST API + MCP tools |

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
├── handler/GlobalExceptionHandler.java     errors as ProblemDetail (RFC 9457)
└── tool/MenuMcpTools.java                  Spring AI MCP tools

src/main/resources
├── application.properties
└── data.sql                                5 sample menu items

src/test/java/com/backendbadi/menu
├── MenuItemControllerTest.java             8 tests: MockMvc (@SpringBootTest + @AutoConfigureMockMvc)
├── MenuItemApiIntegrationTest.java         3 tests: real HTTP with RestTestClient
└── MenuMcpToolsTest.java                   5 tests: MCP tool behavior
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

## Run with Docker
Don't have Docker yet? See [DOCKER-INSTALLATION-GUIDE.md](DOCKER-INSTALLATION-GUIDE.md).

```bash
cp .env.example .env        # then set your own DB_PASSWORD
docker compose up -d --build
curl http://127.0.0.1:8080/api/menu-items
```
- Runs the app with the `docker` profile and PostgreSQL 18.
- Data is kept in the `db-data` volume. Reset with `docker compose down -v`.
- Ports are bound to 127.0.0.1 only, because the MCP endpoint `/mcp` has no authentication.

### From the terminal
```bash
mvn spring-boot:run      # start the app
mvn clean verify         # build + run all REST and MCP tests
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

## 4. MCP server

This application exposes the tiffin menu as tools for MCP-compatible AI clients using Spring AI's WebMVC MCP server starter.

- Endpoint: `http://127.0.0.1:8080/mcp`
- Transport: Streamable HTTP
- Server name: `tiffin-menu-mcp`
- Server version: `1.0.0`
- Prices: Indian rupees

### Available tools

| Tool | Purpose | Type |
|---|---|---|
| `get_menu` | Get all menu items, optionally only items available today | Read-only |
| `find_items_under_price` | Find available items at or below a maximum price, sorted cheapest first | Read-only |
| `get_menu_item` | Get one menu item by ID | Read-only |
| `add_menu_item` | Add a new menu item (name and price) | Mutating |
| `set_item_availability` | Mark an item available or sold out | Mutating |

The MCP tools are implemented in `MenuMcpTools` and call the existing `MenuItemService`, so REST and MCP clients use the same business logic and H2 database.

### MCP configuration

The server is configured in `application.properties`:

```properties
spring.ai.mcp.server.name=tiffin-menu-mcp
spring.ai.mcp.server.version=1.0.0
spring.ai.mcp.server.protocol=STREAMABLE
spring.ai.mcp.server.type=SYNC
server.address=127.0.0.1
```

Example MCP client configuration:

```json
{
  "mcpServers": {
    "tiffin-menu": {
      "url": "http://127.0.0.1:8080/mcp",
      "type": "streamableHttp"
    }
  }
}
```

The endpoint is intentionally bound to localhost and has no authentication. It should only be run locally or behind authentication and network controls in other environments.

---

## 5. Spring Boot 4.x notes (good to know)
- The web starter is **`spring-boot-starter-webmvc`** (it was `spring-boot-starter-web` in Boot 2/3).
- Test starters are **per technology**: `spring-boot-starter-webmvc-test` (it brings JUnit, AssertJ, Mockito and MockMvc).
- `@SpringBootTest` no longer configures `MockMvc` by itself, so **`@AutoConfigureMockMvc`** is required.
- HTTP tests use **`RestTestClient`** with `@AutoConfigureRestTestClient` (`TestRestTemplate` is deprecated in Boot 4.2).
- The H2 console has its own module: **`spring-boot-h2console`**.
- JSON is handled by **Jackson 3**. Java records work as request and response bodies out of the box.
- `ProblemDetail.instance` is set **explicitly** in `GlobalExceptionHandler`, because Spring Framework 7.1 no longer fills it in automatically.

---

## 6. Version history

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

## 7. Watch the videos
📺 **Backend Badi** on YouTube: Spring Boot, Java & Kotlin in Telugu
- Upgrade Spring Boot 4.1 → 4.2: *[Watch this YouTube video](https://youtu.be/k3eMzRon5o0)*, with the written guide in [UPGRADE-4.1-to-4.2](UPGRADE-4.1-to-4.2.md)
- MCP tools in Spring Boot 4.2: *[Watch this YouTube video](https://youtu.be/NHk0atzFBzo)*, with the written guide in [MCP-IMPLEMENTATION-PLAN](MCP-IMPLEMENTATION-PLAN.md)