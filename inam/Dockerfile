# Fork addition (branch "inam"): builds Structurizr from source and packages it.
# The runtime stage mirrors structurizr-application/Dockerfiles/eclipse-temurin-alpine.
#
# Build from the repository root (or use inam/build.sh):
#   docker build -f inam/Dockerfile --build-arg APP_REVISION="$(cat inam/VERSION)" -t structurizr-inam:"$(cat inam/VERSION)" .

ARG BUILD_IMAGE=eclipse-temurin:25-jdk
ARG RUNTIME_IMAGE=eclipse-temurin:25-jre-alpine

FROM ${BUILD_IMAGE} AS build
ARG APP_REVISION
RUN test -n "${APP_REVISION}" || (echo "The APP_REVISION build argument is required" >&2 && exit 1)
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -DskipTests -Dapp.revision="${APP_REVISION}" -pl structurizr-application -am package

FROM ${RUNTIME_IMAGE}
ARG APP_REVISION
LABEL org.opencontainers.image.title="Structurizr (inam fork)" \
      org.opencontainers.image.version="${APP_REVISION}" \
      org.opencontainers.image.source="https://github.com/InomjonKadirov/structurizr" \
      org.opencontainers.image.licenses="Apache-2.0"

ENV PORT=8080

COPY --from=build /src/structurizr-application/target/structurizr-*.war /usr/local/structurizr.war
COPY structurizr-themes /usr/local/structurizr-themes

EXPOSE ${PORT}

WORKDIR /usr/local/structurizr

ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-Dio.netty.noUnsafe=true", "--enable-native-access=ALL-UNNAMED", "-jar", "/usr/local/structurizr.war"]
