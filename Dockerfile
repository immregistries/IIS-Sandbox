FROM maven:3.5.2-jdk-8-alpine AS build

COPY src /usr/src/app/src
COPY pom.xml /usr/src/app

RUN mvn -f /usr/src/app/pom.xml clean package -Dmaven.test.skip=true

FROM tomcat:9.0.54-jre8-openjdk-slim
COPY --from=build /usr/src/app/target/iis-sandbox.war $CATALINA_HOME/webapps

EXPOSE 8080
