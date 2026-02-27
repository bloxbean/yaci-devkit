FROM eclipse-temurin:21-jdk-alpine AS build
RUN apk add --no-cache bash
WORKDIR /src
COPY build.gradle settings.gradle ./
COPY src/ src/

RUN wget -q https://services.gradle.org/distributions/gradle-8.4-bin.zip -O /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    rm /tmp/gradle.zip

RUN /opt/gradle-8.4/bin/gradle --no-daemon jar

FROM eclipse-temurin:21-jre-alpine
COPY --from=build /src/build/libs/yaci-mcp-0.1.0.jar /app/yaci-mcp.jar
EXPOSE 9500
ENTRYPOINT ["java", "-jar", "/app/yaci-mcp.jar"]
