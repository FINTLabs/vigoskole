FROM eclipse-temurin:26-jdk AS builder
WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
RUN chmod +x gradlew
RUN ./gradlew --no-daemon dependencies

COPY src src
RUN ./gradlew --no-daemon bootJar

FROM gcr.io/distroless/java-base-debian12:nonroot
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED", "-jar", "/app/app.jar"]
