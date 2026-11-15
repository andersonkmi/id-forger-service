FROM eclipse-temurin:21
RUN mkdir /opt/app
COPY build/libs/id-forger-admin-1.2.0.jar /opt/app
EXPOSE 27111
CMD ["java", "-jar", "/opt/app/id-forger-admin-1.2.0.jar"]
