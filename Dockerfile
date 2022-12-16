FROM gradle:7.5-jdk17 AS build
ARG TOKEN
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon

FROM openjdk:11
EXPOSE 8080:8080
ARG TOKEN
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/
ENTRYPOINT ["java","-jar","/app/rock-paper-scissors-lizard-spock-bot-all.jar"]