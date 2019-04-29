FROM adoptopenjdk/openjdk8 as builder

WORKDIR /app
COPY . /app

RUN ./gradlew --no-daemon stage

FROM adoptopenjdk/openjdk8:jre

COPY --from=builder /app/server/build/libs/server.jar /server.jar

RUN apt-get update
RUN apt-get install -y --no-install-recommends fontconfig
# Cloud Run pukes when using the native blas impl
#RUN apt-get install -y --no-install-recommends libgfortran3 libatlas3-base libopenblas-base

ENV PORT 8080

CMD ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/server.jar"]
