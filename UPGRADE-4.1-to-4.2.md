# Upgrade Guide: Spring Boot 4.1 → 4.2 (Tiffin Menu API)

A complete, step-by-step guide for upgrading the Tiffin Menu API from **Spring Boot 4.1.x** to **Spring Boot 4.2.0**.

**How to read this guide**
- Every step has **Do** (exact actions) and **Expect** (what you should see).
- ✅ = confirmed in Spring's official release notes or docs.
- ⚠️ = from a detailed third-party upgrade checklist, or not yet confirmed in the official docs. **Check it on your own machine.** If what you see differs, the official docs win.
- Commands are shown for Bash. On **Windows PowerShell**, use `curl.exe` instead of `curl`, and keep the double quotes around `-D...` arguments (see Part C).

---

## 0. Overview

| | Spring Boot 4.1 | Spring Boot 4.2 |
|---|---|---|
| Release date | 10 June 2026 | 19 November 2026 ✅ |
| Spring Framework | 7.0.x | **7.1.0** ✅ |
| Java | 17 – 25 | 17 – 27 (per endoflife.date; ⚠️ check the 4.2 *System Requirements* page) |
| Kind of release | minor | **minor**: same `jakarta`, same Jackson 3, same Hibernate 7 line |

**Key point:** 4.1 → 4.2 is a *minor* upgrade. There's no big rewrite like Boot 2 → 3, but there are still **deprecations, removed APIs and changed defaults**. That's why it pays to follow a fixed process instead of "just changing the version".

**What changes for this project:**

| # | Change in 4.2 | Effect on Tiffin Menu API | Status |
|---|---|---|---|
| 1 | Everything that was deprecated in 4.1 is **removed** | Must have **zero deprecation warnings** on 4.1 first | ✅ |
| 2 | `TestRestTemplate` **deprecated** → use `RestTestClient` + `@AutoConfigureRestTestClient` | `MenuItemApiIntegrationTest` gets rewritten | ✅ |
| 3 | `RestTemplate` support deprecated (Framework 7.1) | Not used in our main code, but worth knowing | ✅ |
| 4 | `server.forward-headers-strategy` **deprecated**, replaced by `server.request.header.forwarded.*` | Our `application.properties` uses it | ⚠️ |
| 5 | Framework 7.1 no longer sets `ProblemDetail.instance` to the request path automatically | Our 404 JSON may lose the `"instance"` field | ⚠️ |
| 6 | New versions: Spring Framework 7.1.0, Spring Data 2026.1.0, Micrometer 1.18.0, JUnit 6.1.x, H2 2.5.x | Visible in the dependency tree | ⚠️ (version numbers) |

Changes that **don't** affect this project are listed in step 15: the AMQP/RabbitMQ starter rename, Tomcat properties moving under `server.tomcat.servlet.*`, relative redirects now on by default, LDAP SSL, the Buildpacks builder change, and Micrometer convention changes.

---

## PART A: Preparation

### A1. Check your tools
**Do**
```bash
java -version     # must say 21 (or 17-25)
mvn -v            # optional; IntelliJ's bundled Maven is fine
git --version
```
In IntelliJ: *File → Project Structure → Project → SDK = 21*, and *Settings → Build Tools → Maven → Runner → JRE = 21*.

**Expect:** `openjdk version "21..."`.

