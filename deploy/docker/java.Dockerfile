# Imagen de un proceso Spring Boot (identidad, datos, distributivo, salida o gateway).
# Capas de Spring Boot para que un cambio de código no vuelva a subir las dependencias.
FROM eclipse-temurin:25-jre AS extraccion
WORKDIR /app
ARG JAR
COPY ${JAR} app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extraido

FROM eclipse-temurin:25-jre
RUN groupadd --system --gid 10001 mdd && useradd --system --uid 10001 --gid mdd mdd
WORKDIR /app
COPY --from=extraccion /app/extraido/dependencies/ ./
COPY --from=extraccion /app/extraido/spring-boot-loader/ ./
COPY --from=extraccion /app/extraido/snapshot-dependencies/ ./
COPY --from=extraccion /app/extraido/application/ ./
USER 10001
# El límite de memoria de cada JVM se pasa en JAVA_TOOL_OPTIONS desde el compose (ADR-008, R-19).
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
