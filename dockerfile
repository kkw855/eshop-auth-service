FROM sbtscala/scala-sbt:eclipse-temurin-21.0.8_9_1.11.5_3.7.2 as builder

WORKDIR /app

COPY . .

RUN sbt assembly

FROM openjdk:21-slim

COPY --from=builder /app/target/scala-3.7.0/eshop-auth-assembly-0.1.0-SNAPSHOT.jar /app/app.jar

# Expose the port Nginx will listen on
EXPOSE 5001

# Command to start Nginx
CMD ["java", "-jar", "/app/app.jar"]
