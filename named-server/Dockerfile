FROM openjdk:17-jdk-alpine
LABEL org.opencontainers.image.authors="andrey.khlebnikov@synchro.pro"
ARG JAR_FILE=./named-server/target/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
