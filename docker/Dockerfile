## This Dockefile builds a reduced footprint container.

ARG JAVA_VERSION=17

ARG ALPINE_VERSION=3.17.1
ARG LOCK_UNLOCK_VERSION=""

# Internal, passed between stages.
ARG RUN_DIR=/run
ARG DATA_DIR=${RUN_DIR}/data
ARG LOCK_UNLOCK_JAR=lock-unlock-subgraph-${LOCK_UNLOCK_VERSION}.jar
ARG LOCAL_DOCKER_DIR=docker

## ---- Stage: Build
FROM eclipse-temurin:${JAVA_VERSION}-alpine AS BUILD

ARG LOCK_UNLOCK_VERSION
ARG LOCK_UNLOCK_JAR
ARG RUN_DIR
RUN echo && echo "==== Docker build for Lock-Unlock Subgraph ${LOCK_UNLOCK_VERSION} ====" && echo

## -- Build lock-unlock-subgraph jar.
WORKDIR /build
COPY . .
RUN ./gradlew build

RUN cp build/libs/${LOCK_UNLOCK_JAR} ${RUN_DIR}/${LOCK_UNLOCK_JAR}

WORKDIR ${RUN_DIR}


## ---- Stage: Runtime
FROM amazoncorretto:17-alpine3.19

## Import ARGs
ARG RUN_DIR
ARG DATA_DIR
ARG LOCK_UNLOCK_JAR
ARG LOCAL_DOCKER_DIR

ARG LOCK_UNLOCK_USER=unlock
ARG LOCK_UNLOCK_GROUP=$LOCK_UNLOCK_USER
ARG LOCK_UNLOCK_GID=1000
ARG LOCK_UNLOCK_UID=1000

# Run as this user
# -H : no home directory
# -D : no password
RUN addgroup -g "${LOCK_UNLOCK_GID}" "${LOCK_UNLOCK_GROUP}" && \
    adduser "${LOCK_UNLOCK_USER}" -G "${LOCK_UNLOCK_GROUP}" -s /bin/ash -u "${LOCK_UNLOCK_UID}" -H -D

RUN mkdir --parents "${RUN_DIR}" && \
    mkdir --parents "${DATA_DIR}" && \
    chown -R $LOCK_UNLOCK_USER ${RUN_DIR}

USER $LOCK_UNLOCK_USER

WORKDIR $RUN_DIR

COPY --chown=${LOCK_UNLOCK_GID}:${LOCK_UNLOCK_GID} --from=BUILD ${RUN_DIR}/${LOCK_UNLOCK_JAR} ${RUN_DIR}/${LOCK_UNLOCK_JAR}
COPY --chown=${LOCK_UNLOCK_GID}:${LOCK_UNLOCK_GID} ${LOCAL_DOCKER_DIR}/entrypoint.sh .
COPY --chown=${LOCK_UNLOCK_GID}:${LOCK_UNLOCK_GID} ./data ${DATA_DIR}

RUN ls -al

RUN chmod a+x entrypoint.sh

## Default environment variables.
ENV \
    JAVA_OPTIONS="-Xmx4048m -Xms2048m"  \
    LOCK_UNLOCK_JAR="${LOCK_UNLOCK_JAR}"          \
    RUN_DIR="${RUN_DIR}"

EXPOSE 8080

ENTRYPOINT ["./entrypoint.sh" ]
CMD []