### A2. Make the 4.1 project green and tag it
**Do** (skip `git init` if you cloned the repository)
```bash
cd tiffin-menu
mvn clean verify
git init
git add .
git commit -m "Tiffin Menu API on Spring Boot 4.1"
git tag v1-boot41
```
**Expect**
```
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### A3. Save "before" evidence
Before upgrading anything, record how the app behaves today, so you can compare after the upgrade.

**Do**: run the app (`MenuApplication`), then:
```bash
# 1. Error JSON
curl -s http://localhost:8080/api/menu-items/999 > before-404.json
# 2. Actuator info
curl -s http://localhost:8080/actuator/info > before-info.json
# 3. Main library versions
mvn -q dependency:tree "-Dincludes=org.springframework:spring-core,org.springframework.boot:spring-boot,org.hibernate.orm:hibernate-core,com.h2database:h2" > before-versions.txt
```
Keep these three files **out of Git** (or put them in a `notes/` folder).

---

## PART B: The upgrade, step by step

### Step 1. Read the release notes
**Do**: read these first:
- Spring blog: *"Spring Boot 4.2.0 available now"* (19 Nov 2026)
- Release notes wiki: `github.com/spring-projects/spring-boot/wiki` → **Spring Boot 4.2 Release Notes** (if the full page isn't published yet, read the **4.2.0-M1, M2, RC1 and RC2** release notes; each has an *"Upgrading from Spring Boot 4.1"* section)
- Spring Boot docs → **Upgrading Spring Boot**

The M1 notes state that classes, methods and properties deprecated in 4.1 were removed. That tells us what to do first (step 4).

---

### Step 2. Create an upgrade branch
**Do**
```bash
git checkout -b upgrade-boot-4.2
```
If anything goes wrong, switching back to your main branch (`main` or `master`) takes you straight back.

---

### Step 3. Make sure you're on the latest 4.1.x patch
**Do**: check Maven Central for the newest `4.1.x` (for example 4.1.3). If `pom.xml` is older, update the `<parent>` version and run `mvn clean verify` again.

**Expect**: still 11 tests green.

Always upgrade from the latest patch of the old version, not from an older one.

---

### Step 4. Clear all deprecation warnings on 4.1
Our `pom.xml` already compiles with `-Xlint:deprecation`:
```xml
<compilerArgs>
    <arg>-Xlint:deprecation</arg>
</compilerArgs>
```
**Do**
```bash
mvn clean test-compile
```
In IntelliJ you can also run *Code → Analyze Code → Run Inspection by Name… → "Deprecated API usage"*.

**Expect**: **no** `has been deprecated` warnings from our code on 4.1.

Anything deprecated in 4.1 doesn't exist in 4.2. Fix any warnings now, while the code still compiles.

---

### Step 5. Add the properties migrator (temporarily)
**Do**: add to `pom.xml` inside `<dependencies>`:
```xml
<!-- TEMPORARY: reports renamed/deprecated properties at startup. Remove after the upgrade. -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-properties-migrator</artifactId>
    <scope>runtime</scope>
</dependency>
```
This module checks `application.properties` at startup and reports which keys were renamed or deprecated.

---

### Step 6. Change the Spring Boot version to 4.2.0
**Do**: in `pom.xml`:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.2.0</version>   <!-- was 4.1.3 -->
    <relativePath/>
</parent>
```
And in `application.properties`:
```properties
info.app.spring-boot=4.2
```
One number changes, but behind it Spring Framework, Spring Data, Micrometer, JUnit and more all move to new versions together, through the parent's dependency management.

---

### Step 7. Reload Maven in IntelliJ
**Do**: *Maven tool window → 🔄 Reload All Maven Projects* (or the floating "Load Maven Changes" button, Ctrl+Shift+O).

**Expect**: the download progress bar, then no red underlines.

**If it says it can't find 4.2.0**: run `mvn -U clean verify` once (`-U` forces Maven to check for new versions), and check your internet or proxy.

---

### Step 8. Check that the new versions really arrived
**Do**
```bash
mvn -q dependency:tree "-Dincludes=org.springframework:spring-core,org.springframework.boot:spring-boot,org.hibernate.orm:hibernate-core,com.h2database:h2" > after-versions.txt
```
Compare `before-versions.txt` and `after-versions.txt` (IntelliJ: select both → right-click → *Compare Files*).

**Expect** ⚠️ (version numbers from the third-party checklist; check your own output):

| Library | 4.1.3 | 4.2.0 |
|---|---|---|
| spring-boot | 4.1.3 | **4.2.0** |
| spring-core (Framework) | 7.0.x | **7.1.0** |
| hibernate-core | 7.4.x | 7.4.x (unchanged) |
| h2 | 2.4.x | **2.5.x** |

Hibernate stays the same, so the entity code doesn't change. Spring Framework moves to 7.1, and that's where most of the changes come from.

---

### Step 9. Compile and read the warnings
**Do**
```bash
mvn clean test-compile
```
**Expect**: warnings like these (the exact wording may differ):
```
[WARNING] .../MenuItemApiIntegrationTest.java: [removal] TestRestTemplate in org.springframework.boot.resttestclient has been deprecated and marked for removal
[WARNING] .../MenuItemApiIntegrationTest.java: [removal] AutoConfigureTestRestTemplate ... has been deprecated and marked for removal
```
✅ The deprecation itself is confirmed in the 4.2.0-M1 release notes.

