FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend ./backend
COPY frontend ./frontend
RUN mvn -f backend/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/backend/target/page-pulse.jar ./page-pulse.jar
EXPOSE 8080
CMD ["java", "-jar", "page-pulse.jar"]
