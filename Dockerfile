FROM gradle:7.5.0-jdk18-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew build -x check

FROM amazoncorretto:18-alpine-jdk
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*-SNAPSHOT.jar /app/app.jar

CMD java -Dserver.port=$PORT $JAVA_OPTS -jar /app/app.jar
