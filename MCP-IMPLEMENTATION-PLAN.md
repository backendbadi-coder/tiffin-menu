# Implementation Plan: Turn the Tiffin Menu API into an AI Tool (Spring AI 2.1 + MCP)

This guide adds an **MCP server** to the existing **Tiffin Menu API** (Spring Boot 4.2 / Java 25). AI assistants can then call our Java methods as **tools**, and answer questions like *"What's available today under ₹50?"*, *"ఈరోజు menu లో ఏమున్నాయి?"* or *"Add Punugulu for ₹30"* with real data from the app.

**How to read this guide**
- Every step has **Do** (exact actions) and **Expect** (what you should see).
- ✅ = checked against the Spring AI 2.1.0 source code or official docs.
- ⚠️ = third-party tools, or not confirmed in the official docs. **Check it on your own machine.** If what you see differs, the official docs win.
- Starting point: the repo `backendbadi-coder/tiffin-menu` on `master` (Spring Boot 4.2.0, Java 25).
- Commands are shown for Bash. On **Windows PowerShell**, use `curl.exe` instead of `curl`.

---

## 0. Concepts

| Term | Simple meaning |
|---|---|
| **AI assistant / LLM** | A chat AI. It knows a lot, but it **can't see your app's data** (your menu, your database). |
| **Tool** | A function the AI is allowed to call, for example "get the menu". |
| **MCP (Model Context Protocol)** | A **standard plug** that connects AI assistants to tools. It works like USB: build one MCP server, and any MCP-capable AI app can use it. |
| **MCP server** | Our Spring Boot app. It lists its tools and runs them when the AI asks. |
| **MCP client** | The AI app (for example a desktop AI assistant, an IDE agent such as GitHub Copilot, or MCP Inspector for testing). |

```
 You: "What's under ₹50 today?"
        │
        ▼
 AI assistant (MCP client) ── "call find_items_under_price(50)" ──▶ Spring Boot app (MCP server)
        ▲                                                             │  MenuItemService → H2 database
        └──────────── [{"name":"Vada","price":35}, ...] ◀────────────┘
 AI: "Vada (₹35) and Idli (₹40) are available under ₹50."
```

**Key facts** ✅
- Spring AI 2.1.0 is built against **Spring Boot 4.2.0**, which matches this project.
- Since Spring AI 2.0, the MCP annotations (`@McpTool`, `@McpResource`, `@McpPrompt`) are **part of Spring AI**: one annotation turns a Spring method into an MCP tool.
- The default transport is **Streamable HTTP** at the endpoint **`POST /mcp`**.

The REST API already serves Postman and mobile apps. This guide makes the same Java code usable by AI, without writing any AI code ourselves.

---

## 1. What we'll build

| Tool name | What it does | Type |
|---|---|---|
| `get_menu` | List all items (optionally only available ones) | read-only |
| `find_items_under_price` | Available items at or below a price, cheapest first | read-only |
| `get_menu_item` | One item by id | read-only |
| `add_menu_item` | Add a new item | write |
| `set_item_availability` | Mark an item available or sold out | write |

**Files changed** (5):
```
pom.xml                                   + Spring AI BOM + MCP server starter
application.properties                    + MCP server settings, localhost-only binding
service/MenuItemService.java              + 2 small methods
mcp/MenuMcpTools.java                     NEW: the 5 tools
test/.../MenuMcpToolsTest.java            NEW: tests for the tools
```

---

## PART A: Preparation

### A1. Tools you need
| Tool | Why | Check |
|---|---|---|
| JDK 25 | The project's Java version | `java -version` |
| Git, IntelliJ IDEA | as before | |
| **Node.js 18+** (LTS) ⚠️ | To run **MCP Inspector** (a free test UI) with `npx` | `node -v` |
| An MCP-capable AI app (optional) | To chat with your tools in part D | see step 11 |

### A2. Start from a clean, green master
```bash
git checkout master
git pull
mvn clean verify          # 11 tests green
git checkout -b feature/mcp-server
```