The build still works, but `TestRestTemplate` will be removed in a future version. Step 11 fixes it.

---

### Step 10. Run the tests and the app, then read the startup log
**Do**
```bash
mvn clean verify
```
**Expect**: `Tests run: 11, Failures: 0`. Deprecated code still runs.

Then start `MenuApplication` and look at the **first lines of the console**.

**Expect** ⚠️: a properties-migrator report similar to:
```
WARN ... PropertiesMigrationListener :
The use of configuration keys that are now deprecated was found in the following property sources:
...
	Key: server.forward-headers-strategy
		Line: 20
		Reason: Replaced by 'server.request.header.forwarded.strategy'
```
**Fix**: in `application.properties`, replace
```properties
server.forward-headers-strategy=framework
```
with ⚠️
```properties
server.request.header.forwarded.strategy=framework
```
**Confirm the new key**:
1. The migrator's own message (the "Replaced by" line) is the most reliable source. Use exactly what it prints.
2. Or open the Spring Boot 4.2 docs → *Appendix → Common Application Properties* and search for `forwarded`.
3. If the migrator prints **no** warning, the key isn't deprecated in your version. Leave it as it is and skip this fix.

Restart the app: the warning should be gone. Without the migrator, an old key could silently stop working in a future version.

---

### Step 11. Replace TestRestTemplate with RestTestClient ✅
**Do (1)**: rewrite `src/test/java/com/backendbadi/menu/MenuItemApiIntegrationTest.java`:

```java
package com.backendbadi.menu;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

/** Real HTTP tests with RestTestClient (replaces the deprecated TestRestTemplate). */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:api-test-db")
class MenuItemApiIntegrationTest {

    @Autowired
    private RestTestClient client;

    @Test
    void readsASampleItemOverHttp() {
        client.get().uri("/api/menu-items/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(MenuItemResponse.class)
                .value(item -> assertThat(item.name()).isEqualTo("Idli"));
    }

    @Test
    void createsAndReadsBackAnItem() {
        MenuItemResponse created = client.post().uri("/api/menu-items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MenuItemRequest("Mysore Bonda", 40, true))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(MenuItemResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();

        client.get().uri("/api/menu-items/{id}", created.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo("Mysore Bonda");
    }

    @Test
    void unknownIdReturns404() {
        client.get().uri("/api/menu-items/999")
                .exchange()
                .expectStatus().isNotFound();
    }
}
```
✅ The class names and packages (`RestTestClient` in `org.springframework.test.web.servlet.client`, `@AutoConfigureRestTestClient` in `org.springframework.boot.resttestclient.autoconfigure`) come from the official Spring Boot testing docs.
⚠️ The fluent method names (`.body(...)`, `.value(...)`, `.returnResult().getResponseBody()`, `.jsonPath(...)`) follow the style of `WebTestClient`. If IntelliJ underlines one, press **Ctrl+Space** after the dot to see the real method name. If your `model` classes live in a sub-package (for example `com.backendbadi.menu.model`), add the matching imports.

**Do (2)**: in `pom.xml`, **remove** the dependency that only `TestRestTemplate` needed:
```xml
<!-- delete this block -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-restclient</artifactId>
    <scope>test</scope>
</dependency>
```
Keep `spring-boot-resttestclient`, because `@AutoConfigureRestTestClient` lives there.

**Do (3)**
```bash
mvn clean verify
```
**Expect**: `Tests run: 11, Failures: 0`, and **no more deprecation warnings**.

Old style: get a `ResponseEntity` and assert on it by hand. New style: one fluent chain that reads like a sentence: get, exchange, expect status, expect body.

> Tip: `RestTestClient` is also available on 4.1, so in a real project you can make this change *before* the upgrade.

---

### Step 12. Compare the behaviour: the ProblemDetail "instance" field ⚠️
**Do**: run the app, then
```bash
curl -s http://localhost:8080/api/menu-items/999 > after-404.json
```
Compare `before-404.json` and `after-404.json` (IntelliJ *Compare Files*).

**What may happen** ⚠️: a detailed third-party upgrade checklist reports that Spring Framework 7.1 **no longer sets `ProblemDetail.instance` to the request path**. In that case the `"instance": "/api/menu-items/999"` line is **missing** after the upgrade.

