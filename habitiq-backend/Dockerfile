FROM eclipse-temurin:21-jre
ARG SERVICE_NAME
WORKDIR /app

# Copy the pre-built JAR from the host machine's target folder
COPY ${SERVICE_NAME}/target/${SERVICE_NAME}-1.0.0-SNAPSHOT.jar /app/app.jar

RUN apt-get update && apt-get install -y curl ca-certificates --no-install-recommends && rm -rf /var/lib/apt/lists/*

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
