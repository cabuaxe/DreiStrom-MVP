### Stage 1: Build ###
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/.mvn .mvn
# Cache dependencies
RUN --mount=type=cache,target=/root/.m2 \
    apt-get update && apt-get install -y maven && \
    mvn dependency:go-offline --batch-mode --no-transfer-progress
COPY backend/src src
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests --batch-mode --no-transfer-progress

### Stage 2: Runtime ###
FROM eclipse-temurin:21-jre-jammy
RUN groupadd -r dreistrom && useradd -r -g dreistrom dreistrom
WORKDIR /app
COPY --from=build /app/target/dreistrom-*.jar app.jar
RUN chown -R dreistrom:dreistrom /app
USER dreistrom

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
