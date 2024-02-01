## This Dockefile builds a reduced footprint container.

ARG JAVA_VERSION=17

ARG ALPINE_VERSION=3.17.1
ARG LOCK_UNLOCK_VERSION=""

# Internal, passed between stages.
ARG DATA_DIR=/data
ARG RUN_DIR=/run
ARG LOCK_UNLOCK_JAR=lock-unlock-subgraph-${LOCK_UNLOCK_VERSION}.jar
ARG JAVA_MINIMAL=/opt/java-minimal
ARG LOCAL_DOCKER_DIR=docker

## ---- Stage: Download and build java.
FROM eclipse-temurin:${JAVA_VERSION}-alpine AS base

ARG JAVA_MINIMAL
ARG LOCK_UNLOCK_VERSION
ARG LOCK_UNLOCK_JAR
ARG LOCAL_DOCKER_DIR
ARG DATA_DIR
ARG RUN_DIR
RUN echo && echo "==== Docker build for Lock-Unlock Subgraph ${LOCK_UNLOCK_VERSION} ====" && echo

## -- Build lock-unlock-subgraph jar.
WORKDIR /build
COPY . .
RUN gradlew build

RUN cp build/libs/${LOCK_UNLOCK_JAR} ${RUN_DIR}/${LOCK_UNLOCK_JAR}

WORKDIR ${RUN_DIR}

## -- Make reduced Java JDK

ARG JDEPS_EXTRA="jdk.crypto.cryptoki,jdk.crypto.ec"
RUN \
  JDEPS="$(jdeps --multi-release base --print-module-deps --ignore-missing-deps ${LOCK_UNLOCK_JAR})"  && \
  jlink \
        --compress 2 --strip-debug --no-header-files --no-man-pages \
        --output "${JAVA_MINIMAL}" \
        --add-modules "${JDEPS},${JDEPS_EXTRA}"

COPY ${LOCAL_DOCKER_DIR}/entrypoint.sh .
COPY ./data /data

## ---- Stage: Build runtime
FROM alpine:${ALPINE_VERSION}

## Import ARGs
ARG JAVA_MINIMAL
ARG RUN_DIR
ARG LOCK_UNLOCK_JAR

COPY --from=base /opt/java-minimal /opt/java-minimal
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
    chown -R $LOCK_UNLOCK_USER ${RUN_DIR}

USER $LOCK_UNLOCK_USER

RUN \
    mkdir -p $LOGS && \
    mkdir -p $DATA && \
    chmod a+x entrypoint.sh

## Default environment variables.
ENV \
    JAVA_HOME=${JAVA_MINIMAL}           \
    JAVA_OPTIONS="-Xmx2048m -Xms2048m"  \
    LOCK_UNLOCK_JAR="${LOCK_UNLOCK_JAR}"          \
    RUN_DIR="${RUN_DIR}"

EXPOSE 3030

ENTRYPOINT ["./entrypoint.sh" ]
CMD []