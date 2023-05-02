FROM ubuntu:22.04
ENV JAVA_HOME=/opt/java/openjdk
COPY --from=eclipse-temurin:17 $JAVA_HOME $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"

ARG TARGETOS
ARG TARGETARCH

COPY docker/install-packages.sh .
RUN chmod -R 755 install-packages.sh
RUN ./install-packages.sh

RUN groupadd -r app && useradd --no-log-init -r -g app app

RUN mkdir "/app"
RUN chown app:app /app

#Commented for now
#USER app

WORKDIR /app

COPY docker/download-$TARGETARCH.sh .
RUN sh download-$TARGETARCH.sh
#RUN apk --no-cache add curl

RUN echo "I'm building for $TARGETOS/$TARGETARCH"

RUN mkdir -p /app/store/config
COPY docker/store-application.properties /app/store/config/application.properties

RUN wget https://github.com/bloxbean/yaci-store/releases/download/v0.0.6/yaci-store-all-0.0.6.jar -O /app/store/yaci-store.jar

RUN wget https://github.com/bloxbean/yaci-cli/releases/download/v0.0.14/yaci-cli-0.0.14.jar -O /app/yaci-cli.jar

RUN mkdir -p /app/config
COPY docker/application.properties /app/config/

ENV PATH="$PATH:/app/cardano-bin"
ENV CARDANO_NODE_SOCKET_PATH=/clusters/default/node-spo1/node.sock
ENV PROTOCOL_MAGIC=42

WORKDIR /app
EXPOSE 3001
EXPOSE 3002
EXPOSE 3003
EXPOSE 8090
EXPOSE 10000
EXPOSE 8080

ENTRYPOINT ["java", "-jar","/app/yaci-cli.jar"]
