# Stage 1: Build the application
FROM ghcr.io/graalvm/graalvm-ce:latest as builder

# Install necessary tools
RUN gu install native-image

# Install Git and other dependencies
RUN microdnf install -y git curl

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
FROM alpine:latest

# Install glibc on Alpine
RUN apk add --no-cache libc6-compat

# Copy the executable from the builder stage
COPY --from=builder /app/royal-game-of-ur /royal-game-of-ur

# Set the entrypoint
ENTRYPOINT ["/royal-game-of-ur"]
