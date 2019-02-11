FROM openjdk:8-jdk-alpine
MAINTAINER Komal Thareja<komal.thareja@gmail.com>
ARG JAR_FILE
WORKDIR /code1
ADD target/${JAR_FILE} /code1/notificationSink.jar
VOLUME ["/code1"]
CMD ["java", "-jar", "/code1/notificationSink.jar"]
