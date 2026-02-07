# Build stage
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app
COPY . .
RUN mvn -pl common-proto,ml-risk-ms install -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /app/ml-risk-ms/target/*.jar app.jar
EXPOSE 8082 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
