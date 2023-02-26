FROM bitnami/java:17-debian-11

ARG TARGETOS
ARG TARGETARCH

COPY docker/install-packages.sh .
RUN chmod -R 755 install-packages.sh
RUN ./install-packages.sh

COPY docker/download-$TARGETARCH.sh .
RUN sh download-$TARGETARCH.sh
#RUN apk --no-cache add curl

RUN echo "I'm building for $TARGETOS/$TARGETARCH"

COPY build/libs/yaci-cli-*.jar yaci-cli.jar

RUN mkdir -p /config
COPY docker/application.properties /config/

WORKDIR /
EXPOSE 3001
EXPOSE 3002
EXPOSE 3003
EXPOSE 8090
EXPOSE 10000

CMD java -Dcom.sun.management.jmxremote -noverify ${JAVA_OPTS} -jar yaci-cli.jar
