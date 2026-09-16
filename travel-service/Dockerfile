FROM eclipse-temurin:25.0.4_7-jdk-noble AS build
WORKDIR /app
COPY gradle ./gradle
COPY gradlew settings.gradle build.gradle gradle.properties ./
RUN chmod +x gradlew
COPY src ./src
RUN ./gradlew bootJar --no-daemon --max-workers=2

FROM eclipse-temurin:25.0.4_7-jre-alpine-3.24
WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/app.jar
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
