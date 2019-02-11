FROM openjdk:8-jre-alpine
MAINTAINER Komal Thareja<komal.thareja@gmail.com>

WORKDIR /code
ADD target/mobius-spring-1.0.0-SNAPSHOT.jar /code/mobius-spring-1.0.0-SNAPSHOT.jar
EXPOSE 8080
VOLUME ["/code"]
CMD ["java", "-jar", "/code/mobius-spring-1.0.0-SNAPSHOT.jar"]
