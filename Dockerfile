FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test

RUN cp build/libs/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
