# Build stage
FROM gradle:jdk8-jammy AS TEMP_BUILD_IMAGE

ENV APP_HOME=/usr/app/

WORKDIR $APP_HOME

COPY build.gradle settings.gradle $APP_HOME

RUN gradle build

COPY . .

# actual container
FROM openjdk:11-jre-slim

ENV ARTIFACT_NAME=test-task-1.0-SNAPSHOT.jar
ENV APP_HOME=/usr/app/

WORKDIR $APP_HOME

COPY --from=TEMP_BUILD_IMAGE $APP_HOME/build/libs/$ARTIFACT_NAME .

EXPOSE 8080

ENTRYPOINT exec java -jar ${ARTIFACT_NAME}