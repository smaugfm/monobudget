FROM gradle:6.9.2-jdk11-alpine as BUILD

WORKDIR /opt/app
COPY *.kts ./
COPY *.properties ./
RUN gradle dependencies
COPY src ./src
RUN gradle shadowJar

FROM openjdk:11-jre-slim

COPY --from=BUILD /opt/app/build/libs/ynab-mono-*-fat.jar /opt/app/ynab-mono.jar
ARG WEBHOOK_PORT=80
EXPOSE $WEBHOOK_PORT
