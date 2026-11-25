ARG ARTIFACT_SOURCE=compile

FROM maven:3.9.16-eclipse-temurin-21-alpine AS compile
WORKDIR /build
COPY pom.xml ./
COPY src ./src
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml mvn -B -ntp -DskipTests package

FROM scratch AS prebuilt
COPY target/academia.jar /build/target/academia.jar

FROM ${ARTIFACT_SOURCE} AS artifact

FROM eclipse-temurin:21-jre-alpine
COPY --from=artifact --chmod=444 /build/target/academia.jar /opt/academia.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/opt/academia.jar"]
