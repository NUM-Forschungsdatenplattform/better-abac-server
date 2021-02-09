FROM adoptopenjdk/openjdk11:alpine
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
CMD mkdir plugins
CMD chmod 777 plugins
COPY target/plugins/num-consent-plugin.zip plugins/num-consent-plugin.zip
ENTRYPOINT ["java","-jar","/app.jar"]
