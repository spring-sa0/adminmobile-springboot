LABEL authors="datchemi"

# ---- Etape de build ---------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache les dependances Maven dans une couche separee du code source, pour ne
# pas tout re-telecharger a chaque build quand seul le code change.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package && \
    cp target/adminmobile-*.jar target/app.jar

# ---- Etape d'execution -------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

WORKDIR /app
COPY --from=build --chown=spring:spring /build/target/app.jar app.jar

EXPOSE 8080

# Permet de passer des options JVM (ex. -Xmx512m) sans reconstruire l'image.
ENV JAVA_OPTS=""

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/${SERVER_PORT:-8080}' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]