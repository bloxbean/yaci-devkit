VERSION 0.8

#ARG --global ALL_BUILD_TARGETS="cli viewer"
#ARG --global ALL_BUILD_TARGETS_NATIVE="cli-native viewer"
#ARG --global ALL_BUILD_TARGETS_NATIVE_LOCAL="cli-native-local viewer"
ARG --global DOCKER_IMAGE_PREFIX="yaci"
ARG --global tag="dev"
ARG --global local="true"
ARG --global REGISTRY_ORG = "bloxbean"

build:
  LOCALLY
  BUILD +cli
  BUILD +viewer
  BUILD +zip

build-native-all-platforms:
  LOCALLY
  BUILD --platform=linux/amd64 --platform=linux/arm64  +cli-native-local
  BUILD --platform=linux/amd64 --platform=linux/arm64  +viewer
  BUILD +zip

build-native:
  BUILD +cli-native
  BUILD +viewer
  BUILD +zip

build-java-all-platforms:
  LOCALLY
  BUILD --platform=linux/amd64 --platform=linux/arm64 +cli
  BUILD --platform=linux/amd64 --platform=linux/arm64 +viewer
  BUILD +zip

cli:
  ARG EARTHLY_TARGET_NAME
  ARG EARTHLY_GIT_SHORT_HASH
  FROM DOCKERFILE --build-arg  APP_VERSION=${tag} --build-arg COMMIT_ID=${EARTHLY_GIT_SHORT_HASH} applications/cli/.
  SAVE IMAGE --push ${REGISTRY_ORG}/${DOCKER_IMAGE_PREFIX}-${EARTHLY_TARGET_NAME}:${tag}

cli-native-local:
  LOCALLY
  ARG EARTHLY_TARGET_NAME
  ARG EARTHLY_GIT_SHORT_HASH
  WORKDIR applications/cli
  RUN ./gradlew --no-daemon -i -Pversion=${tag} clean build nativeCompile
  FROM DOCKERFILE -f applications/cli/Dockerfile_native --build-arg  APP_VERSION=${tag} --build-arg COMMIT_ID=${EARTHLY_GIT_SHORT_HASH} applications/cli/.
  SAVE IMAGE --push ${REGISTRY_ORG}/${DOCKER_IMAGE_PREFIX}-cli-native:${tag}

cli-native:
  FROM eclipse-temurin:21-jdk-jammy
  ARG EARTHLY_TARGET_NAME
  ARG EARTHLY_GIT_SHORT_HASH
  WORKDIR applications/cli
  RUN ./gradlew --no-daemon -i -Pversion=${tag} clean build nativeCompile
  FROM DOCKERFILE -f applications/cli/Dockerfile_native --build-arg  APP_VERSION=${tag} --build-arg COMMIT_ID=${EARTHLY_GIT_SHORT_HASH} applications/cli/.
  SAVE IMAGE --push ${REGISTRY_ORG}/${DOCKER_IMAGE_PREFIX}-cli-native:${tag}

viewer:
  ARG EARTHLY_TARGET_NAME
  FROM DOCKERFILE applications/${EARTHLY_TARGET_NAME}/.
  SAVE IMAGE --push ${REGISTRY_ORG}/${DOCKER_IMAGE_PREFIX}-${EARTHLY_TARGET_NAME}:${tag}
zip:
  LOCALLY
  RUN rm -rf build
  FROM alpine:3.19.1
  WORKDIR /app
  ARG EARTHLY_TARGET_NAME
  ARG EARTHLY_GIT_SHORT_HASH
  RUN apk add --no-cache zip
  RUN mkdir -p /app/yaci-devkit-${tag}
  RUN mkdir -p /app/yaci-devkit-${tag}/config
  RUN mkdir -p /app/yaci-devkit-${tag}/scripts
  RUN echo > /app/yaci-devkit-${tag}/version
  RUN echo "tag=${tag}" >> /app/yaci-devkit-${tag}/config/version
  RUN echo "revision=${EARTHLY_GIT_SHORT_HASH}" >> /app/yaci-devkit-${tag}/config/version
  COPY  config/env /app/yaci-devkit-${tag}/config/

  COPY  bin/devkit.sh /app/yaci-devkit-${tag}/bin/

  COPY  LICENSE \
        README.md \
         /app/yaci-devkit-${tag}/

  COPY  scripts/*.*  /app/yaci-devkit-${tag}/scripts/

  RUN cd /app && zip -r yaci-devkit-${tag}.zip .
  SAVE ARTIFACT yaci-devkit-${tag}.zip AS LOCAL build/yaci-devkit-${tag}.zip

test-nodes-setup:
  LOCALLY
  RUN cd build/ && unzip yaci-devkit-${tag}.zip
  RUN cp -r build/yaci-devkit-${tag} build/node1
  RUN cp -r build/yaci-devkit-${tag} build/node2
  RUN rm -rf build/yaci-devkit-${tag}
