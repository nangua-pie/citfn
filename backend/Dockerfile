# Build stage: compile the jar with the pinned JDK and Maven version.
FROM maven:3.8.5-eclipse-temurin-11 AS build

WORKDIR /build

# Dependencies resolve first so a source-only change reuses the cached layer.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Runtime stage: JRE only.
FROM eclipse-temurin:11-jre-alpine

WORKDIR /app

# Never run the API as root.
RUN addgroup -S forum && adduser -S forum -G forum
USER forum

COPY --from=build /build/target/forum-server-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
