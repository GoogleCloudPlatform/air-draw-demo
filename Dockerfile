FROM adoptopenjdk/openjdk8 as builder

WORKDIR /app
COPY . /app

RUN ./gradlew --no-daemon stage

FROM adoptopenjdk/openjdk8:jre

COPY --from=builder /app/server/build/libs/server.jar /server.jar

RUN apt-get update
RUN apt-get install -y --no-install-recommends fontconfig
RUN apt-get install -y --no-install-recommends libatlas3-base libopenblas-base
RUN ln -s /usr/lib/libfontconfig.so.1 /usr/lib/libfontconfig.so && \
    ln -s /lib/libuuid.so.1 /usr/lib/libuuid.so.1 && \
    ln -s /lib/libc.musl-x86_64.so.1 /usr/lib/libc.musl-x86_64.so.1
ENV LD_LIBRARY_PATH /usr/lib

ENV PORT 8080

CMD ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/server.jar"]
