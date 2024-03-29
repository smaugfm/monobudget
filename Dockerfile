FROM eclipse-temurin:17

WORKDIR /opt/app

COPY build/libs/monobudget-fat.jar /opt/app/monobudget.jar
EXPOSE $WEBHOOK_PORT

CMD ["java", "-jar", "monobudget.jar", "${JAVA_OPTIONS}"]
