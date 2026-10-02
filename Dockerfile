# ============================================================
# Stage 1: Build
# ============================================================
FROM maven:3.8.6-openjdk-8-slim AS builder

WORKDIR /workspace

# Copy dependency descriptor first for layer caching
COPY pom.xml .

# Download all dependencies (cached layer unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy the full source tree
COPY src ./src

# Build the WAR, skipping tests
RUN mvn clean package -DskipTests -B

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:8-jre

LABEL maintainer="HMS Team" \
      application="Doctor-Patient-Portal" \
      version="0.0.1-SNAPSHOT"

# Timezone
ENV TZ=UTC

# JVM tuning
ENV JAVA_OPTS="-Xms256m -Xmx512m \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -Djava.security.egd=file:/dev/./urandom"

# Application environment variables
ENV DB_HOST=localhost \
    DB_PORT=3306 \
    DB_NAME=hospital \
    DB_USER=root \
    DB_PASSWORD=changeme \
    REDIS_HOST=localhost \
    REDIS_PORT=6379

# Install Tomcat 9 (supports Servlet 4.0 / Java 8)
ENV CATALINA_HOME=/opt/tomcat
ENV PATH=$CATALINA_HOME/bin:$PATH

RUN set -eux; \
    apt-get update -qq && apt-get install -y --no-install-recommends ca-certificates && \
    rm -rf /var/lib/apt/lists/*; \
    groupadd -r tomcat && useradd -r -g tomcat -d $CATALINA_HOME -s /sbin/nologin tomcat; \
    mkdir -p $CATALINA_HOME; \
    TOMCAT_VERSION=9.0.85; \
    TOMCAT_URL="https://archive.apache.org/dist/tomcat/tomcat-9/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz"; \
    apt-get update -qq && apt-get install -y --no-install-recommends wget && \
    wget -q "$TOMCAT_URL" -O /tmp/tomcat.tar.gz && \
    tar -xzf /tmp/tomcat.tar.gz -C $CATALINA_HOME --strip-components=1 && \
    rm /tmp/tomcat.tar.gz && \
    apt-get purge -y --auto-remove wget && \
    rm -rf /var/lib/apt/lists/*; \
    rm -rf $CATALINA_HOME/webapps/ROOT \
           $CATALINA_HOME/webapps/examples \
           $CATALINA_HOME/webapps/docs \
           $CATALINA_HOME/webapps/host-manager \
           $CATALINA_HOME/webapps/manager; \
    chown -R tomcat:tomcat $CATALINA_HOME

# Deploy WAR
COPY --from=builder /workspace/target/Doctor-Patient-Portal.war $CATALINA_HOME/webapps/ROOT.war

RUN chown tomcat:tomcat $CATALINA_HOME/webapps/ROOT.war

USER tomcat

EXPOSE 8080

CMD ["catalina.sh", "run"]