---

## PART B: Build the MCP server

### Step 1. Add Spring AI to `pom.xml` ✅
**Do (1)**: add the version property inside `<properties>`:
```xml
<properties>
    <java.version>25</java.version>
    <spring-ai.version>2.1.0</spring-ai.version>
</properties>
```

**Do (2)**: add the Spring AI **BOM** (bill of materials) *after* `</dependencies>` and *before* `<build>`:
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Do (3)**: add the MCP server starter inside `<dependencies>` (no version, because the BOM manages it):
```xml
<!-- MCP server over HTTP for Spring MVC apps (Streamable HTTP, endpoint /mcp) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

**Do (4)**: reload Maven (*Maven tool window → 🔄*).

**Expect**: no red underlines; `mvn clean verify` is still green.

There are three MCP server starters: plain STDIO, WebFlux and WebMVC. This app uses Spring MVC, so we pick `-webmvc`. The BOM keeps all Spring AI versions in sync, the same way the Spring Boot parent does for Boot.

---

### Step 2. Configure the MCP server (`application.properties`) ✅
**Do**: add at the end:
```properties
# ---------- MCP server (Spring AI) ----------
spring.ai.mcp.server.name=tiffin-menu-mcp
spring.ai.mcp.server.version=1.0.0
spring.ai.mcp.server.instructions=Tools for the Annapurna tiffin center menu. Prices are in Indian rupees. Use the read-only tools to answer questions; only add or change items when the user clearly asks.
# STREAMABLE is the default; written here to make it explicit. Endpoint: POST /mcp
spring.ai.mcp.server.protocol=STREAMABLE
spring.ai.mcp.server.type=SYNC

# ---------- SAFETY ----------
# The MCP endpoint has NO authentication by default. Only accept connections from this computer.
server.address=127.0.0.1
```

| Property | Default (Spring AI 2.1.0) | Meaning |
|---|---|---|
| `spring.ai.mcp.server.name` | `mcp-server` | Name shown to the AI client |
| `spring.ai.mcp.server.version` | `1.0.0` | Version of *our* server |
| `spring.ai.mcp.server.instructions` | none | A hint telling the AI how to use our tools |
| `spring.ai.mcp.server.protocol` | `STREAMABLE` | `STREAMABLE`, `STATELESS` or `SSE` (SSE is deprecated) |
| `spring.ai.mcp.server.type` | `SYNC` | `SYNC` or `ASYNC` (we use normal blocking code, so SYNC) |
| `spring.ai.mcp.server.streamable-http.mcp-endpoint` | `/mcp` | URL path of the MCP endpoint |

> **Security (important):** the Spring AI docs warn that the HTTP MCP endpoint is **unauthenticated by default**, and the starters don't add any security. Binding the app to 127.0.0.1 means only your own computer can reach it. A real deployment needs Spring Security in front of `/mcp`.

---

### Step 3. Add two small methods to `MenuItemService`
**Do**: add these methods to `service/MenuItemService.java`:
```java
    /** Available items at or below a price, cheapest first. */
    @Transactional(readOnly = true)
    public List<MenuItemResponse> findAvailableUnderPrice(int maxPrice) {
        return repository.findAll(Sort.by("price")).stream()
                .filter(MenuItem::isAvailable)
                .filter(item -> item.getPrice() <= maxPrice)
                .map(MenuItemResponse::from)
                .toList();
    }

    /** Mark an item as available or sold out. */
    public MenuItemResponse setAvailability(Long id, boolean available) {
        MenuItem item = getItem(id);
        item.setAvailable(available);
        return MenuItemResponse.from(item);   // saved when the transaction commits
    }
```
`MenuItem` is in `com.backendbadi.menu.entity`, which the service already imports. If `Sort` isn't imported yet, add `import org.springframework.data.domain.Sort;`.

The AI tools call the same service as the REST controller, so business logic stays in one place: the controller and MCP are two different doors into the same room.

---

### Step 4. Create the MCP tools class ✅
**Do**: create `src/main/java/com/backendbadi/menu/mcp/MenuMcpTools.java`:
```java
package com.backendbadi.menu.mcp;

