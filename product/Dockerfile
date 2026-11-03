FROM eclipse-temurin:21-jre
WORKDIR /app
# 먼저 로컬에서 .\gradlew.bat bootJar -x test 로 jar를 만든 뒤 이미지를 빌드합니다.
COPY build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
