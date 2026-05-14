FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
# Download dependencies first (layer cache)
RUN mvn dependency:go-offline -B -q
COPY src src
RUN mvn package -DskipTests -B -q && \
    mkdir -p target/extracted && \
    java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/extracted/dependencies/          ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/    ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/           ./
EXPOSE 8090
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
