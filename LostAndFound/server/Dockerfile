FROM maven:3.9.6-eclipse-temurin-11 as build
WORKDIR /app
COPY pom.xml /app/pom.xml
COPY src /app/src
RUN mvn -f /app/pom.xml -DskipTests package

FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
ENV APP_OPTS=""
ENTRYPOINT ["sh","-c","java $APP_OPTS -jar /app/app.jar"]
