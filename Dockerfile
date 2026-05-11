FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache yt-dlp ffmpeg curl unzip && \
    curl -fsSL https://deno.land/install.sh | sh && \
    ln -s /root/.deno/bin/deno /usr/local/bin/deno

COPY --from=build /app/build/libs/*.jar app.jar

VOLUME /app/downloads

EXPOSE 8080
ENTRYPOINT ["java", "-XX:TieredStopAtLevel=1", "-jar", "app.jar"]