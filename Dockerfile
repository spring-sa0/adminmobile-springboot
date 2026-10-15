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
LABEL authors="datchemi"

RUN groupadd --system spring && useradd --system --gid spring spring \
    && mkdir -p /app/logs && chown -R spring:spring /app
USER spring:spring

WORKDIR /app
COPY --from=build --chown=spring:spring /build/target/app.jar app.jar

# Journaux adminmobile.log / audit.log (voir logback-spring.xml) : monter un volume ici pour les
# conserver quand le conteneur est recree (voir docker-compose.yml).
ENV LOG_DIR=/app/logs
VOLUME ["/app/logs"]

EXPOSE 8080

# Permet de passer des options JVM (ex. -Xmx512m) sans reconstruire l'image.
ENV JAVA_OPTS=""

# Interroge /actuator/health, qui verifie aussi la connexion a la base : le conteneur passe
# "unhealthy" si Oracle est injoignable, pas seulement si le port ne repond plus.
# (Requete HTTP via /dev/tcp de bash : l'image JRE ne fournit ni curl ni wget.)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/${SERVER_PORT:-8080} && printf "GET /actuator/health HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 && grep -q "\"status\":\"UP\"" <&3' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]