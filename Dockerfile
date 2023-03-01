# temp container to build using gradle
FROM bitnami/java:17-debian-11 AS YACI_STORE_BUILD
WORKDIR /
RUN mkdir -p /app
COPY docker/install-packages.sh .
RUN chmod -R 755 install-packages.sh
RUN ./install-packages.sh
RUN echo 77a5291
RUN git clone https://github.com/bloxbean/yaci-store.git
WORKDIR /yaci-store
RUN git checkout 77a5291
RUN ./gradlew clean build

FROM bitnami/java:17-debian-11

ARG TARGETOS
ARG TARGETARCH

COPY docker/install-packages.sh .
RUN chmod -R 755 install-packages.sh
RUN ./install-packages.sh

RUN groupadd -r app && useradd --no-log-init -r -g app app

RUN mkdir "/app"
RUN chown app:app /app

USER app
WORKDIR /app

COPY docker/download-$TARGETARCH.sh .
RUN sh download-$TARGETARCH.sh
#RUN apk --no-cache add curl

RUN echo "I'm building for $TARGETOS/$TARGETARCH"
RUN #mkdir "/app"

COPY build/libs/yaci-cli-*.jar /app/yaci-cli.jar

RUN mkdir -p /app/config
COPY docker/application.properties /app/config/

RUN mkdir -p /app/store/config
COPY docker/store-application.properties /app/store/config/application.properties
COPY --from=YACI_STORE_BUILD /yaci-store/applications/all/build/libs/yaci-store-all*.jar /app/store/yaci-store.jar

WORKDIR /app
EXPOSE 3001
EXPOSE 3002
EXPOSE 3003
EXPOSE 8090
EXPOSE 10000
EXPOSE 8080

ENTRYPOINT ["java", "-jar","/app/yaci-cli.jar"]
#CMD java -Dcom.sun.management.jmxremote -noverify ${JAVA_OPTS} -jar /app/yaci-cli.jar
