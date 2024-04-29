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
  RUN echo > /app/yaci-devkit-${tag}/version
  RUN echo "tag=${tag}" >> /app/yaci-devkit-${tag}/version
  RUN echo "revision=${EARTHLY_GIT_SHORT_HASH}" >> /app/yaci-devkit-${tag}/version
  COPY  docker-compose.yml \
                 env \
                 ssh.sh \
                 devkit.sh \
                 start.sh \
                 stop.sh \
                 yaci-cli.sh \
                 cardano-cli.sh \
                 info.sh \
                 LICENSE \
                 README.md \
                    /app/yaci-devkit-${tag}/

  RUN cd /app && zip -r yaci-devkit-${tag}.zip .
  SAVE ARTIFACT yaci-devkit-${tag}.zip AS LOCAL build/yaci-devkit-${tag}.zip

#docker-publish:
#    WAIT
#        BUILD +build-all-platforms
#    END
#    LOCALLY
#    LET IMAGE_NAME = ""
#    FOR image_target IN $ALL_BUILD_TARGETS
#        SET IMAGE_NAME = ${DOCKER_IMAGE_PREFIX}-${image_target}
#        RUN docker tag ${IMAGE_NAME}:${tag} bloxbean/${IMAGE_NAME}:${tag}
#        RUN docker push bloxbean/${IMAGE_NAME}:${tag}
#    END
