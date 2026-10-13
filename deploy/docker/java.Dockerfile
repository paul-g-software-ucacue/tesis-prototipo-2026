# Imagen de un proceso Spring Boot (identidad, datos, distributivo, salida o gateway).
# Compila desde el código (ADR-013), así que un clon limpio se levanta con un solo
# `docker compose up`; B00-H08 adapta cd.yml a esta etapa. El contexto es backend/.
# Ejemplo: docker build -f deploy/docker/java.Dockerfile --build-arg MODULO=apps:identidad \
#          --build-arg NOMBRE=identidad backend

FROM eclipse-temurin:25-jdk AS compilacion
ARG MODULO
ARG NOMBRE
WORKDIR /src
COPY . .
# La caché de Gradle se comparte entre las cinco imágenes y entre construcciones. sharing=locked
# las construye de a una: Gradle no puede coordinar el bloqueo de su caché entre contenedores.
# Un COPY no puede transformar MODULO, así que el JAR se deja en una ruta fija (/app.jar).
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    ./gradlew --no-daemon --quiet ":${MODULO}:bootJar" \
    && cp "$(echo "${MODULO}" | tr : /)/build/libs/${NOMBRE}.jar" /app.jar

# Capas de Spring Boot para que un cambio de código no vuelva a subir las dependencias.
FROM eclipse-temurin:25-jre AS extraccion
WORKDIR /app
COPY --from=compilacion /app.jar app.jar
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
