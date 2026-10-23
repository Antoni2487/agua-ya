# --- Build ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -q -B -DskipTests package

# --- Runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 aguaya
COPY --from=build /app/target/aguaya-*.jar app.jar
USER aguaya
EXPOSE 8080
ENV TZ=America/Lima
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
