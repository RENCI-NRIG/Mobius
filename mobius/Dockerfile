FROM openjdk:8-jre-alpine
MAINTAINER Komal Thareja<komal.thareja@gmail.com>
ARG JAR_FILE
WORKDIR /code
ADD target/${JAR_FILE} /code/mobius-spring.jar
EXPOSE 8080
VOLUME ["/code"]
CMD ["java", "-jar", "/code/mobius-spring.jar"]
