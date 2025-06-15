# Use Java 21
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy Maven files first (for better caching)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download dependencies
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "target/french-auction-scraper-0.0.1-SNAPSHOT.jar"]