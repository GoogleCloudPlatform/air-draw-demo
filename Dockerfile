FROM adoptopenjdk/openjdk8 as builder

WORKDIR /app
COPY . /app

RUN ./gradlew --no-daemon --console=plain :server:shadowJar

FROM adoptopenjdk/openjdk8:jre

COPY --from=builder /app/server/build/libs/server.jar /server.jar

RUN apt-get update && apt-get install -y --no-install-recommends fontconfig

CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/server.jar"]