import java.util.List;

import com.backendbadi.menu.model.MenuItemRequest;
import com.backendbadi.menu.model.MenuItemResponse;
import com.backendbadi.menu.service.MenuItemService;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * Exposes the tiffin menu to AI assistants as MCP tools.
 * Spring AI finds every @McpTool method on a Spring bean and registers it with the MCP server.
 * The "description" texts matter: the AI reads them to decide WHEN and HOW to call each tool.
 */
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
```

✅ Verified in the Spring AI 2.1.0 source:
- the annotations live in `org.springframework.ai.mcp.annotation`
- `@McpTool` has `name`, `description` and `annotations`
- `@McpTool.McpAnnotations` has `readOnlyHint`, `destructiveHint`, `idempotentHint` and `openWorldHint`
- `@McpToolParam` has `description` and `required`, with `required` defaulting to **true**
- the docs say annotated beans are detected and registered automatically

⚠️ The method names `findAll()`, `findById()` and `create()` on `MenuItemService` are the ones in this repo. If your service uses different names, adjust the calls.

**Four ideas behind this class:**
1. `@Component` + `@McpTool` is all the AI wiring. There's no AI SDK code.
2. The **description is a prompt for the AI**. A clear description means the AI picks the right tool.
3. Hints such as `readOnlyHint=true` tell the AI client a tool only reads data. Clients can use the hints to decide when to ask the user before running a tool.
4. MCP calls don't go through `@Valid`, so the write tools validate their input themselves. The AI is a new kind of user, and it can send bad input too.

> What happens on errors: if a tool throws (for example `MenuItemNotFoundException` for an unknown id, or our `IllegalArgumentException`), the MCP server sends an **error result** back to the AI instead of crashing, and the AI usually explains it to the user. ⚠️ Try it in step 10 (call `get_menu_item` with id 999).

---

### Step 5. Run the app
**Do**: run `MenuApplication`.

**Expect**: `Tomcat started on port 8080` and the usual startup lines. The REST API still works:
```bash
curl http://127.0.0.1:8080/api/menu-items
```
⚠️ Spring AI may log the registered tools or the MCP server at startup. The exact log text varies between versions.

> Because of `server.address=127.0.0.1`, write **`127.0.0.1`** in every URL, not `localhost`. Node.js tools (MCP Inspector, `mcp-remote`) may resolve `localhost` to the IPv6 address `::1`, where the app isn't listening, and then fail with "connection refused". ⚠️

---

## PART C: Test the tools with MCP Inspector (no AI account needed)

### Step 6. Start MCP Inspector ⚠️
MCP Inspector is the official, free browser-based test tool from the MCP project. It works like Postman, but for MCP.

**Do**: in a new terminal:
```bash
npx @modelcontextprotocol/inspector
```
**Expect**: it prints a local URL and opens a browser tab with the Inspector UI. The first run downloads the package, so allow about a minute.

### Step 7. Connect to the server
**Do** in the Inspector UI:
1. **Transport Type** → **Streamable HTTP**
2. **URL** → `http://127.0.0.1:8080/mcp`
3. Click **Connect**

**Expect**: status "Connected". The server name **tiffin-menu-mcp** and version 1.0.0 are shown.

### Step 8. List the tools
**Do**: open the **Tools** tab → **List Tools**.

**Expect**: the 5 tools with their descriptions and input fields: `get_menu`, `find_items_under_price`, `get_menu_item`, `add_menu_item`, `set_item_availability`.

This is exactly what an AI sees: names, descriptions and input fields. Spring AI generated all of it from the Java method signatures.

