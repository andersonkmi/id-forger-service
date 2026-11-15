FROM eclipse-temurin:21
RUN mkdir /opt/app
COPY build/libs/id-forger-api-1.2.0.jar /opt/app
EXPOSE 27110
CMD ["java", "-jar", "/opt/app/id-forger-api-1.2.0.jar"]
