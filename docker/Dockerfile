FROM eclipse-temurin:21-jdk
MAINTAINER napster@npstr.space

ENV ENV docker

WORKDIR /opt/baymax

RUN touch baymax.sqlite

ENTRYPOINT ["java", "-Xmx256m", "-jar", "baymax.jar"]

COPY build/libs/baymax.jar /opt/baymax/baymax.jar
