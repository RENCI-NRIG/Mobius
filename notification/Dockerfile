FROM openjdk:8-jre-alpine
MAINTAINER Komal Thareja<komal.thareja@gmail.com>
ARG JAR_FILE
WORKDIR /code
ADD target/${JAR_FILE} /code/notificationSink.jar
VOLUME ["/code"]
CMD ["java", "-jar", "/code/notificationSink.jar"]
