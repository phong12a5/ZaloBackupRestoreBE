FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/device-management-service-1.0.0.jar app.jar
# Port mặc định cho service này, bạn có thể thay đổi nếu cần
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
