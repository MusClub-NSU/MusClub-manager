FROM openjdk:21-slim

WORKDIR /app

COPY build/libs/backend-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

