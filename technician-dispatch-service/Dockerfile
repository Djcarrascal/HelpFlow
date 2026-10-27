# ---------- Etapa build: Maven + JDK ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Solo el pom primero: Docker cachea las dependencias si el pom no cambia
COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q clean package -DskipTests

# ---------- Etapa run: JRE slim ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario sin privilegios: el proceso no corre como root
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
