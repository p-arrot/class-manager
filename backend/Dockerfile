FROM eclipse-temurin:21.0.6_7-jre
WORKDIR /app
COPY target/edu-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
