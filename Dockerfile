FROM openjdk:11-jre-slim

WORKDIR /opt/app

COPY build/libs/monobudget-*-fat.jar /opt/app/monobudget.jar
EXPOSE $WEBHOOK_PORT
EXPOSE $SET_WEBHOOK
EXPOSE $JAVA_OPTIONS
EXPOSE $MONO_WEBHOOK_URL

CMD ["java", "-jar", "monobudget.jar", "${JAVA_OPTIONS}"]
