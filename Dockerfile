# ---------- Stage 1: build the JAR with Maven + JDK 25 ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# Copy pom.xml first: dependencies are downloaded once and cached
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:go-offline

# Then copy the source code and build (tests run separately, see note)
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B package -DskipTests

# ---------- Stage 2: small runtime image with only the JRE ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Don't run as root inside the container
RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring

COPY --from=build /workspace/target/tiffin-menu-0.0.1-SNAPSHOT.jar app.jar

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]