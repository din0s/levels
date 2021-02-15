FROM gradle:6.8.0-jdk11 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle shadowJar --no-daemon

FROM openjdk:11-jre-slim
RUN useradd -ms /bin/bash levels
COPY --from=build /home/gradle/src/build/libs/*.jar /home/levels/levels.jar
USER levels
WORKDIR /home/levels
ENTRYPOINT [ "java", "-jar", "levels.jar" ]
