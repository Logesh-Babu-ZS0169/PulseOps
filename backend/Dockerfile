# ---------- Stage 1: Build ----------
FROM maven:3.8.6-eclipse-temurin-11 AS build

# Set working directory
WORKDIR /app

# Copy only the pom.xml first (for dependency caching)
COPY pom.xml .

# Download all dependencies with retries and timeouts to prevent partial downloads
RUN mvn dependency:go-offline -B \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
    -Dmaven.wagon.http.connectionTimeout=60000 \
    -Dmaven.wagon.http.readTimeout=60000

# Copy the source code after dependencies are cached
COPY src ./src

# Package the application (skip tests for faster build)
RUN mvn clean package -DskipTests -B

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:11-jre

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8181

# Set Java memory options (adjust as needed)
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
