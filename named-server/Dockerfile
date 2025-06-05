FROM eclipse-temurin:17-jre
LABEL org.opencontainers.image.authors="andrey.khlebnikov@synchro.pro"
ARG JAR_FILE=./named-server/target/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
