FROM openjdk:11-jre-slim

WORKDIR /opt/app

COPY build/libs/monobudget-*-fat.jar /opt/app/monobudget.jar
ARG WEBHOOK_PORT=80
EXPOSE $WEBHOOK_PORT
EXPOSE $JAVA_OPTIONS

CMD ["java", "-jar", "monobudget.jar", "${JAVA_OPTIONS}"]
