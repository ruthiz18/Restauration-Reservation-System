# ---- build the jar ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---- run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 reservo
COPY --from=build /build/target/reservo-backend-*.jar app.jar
USER reservo
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
