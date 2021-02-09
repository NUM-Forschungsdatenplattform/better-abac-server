FROM openjdk:11-jdk-alpine
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
COPY target/plugins/num-consent-plugin.zip plugins/num-consent-plugin.zip
ENTRYPOINT ["java","-jar","/app.jar"]
