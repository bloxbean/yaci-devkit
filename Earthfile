VERSION 0.8

ARG --global ALL_BUILD_TARGETS="cli viewer"
ARG --global DOCKER_IMAGE_PREFIX="yaci"
ARG --global TAG="0.0.1-preview"

build:
  LOCALLY
  FOR image_target IN $ALL_BUILD_TARGETS
    BUILD +$image_target
  END

build-all-platforms:
  LOCALLY
  FOR image_target IN $ALL_BUILD_TARGETS
    BUILD --platform=linux/amd64 --platform=linux/arm64 +$image_target
  END

cli:
  ARG EARTHLY_TARGET_NAME
  ARG EARTHLY_GIT_SHORT_HASH
  FROM DOCKERFILE --build-arg  APP_VERSION=${TAG} --build-arg COMMIT_ID=${EARTHLY_GIT_SHORT_HASH} applications/${EARTHLY_TARGET_NAME}/.
  SAVE IMAGE bloxbean/${DOCKER_IMAGE_PREFIX}-devkit:${TAG}

viewer:
  ARG EARTHLY_TARGET_NAME
  FROM DOCKERFILE applications/${EARTHLY_TARGET_NAME}/.
  SAVE IMAGE bloxbean/${DOCKER_IMAGE_PREFIX}-${EARTHLY_TARGET_NAME}:${TAG}