- **If it's missing and your clients use it**, set it yourself. Change `GlobalExceptionHandler.handleNotFound`:
  ```java
  import java.net.URI;
  import jakarta.servlet.http.HttpServletRequest;

  @ExceptionHandler(MenuItemNotFoundException.class)
  public ProblemDetail handleNotFound(MenuItemNotFoundException ex, HttpServletRequest request) {
      ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
      problem.setTitle("Menu item not found");
      problem.setInstance(URI.create(request.getRequestURI()));   // set explicitly
      return problem;
  }
  ```
  Then add one assertion to `MenuItemControllerTest.unknownIdReturnsProblemDetail404()`:
  ```java
  .andExpect(jsonPath("$.instance").value("/api/menu-items/999"));
  ```
- **If both files are identical**, nothing changed for you, but the comparison was still worth doing.

Passing tests aren't the whole story: minor upgrades can change small details in the JSON that a mobile app or frontend depends on.

---

### Step 13. Final manual check of the whole app
**Do**: with the app running:
```bash
curl http://localhost:8080/api/menu-items                     # 5 items
curl -i -X POST http://localhost:8080/api/menu-items -H "Content-Type: application/json" -d '{"name":"Poori","price":50}'   # 201
curl -i -X POST http://localhost:8080/api/menu-items -H "Content-Type: application/json" -d '{"name":"","price":0}'         # 400
curl http://localhost:8080/actuator/health                    # {"status":"UP",...}
curl http://localhost:8080/actuator/info                      # spring-boot: 4.2
```
Open http://localhost:8080/h2-console and run `SELECT * FROM MENU_ITEMS;`.

**Expect**: everything works as it did on 4.1, and `/actuator/info` shows `4.2`.

> PowerShell: the JSON quoting in the POST commands works differently. Use Postman or IntelliJ's HTTP Client for those two requests if `curl.exe` gives a 400 on the valid request.

---

### Step 14. Clean up, commit, tag
**Do**
1. **Remove** the `spring-boot-properties-migrator` dependency from `pom.xml`. It's only for the upgrade: leaving it in slows startup slightly and isn't meant for production.
2. Run the final check:
   ```bash
   mvn clean verify
   ```