### Step 9. Call each tool
| Tool | Input | Expected result |
|---|---|---|
| `get_menu` | `onlyAvailable` = true | 4 items (Upma is not available) |
| `find_items_under_price` | `maxPrice` = 50 | Vada (35), Idli (40) |
| `get_menu_item` | `id` = 2 | Masala Dosa, 60 |
| `add_menu_item` | `name` = Punugulu, `price` = 30 | new item with an id |
| `set_item_availability` | `id` = 5, `available` = true | Upma now available |

Then check the same data through REST, to confirm it's the same database:
```bash
curl http://127.0.0.1:8080/api/menu-items
```

### Step 10. Check the error cases
| Call | Expected |
|---|---|
| `get_menu_item` with `id` = 999 | an error result mentioning "Menu item 999 not found" ⚠️ (check the exact format in Inspector) |
| `add_menu_item` with `price` = 0 | an error result: "price must be greater than 0" |

The tools fail safely: the AI gets a clear error message instead of the app crashing.

---

## PART D: Connect a real AI assistant

### Step 11. Choose an MCP client
Any AI app that supports **MCP over Streamable HTTP** can connect to `http://127.0.0.1:8080/mcp`. Check your client's current docs for "MCP" or "connectors". ⚠️ Setup screens change often, so follow the client's own documentation.

**Option A: GitHub Copilot in IntelliJ IDEA** ⚠️
Open Copilot Chat in **Agent** mode, open the MCP settings (tools icon → *Add more tools…*), which opens `mcp.json`, and add:
```json
{
  "servers": {
    "tiffin-menu": {
      "type": "http",
      "url": "http://127.0.0.1:8080/mcp"
    }
  }
}
```
- The top-level key is **`servers`**. Using `mcpServers` (the format some other clients use) gives the error `[mcpServers] message: Invalid URL`.
- Use `127.0.0.1`, not `localhost` (see step 5).
- Start the app before starting the server in Copilot. After a restart of the app, restart the server in Copilot's MCP list.
- The menu path and file location can differ between plugin versions; check the GitHub Copilot docs if yours looks different.

**Option B: another client that supports HTTP MCP servers directly**
Add a server with URL `http://127.0.0.1:8080/mcp` in the client's MCP settings.

**Option C: a client that only supports local (STDIO) servers** ⚠️
Use the community bridge **`mcp-remote`** (an npm package), which connects a STDIO client to an HTTP server. A typical JSON config entry looks like this:
```json
{
  "mcpServers": {
    "tiffin-menu": {
      "command": "npx",
      "args": ["mcp-remote", "http://127.0.0.1:8080/mcp"]
    }
  }
}
```
Check the `mcp-remote` README and your client's docs for the exact file location and format.

> Many AI clients' "remote connector" features need a **public HTTPS URL**, not localhost. Exposing a local, unauthenticated server to the internet is **not safe**, so stay on localhost.

### Step 12. Try a conversation
With the app running and the client connected, ask in Telugu-English or English:

| You ask | Tool the AI should call |
|---|---|
| "ఈరోజు menu లో ఏమున్నాయి?" (What's on the menu today?) | `get_menu` (onlyAvailable = true) |
| "Show me tiffins under ₹50" | `find_items_under_price` (50) |
| "Masala Dosa price ఎంత?" (How much is Masala Dosa?) | `get_menu` or `get_menu_item` |
| "Upma sold out అయింది, update చెయ్యి" (Upma is sold out, update it) | `set_item_availability` (5, false). Most clients **ask for your permission** first |
| "Add Punugulu for ₹30" | `add_menu_item` |

AI answers vary between runs, and the AI may pick a different (but still correct) tool. Afterwards, open `http://127.0.0.1:8080/api/menu-items` in the browser to confirm the AI changed the real data.

---

## PART E: Tests

### Step 13. Unit-test the tools (no AI and no MCP client needed)
**Do**: create `src/test/java/com/backendbadi/menu/MenuMcpToolsTest.java`:
```java
package com.backendbadi.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.backendbadi.menu.mcp.MenuMcpTools;
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
```
**Do**: `mvn clean verify`

**Expect**: `Tests run: 16, Failures: 0` (11 existing + 5 new).

> The seed data says Vada costs 35 and Idli 40, so "cheapest first" gives Vada, then Idli. Upma (35) is not available, so it's filtered out.

> Do the existing HTTP tests still pass with `server.address=127.0.0.1`? They should, because the test client also calls localhost. ⚠️ If `MenuItemApiIntegrationTest` fails to connect, add `server.address=` (empty) to its `@TestPropertySource`.

---

## PART F: Finish

### Step 14. Update the README
Add a section:
```markdown
## AI tools (MCP server)
The app is also an **MCP server** (Spring AI 2.1). AI assistants can use these tools:
`get_menu`, `find_items_under_price`, `get_menu_item`, `add_menu_item`, `set_item_availability`.
- Endpoint: `http://127.0.0.1:8080/mcp` (Streamable HTTP)
- Test it: `npx @modelcontextprotocol/inspector` → Streamable HTTP → `http://127.0.0.1:8080/mcp`
- ⚠️ No authentication: the app only listens on 127.0.0.1. Don't expose it publicly.
```

### Step 15. Commit, push, pull request
```bash
git add .
git commit -m "feat(mcp): expose tiffin menu as MCP tools with Spring AI 2.1"
git push -u origin feature/mcp-server
```
Open a pull request to `master`, then squash and merge, and delete the branch.

---

## PART G: Troubleshooting

| Problem | Cause | Fix |
|---|---|---|
| `Could not find artifact org.springframework.ai:spring-ai-starter-mcp-server-webmvc` | BOM missing or in the wrong place | The BOM goes inside `<dependencyManagement>`, not inside `<dependencies>`; check `spring-ai.version` = 2.1.0 |
| `cannot find symbol: McpTool` | Wrong import | `import org.springframework.ai.mcp.annotation.McpTool;` |
| Inspector: connection refused | App not running, wrong port, or `localhost` resolved to IPv6 `::1` | Start the app; use `http://127.0.0.1:8080/mcp`, not `localhost`; check `server.address` |
| Inspector connects but shows 0 tools | Class isn't a Spring bean, or is outside the component scan | `@Component` on `MenuMcpTools`, and the package must be under `com.backendbadi.menu` |
| Inspector: wrong transport errors | Transport set to SSE or STDIO | Choose **Streamable HTTP** (our `protocol=STREAMABLE`) |
| Copilot log: `[mcpServers] message: Invalid URL` | `mcp.json` uses the `mcpServers` key | Use the `servers` format from step 11, option A |
| Copilot log: `missing required Authorization header` for `github` | Usually a temporary sign-in issue with Copilot's own GitHub server, not our app | Sign in to GitHub again in IntelliJ, or restart the IDE ⚠️ |
| `npx` not found | Node.js not installed | Install Node.js LTS, then reopen the terminal |
| The AI doesn't use the tools | Client not connected, or vague descriptions | Check the client's MCP status page; make the descriptions more specific |
| The AI calls the wrong tool | Descriptions overlap | Make each description say clearly when to use it |
| Port 8080 in use | Old instance still running | Stop it, or set `server.port=8081` and update all URLs |

---

## Sources
- Spring AI 2.1.0 source (tag `v2.1.0`): annotation packages and attributes, `McpServerProperties` defaults, starter modules, built against Spring Boot 4.2.0: https://github.com/spring-projects/spring-ai/tree/v2.1.0
- Spring AI docs, MCP Server Boot Starters (starter names, protocol property, `POST /mcp`, security warning): https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html
- Spring AI docs, MCP annotations (server): https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html
- Spring AI 2.0.0 GA announcement (MCP annotations moved into Spring AI): https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now/
- Spring AI 2.1.0-M1 announcement (baseline moved to Spring Boot 4.2): https://spring.io/blog/2026/09/25/spring-ai-2-1-0-M1-available-now/
- MCP Inspector, `mcp-remote` and the GitHub Copilot MCP setup: see their own READMEs and docs (third-party, ⚠️)