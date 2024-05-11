VERSION 0.8

ARG --global ALL_BUILD_TARGETS="cli viewer"
ARG --global DOCKER_IMAGE_PREFIX="yaci"
ARG --global tag="dev"
ARG --global REGISTRY_ORG = "bloxbean"

build:
  LOCALLY
  FOR image_target IN $ALL_BUILD_TARGETS
    BUILD +$image_target
  END
  BUILD +zip

build-all-platforms:
  LOCALLY
  FOR image_target IN $ALL_BUILD_TARGETS
    BUILD --platform=linux/amd64 --platform=linux/arm64 +$image_target
  END
  BUILD +zip

cli:
  ARG EARTHLY_TARGET_NAME
  ARG EARTHLY_GIT_SHORT_HASH
  FROM DOCKERFILE --build-arg  APP_VERSION=${tag} --build-arg COMMIT_ID=${EARTHLY_GIT_SHORT_HASH} applications/cli/.
  SAVE IMAGE --push ${REGISTRY_ORG}/${DOCKER_IMAGE_PREFIX}-${EARTHLY_TARGET_NAME}:${tag}
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