3. Commit and tag (use `master` instead of `main` if that's your default branch):
   ```bash
   git add .
   git commit -m "Upgrade to Spring Boot 4.2.0: RestTestClient, new forwarded-header property"
   git tag v2-boot42
   git checkout main && git merge upgrade-boot-4.2
   ```
4. Review the change: `git diff v1-boot41 v2-boot42 --stat`. Only a few files changed.

**Expect**: `BUILD SUCCESS`, 11 tests, 0 deprecation warnings.

---

### Step 15. 4.2 changes that didn't affect this project
If your own app is bigger, check these too:

| If your app uses… | Check this in 4.2 | Status |
|---|---|---|
| RabbitMQ (`spring-boot-starter-amqp`) | To stay on AMQP 0.9 → switch to `spring-boot-starter-rabbitmq`. For AMQP 1.0 → `spring-boot-starter-amqp-rabbitmq` | ✅ |
| `RestTemplate` / `RestTemplateBuilder` | Deprecated → move to `RestClient` / `RestClient.Builder`, `RestClientCustomizer` | ✅ |
| `server.tomcat.use-relative-redirects` etc. | Moved under `server.tomcat.servlet.*`, and relative redirects now default to `true` | ✅ |
| Embedded LDAP with SSL | `spring.ldap.embedded.ssl.enabled=true` now also needs `spring.ldap.embedded.ssl.bundle` | ✅ |
| `mvn spring-boot:build-image` | Default builder is now `paketobuildpacks/ubuntu-resolute-builder` | ✅ |
| Janino | No longer version-managed by Boot. Add the version yourself | ✅ |
| Custom Micrometer JVM conventions | Several conventions deprecated; startup fails if old and new ones are both present | ✅ |
| Metrics dashboards | New opt-in `management.observations.conventions` for OpenTelemetry naming. Roll it out separately | ⚠️ |
| `@DataJpaTest` with a real database | `spring.test.database.replace` default reportedly changed | ⚠️ |

---

### Step 16. (Optional) Move to Java 25
Spring Boot 4.2 runs on newer JDKs too. To move to Java 25:
1. *Project Structure → SDK → Download JDK → 25*, and set *Maven Runner JRE* to 25.
2. `pom.xml`: `<java.version>25</java.version>`.
3. `mvn clean verify`.

Do this as a separate commit (`git tag v3-java25`), never in the same step as the Boot upgrade, so that if something breaks you know which change caused it.

---

## PART C: Troubleshooting

| Problem | Cause | Fix |
|---|---|---|
| `Could not find artifact ...spring-boot-starter-parent:pom:4.2.0` | Maven cache or network | `mvn -U clean verify`; check your internet or proxy; check the version on Maven Central |
| `Invalid Artifact Descriptor Request` when running `dependency:tree` on Windows | PowerShell splits the unquoted `-Dincludes=...` argument at the commas | Put the whole argument in double quotes: `"-Dincludes=org.springframework:spring-core,..."` |
| `Unsupported class file major version 65` (or similar) | Maven or IntelliJ is running an older JDK | Set *Maven Runner JRE* and `JAVA_HOME` to 21 |
| IntelliJ shows red imports after the version change | Maven wasn't reloaded / stale index | *Reload All Maven Projects*; if that doesn't help, *File → Invalidate Caches → Restart* |
| `No qualifying bean of type RestTestClient` | `@AutoConfigureRestTestClient` missing, or `spring-boot-resttestclient` removed | Add the annotation; keep the `spring-boot-resttestclient` test dependency |
| `cannot find symbol: TestRestTemplate` after removing `spring-boot-restclient` | The old test file wasn't fully rewritten | Finish step 11 (no `TestRestTemplate` imports left) |
| Properties migrator prints nothing | Not on the runtime classpath, or no deprecated keys | Check the dependency has `<scope>runtime</scope>`; scroll to the very top of the startup log |
| `getAllReturnsTheSampleMenu` expects 5 but gets 6 | The HTTP test and the MockMvc test share one database | Keep `@TestPropertySource(... api-test-db)` on the HTTP test |
| `Port 8080 already in use` | An old instance is still running | Stop it in IntelliJ (red ■), or set `server.port=8081` |
| H2 console 404 | Module or property missing | Keep `spring-boot-h2console` and `spring.h2.console.enabled=true` |
| Something breaks and you're stuck | | `git stash` or `git checkout v1-boot41`, then repeat the steps one by one |

---

## PART D: Quick checklist

```
[ ] Tests green on the latest 4.1.x, tag v1-boot41
[ ] Save before-404.json, before-info.json, before-versions.txt
[ ] Read the 4.2 release notes ("Upgrading from Spring Boot 4.1")
[ ] git checkout -b upgrade-boot-4.2
[ ] Zero deprecation warnings on 4.1 (-Xlint:deprecation)
[ ] Add spring-boot-properties-migrator (runtime)
[ ] Parent version -> 4.2.0, reload Maven
[ ] Check dependency tree: spring-core 7.1.x
[ ] mvn clean verify; read deprecation warnings
[ ] Start app; fix properties reported by the migrator
[ ] TestRestTemplate -> RestTestClient + @AutoConfigureRestTestClient
[ ] Compare the 404 JSON (ProblemDetail.instance)
[ ] Manual curl checks + actuator + H2 console
[ ] Remove the migrator, mvn clean verify, commit, tag v2-boot42
```

---

## Sources
- Spring Boot 4.2.0-M1 release notes (removed 4.1 deprecations, RestTemplate/TestRestTemplate deprecation, AMQP starters, forwarded headers): https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.2.0-M1-Release-Notes
- Spring Boot 4.2.0-M2 release notes (Tomcat properties, relative redirects, LDAP SSL, Janino, Micrometer conventions): https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.2.0-M2-Release-Notes
- Spring Boot 4.2.0-RC1 release notes (Buildpacks builder): https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.2.0-RC1-Release-Notes
- Spring Boot 4.1 testing docs (packages for `@AutoConfigureMockMvc`, `TestRestTemplate`, `RestTestClient`, `@AutoConfigureRestTestClient`): https://docs.spring.io/spring-boot/4.1/reference/testing/spring-boot-applications.html
- Spring Boot 4.0 migration guide (Boot 4 starter and test-starter names): https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- Third-party, detailed (source of the ⚠️ items): "Spring Boot 4.2 GA: What's New and the 4.1 to 4.2 Upgrade Checklist": https://ankurm.com/spring-boot-4-2-spring-framework-7-1-upgrade-checklist/