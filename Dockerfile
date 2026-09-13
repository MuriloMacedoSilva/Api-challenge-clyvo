# Build separado para manter Maven e fontes fora da imagem final.
FROM maven:3.9.15-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install --yes --no-install-recommends postgresql-client \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 clyvo \
    && useradd --system --uid 10001 --gid 10001 --home-dir /home/clyvo \
        --create-home --shell /usr/sbin/nologin clyvo

WORKDIR /app
COPY --from=build --chown=clyvo:clyvo /workspace/target/*.jar app.jar
COPY --chown=clyvo:clyvo wait-for-postgres.sh /app/wait-for-postgres.sh
RUN chmod 0555 /app/wait-for-postgres.sh \
    && chmod 0444 /app/app.jar

USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["/app/wait-for-postgres.sh"]
