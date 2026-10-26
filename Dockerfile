FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -B -q -pl backend-marvisur/marvisur-api -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/backend-marvisur/marvisur-api/target/marvisur-api-0.1.0-SNAPSHOT-exec.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod JAVA_TOOL_OPTIONS="-Xmx300m -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]