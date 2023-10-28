FROM amazoncorretto:20 as builder
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM amazoncorretto:20
COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
COPY --from=builder application/ ./

ENV API_SECRET_KEY=place_holder
EXPOSE 8081

ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
