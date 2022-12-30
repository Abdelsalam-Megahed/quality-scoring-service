# Build stage
FROM gradle:latest AS BUILD
ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME
COPY build.gradle settings.gradle $APP_HOME
COPY . .
RUN gradle build
RUN gradle installDist

# Package stage
FROM openjdk:11-jre-slim
ENV JAR_NAME=test-task-1.0-SNAPSHOT.jar
ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME
COPY --from=BUILD $APP_HOME .
EXPOSE 8080
ENTRYPOINT exec java -jar $APP_HOME/build/libs/$JAR_NAME