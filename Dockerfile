
# Build the application using Java 25 and Maven
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copy Maven configuration and download dependencies first
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline

# Copy source code and package the application
COPY src src
RUN ./mvnw -B clean package -DskipTests

# Run the packaged application on a smaller Java image
FROM eclipse-temurin:25-jre

WORKDIR /app

RUN useradd --system --uid 10001 appuser

COPY --from=build /app/target/it-asset-tracker-0.0.1-SNAPSHOT.jar app.jar

USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
