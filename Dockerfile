# --- Stage 1: Build the application using Maven ---
# Use an official Maven image with a compatible JDK (e.g., JDK 21)
# Using Eclipse Temurin which is a good choice for Java builds
FROM eclipse-temurin:21-jdk-jammy as builder

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven wrapper files first (to leverage Docker cache)
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies (this layer is cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline

# Copy the rest of the application source code
COPY src ./src

# Package the application using the Maven wrapper
# This will compile the code and create the executable JAR file
RUN ./mvnw package -DskipTests

# --- Stage 2: Create the final lightweight image ---
# Use a smaller base image with just the JRE (Java Runtime Environment)
FROM eclipse-temurin:21-jre-jammy

# Set the working directory
WORKDIR /app

# Define arguments for user/group (optional, good practice for security)
ARG APP_USER=appuser
ARG APP_GROUP=appgroup
ARG UID=1001
ARG GID=1001

# Create group and user
RUN groupadd -g ${GID} ${APP_GROUP} && \
    useradd -u ${UID} -g ${APP_GROUP} -m -s /bin/sh ${APP_USER}

# Copy the executable JAR file built in the 'builder' stage
# Adjust the JAR file name if your pom.xml produces a different name
ARG JAR_FILE=target/api-0.0.1-SNAPSHOT.jar
COPY --from=builder /app/${JAR_FILE} app.jar

# Change ownership to the non-root user
RUN chown ${APP_USER}:${APP_GROUP} app.jar

# Switch to the non-root user
USER ${APP_USER}

# Expose the port the application runs on (defined in application.properties)
EXPOSE 8082

# Define the command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

