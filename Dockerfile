FROM gradle:6.9.2-jdk11-alpine as BUILD

WORKDIR /opt/app
COPY *.kts ./
COPY *.properties ./
RUN gradle dependencies
COPY src ./src
RUN gradle shadowJar

FROM openjdk:11-alpine

COPY --from=BUILD /opt/app/build/libs/ynab-mono-*-fat.jar /bin/runner/ynab-mono.jar
WORKDIR /bin/runner

ARG monoWebhookUrl
ARG monoWebhookPort

CMD java -jar ynab-mono.jar --set-webhook --settings settings.json \
    --mono-webhook-url $monoWebhookUrl \
    --mono-webhook-port $monoWebhookPort
