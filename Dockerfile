# Build stage
FROM eclipse-temurin:17-jdk as build
WORKDIR /workspace/app

# Copy Maven files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Make the Maven wrapper script executable
RUN chmod +x ./mvnw

# Build the application
RUN ./mvnw install -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

# Final stage (runtime)
FROM eclipse-temurin:17-jre
VOLUME /tmp

# Copy the wait-for-it.sh script into the image
COPY wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh

# Set the working directory for the app
WORKDIR /app

# Copy the built application
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app


# Copy the built application JAR from the build stage
COPY --from=build /workspace/app/target/*.jar /app.jar

# Expose the port that your app runs on
EXPOSE 8080


# Add wait-for-it script
COPY wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh

# Add entrypoint wrapper script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Set the entrypoint
ENTRYPOINT ["/entrypoint.sh"]