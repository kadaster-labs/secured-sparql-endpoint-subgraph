## This Dockefile builds a reduced footprint container.

ARG JAVA_VERSION=17

ARG ALPINE_VERSION=3.17.1
ARG LOCK_UNLOCK_VERSION=""

# Internal, passed between stages.
ARG DATA_DIR=/run/data
ARG RUN_DIR=/run
ARG LOCK_UNLOCK_JAR=lock-unlock-subgraph-${LOCK_UNLOCK_VERSION}.jar
ARG LOCAL_DOCKER_DIR=docker

## ---- Stage: Download and build java.
FROM eclipse-temurin:${JAVA_VERSION}-alpine AS base

ARG LOCK_UNLOCK_VERSION
ARG LOCK_UNLOCK_JAR
ARG LOCAL_DOCKER_DIR
ARG DATA_DIR
ARG RUN_DIR
RUN echo && echo "==== Docker build for Lock-Unlock Subgraph ${LOCK_UNLOCK_VERSION} ====" && echo

## -- Build lock-unlock-subgraph jar.
WORKDIR /build
COPY . .
RUN ./gradlew build -x test

RUN cp build/libs/${LOCK_UNLOCK_JAR} ${RUN_DIR}/${LOCK_UNLOCK_JAR}

WORKDIR ${RUN_DIR}

COPY ${LOCAL_DOCKER_DIR}/entrypoint.sh .

## ---- Stage: Build runtime
FROM amazoncorretto:17-alpine3.19

## Import ARGs
ARG RUN_DIR
ARG DATA_DIR
ARG LOCK_UNLOCK_JAR

COPY --from=base ${RUN_DIR}/${LOCK_UNLOCK_JAR} ${RUN_DIR}/${LOCK_UNLOCK_JAR}
COPY --from=base ${RUN_DIR} ${RUN_DIR}

WORKDIR $RUN_DIR

RUN ls -al

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

COPY ./data ${DATA_DIR}

RUN chmod a+x entrypoint.sh

## Default environment variables.
ENV \
    JAVA_OPTIONS="-Xmx2048m -Xms2048m"  \
    LOCK_UNLOCK_JAR="${LOCK_UNLOCK_JAR}"          \
    RUN_DIR="${RUN_DIR}"

EXPOSE 8080

ENTRYPOINT ["./entrypoint.sh" ]
CMD []