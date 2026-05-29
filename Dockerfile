FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /build

COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
RUN chmod +x gradlew

COPY build.gradle.kts settings.gradle.kts ./

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies \
    --no-daemon \
    --parallel \
    --build-cache \
    --configuration-cache \
    --max-workers=$(nproc) \
    -Dorg.gradle.jvmargs="-Xmx2g -XX:+UseG1GC"

COPY src/ src/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar \
    --no-daemon \
    --parallel \
    --build-cache \
    --configuration-cache \
    --max-workers=$(nproc) \
    -Dorg.gradle.jvmargs="-Xmx2g -XX:+UseG1GC"

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
RUN mkdir -p /app/data/downloads && chown -R app:app /app/data

COPY --from=builder --chown=app:app /build/build/libs/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]