# Build with: docker build --tag tokenservice_server .
# Test with: docker run -ti -p 8080:8080 tokenservice_server

FROM adoptopenjdk:11-jre-hotspot
COPY target/lib /usr/src/lib
COPY target/token-service-1.0.0-runner.jar /usr/src/
WORKDIR /usr/src/
CMD java -Dquarkus.http.port=8182 -Xmx64m -jar token-service-1.0.0-runner.jar

#FROM adoptopenjdk:11-jre-hotspot
#COPY target/lib /usr/src/lib
#COPY target/quarkus-app/quarkus-run.jar /usr/src/
#WORKDIR /usr/src/
#CMD java -Dquarkus.http.port=8181 -Xmx64m -jar quarkus-app/quarkus-run.jar

#FROM maven:3.8.4-jdk-11-slim

## Get source code
#COPY src /home/app/src
#COPY pom.xml /home/app

## Build application
#WORKDIR /home/app
#RUN mvn clean package

## Run application
#WORKDIR /home/app/target
#EXPOSE 8181

# The -Xmx64m demanding from the java runtime to have atleast allocated 64m memory
# https://docs.oracle.com/cd/E13222_01/wls/docs61/perform/JVMTuning.html
#CMD java -Dquarkus.http.port=8181 -Xmx64m -jar quarkus-app/quarkus-run.jar
