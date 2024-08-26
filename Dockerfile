FROM arm64v8/amazonlinux:2 AS builder

# Install necessary tools
RUN yum update -y && yum install -y tar gzip wget gcc zlib-devel git

# Install GraalVM (use a version known to work with Amazon Linux 2)
RUN wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.2/graalvm-ce-java17-linux-aarch64-22.3.2.tar.gz \
    && tar -xzf graalvm-ce-java17-linux-aarch64-22.3.2.tar.gz \
    && mv graalvm-ce-java17-22.3.2 /usr/lib/graalvm

# Set up environment variables
ENV JAVA_HOME=/usr/lib/graalvm
ENV PATH=$PATH:$JAVA_HOME/bin

# Install necessary tools
RUN gu install native-image

# Install Clojure CLI tools
RUN curl -O https://download.clojure.org/install/linux-install.sh \
    && chmod +x linux-install.sh \
    && ./linux-install.sh

# Set the working directory
WORKDIR /app

# Copy the project files
COPY . /app

# Build the uberjar
RUN mkdir -p target && clojure -T:build uber

# Create the native image
RUN clojure -M:native-image

# Stage 2: Create the final minimal image
# FROM alpine:latest
FROM public.ecr.aws/lambda/provided:al2-arm64

# Copy the executable from the builder stage
COPY --from=builder /app/royal-game-of-ur /var/task/bootstrap

# Copy the entry point script
COPY docker-entrypoint.sh /var/task/docker-entrypoint.sh

# Make the scripts executable
RUN chmod +x /var/task/bootstrap /var/task/docker-entrypoint.sh

# Set the entrypoint to our script
ENTRYPOINT ["/var/task/docker-entrypoint.sh"]
