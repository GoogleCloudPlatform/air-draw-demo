FROM openjdk:8-jdk-alpine as builder

WORKDIR /app
COPY . /app

RUN ./gradlew --no-daemon stage

FROM openjdk:8-jre-alpine

COPY --from=builder /app/server/build/libs/server.jar /server.jar

ENV PORT 8080

CMD ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/server.jar"]
