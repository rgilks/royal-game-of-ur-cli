# Royal Game of Ur - Justfile

# List all available commands
default:
    @just --list

# ======================
# Project Initialization
# ======================

# Initialize the entire project
init: setup-scripts setup-asdf install-tools setup-hooks
    yarn install
    @echo "Project initialized successfully!"

# Set up script permissions
setup-scripts:
    chmod +x scripts/*.sh
    @echo "Scripts are now executable."

# Set up asdf version manager
setup-asdf:
    asdf plugin add clojure nodejs yarn awscli || true
    asdf install

# Install additional tools
install-tools: install-nbb install-graphviz install-graalvm verify-graalvm

# Set up Git hooks
setup-hooks:
    mkdir -p .git/hooks
    cp scripts/pre_commit.sh .git/hooks/pre_commit
    chmod +x .git/hooks/pre_commit
    @echo "Git hooks have been set up successfully."

# =================
# Tool Installation
# =================

# Install the latest version of nbb
install-nbb:
    yarn global add nbb

# Install Graphviz for generating state diagrams
install-graphviz:
    brew install graphviz

# Install and set up GraalVM
install-graalvm:
    ./scripts/install_graalvm.sh

# Verify GraalVM installation
verify-graalvm:
    ./scripts/verify_graalvm.sh

# Uninstall GraalVM and native-image
uninstall-graalvm:
    ./scripts/uninstall_graalvm.sh

# =================
# Development Tasks
# =================

# Format Clojure files
fmt:
    @echo "Formatting Clojure files..."
    clojure -M:dev:cljfmt fix

# Generate a state diagram
state-diagram:
    dot -Tpng -Gdpi=300 ./docs/rgou-fsm.dot -o ./docs/rgou-fsm.png

# Concatenate all relevant files (for an LLM to read)
concat:
    ./scripts/concat_files.sh . justfile Dockerfile .cljc .cljs .clj .md .dot .edn .json -- node_modules/ .clj-kondo/ reflect-config.json resource-config.json build.clj .cljfmt.edn

# Concatenate all important files clojure
cci:
    ./scripts/concat_files.sh . justfile Dockerfile .cljc .cljs .clj .edn -- node_modules/ ./test/ .clj-kondo/ reflect-config.json resource-config.json .cljfmt.edn nbb.edn

# Concatenate just the program (for an LLM to read)
cc:
    ./scripts/concat_files.sh . README.md .cljc .cljs .clj -- build.clj ./test/

# ==================
# Update and Upgrade
# ==================

# Show out-dated Clojure dependencies
ancient:
    clojure -Sdeps '{:deps {com.github.liquidz/antq {:mvn/version "RELEASE"}}}' \
    -m antq.core

# Update all dependencies and tools
update: update-deps update-tools ancient

# Update the node dependencies
update-deps:
    yarn upgrade

# Update all tools to their latest versions
update-tools:
    ./scripts/update_tools.sh

# =======
# Testing
# =======

# Run all unit tests (nbb)
test:
    yarn test

# Run unit tests and watch for changes (nbb)
watch:
    yarn test:watch

# Run all unit tests (Clojure)
test-clj:
    clojure -M:test

# =================
# Build and Execute
# =================

# Build the project
build:
    ./scripts/build_project.sh

# Run a simulation with custom parameters (nbb)
nbb *ARGS:
    yarn cli {{ARGS}}

# Run a simulation with custom parameters (Clojure)
clj *ARGS:
    clojure -M:clj {{ARGS}}

# =================
# Utility Commands
# =================

# Run a REPL (Clojure)
repl:
    clojure -M:dev

# Clean the project (remove build artifacts)
clean:
    rm -rf target
    rm -rf .cpcache

# =================
# ECR Commands
# =================

ECR_PUBLIC_REGISTRY := "public.ecr.aws/n1r2w5d4"
ECR_PRIVATE_REGISTRY := "250357559699.dkr.ecr.eu-west-1.amazonaws.com"
ECR_REPOSITORY := "rgou"
REGION := "eu-west-1"
LAMBDA_FUNCTION := "ur-test"

# Authenticate Docker to Amazon ECR public registry
ecr-login-public:
    aws ecr-public get-login-password --region us-east-1 | docker login --username AWS --password-stdin {{ECR_PUBLIC_REGISTRY}}

# Authenticate Docker to Amazon ECR private registry
ecr-login-private:
    aws ecr get-login-password --region {{REGION}} | docker login --username AWS --password-stdin {{ECR_PRIVATE_REGISTRY}}

# Tag Docker image for public ECR
ecr-tag-public:
    docker tag royal-game-of-ur:latest {{ECR_PUBLIC_REGISTRY}}/{{ECR_REPOSITORY}}:latest

# Tag Docker image for private ECR
ecr-tag-private:
    #!/usr/bin/env bash
    set -e
    VERSION=$(cat version.txt)
    docker tag royal-game-of-ur:latest {{ECR_PRIVATE_REGISTRY}}/{{ECR_REPOSITORY}}:v${VERSION}

# Push Docker image to public ECR
ecr-push-public:
    docker push {{ECR_PUBLIC_REGISTRY}}/{{ECR_REPOSITORY}}:latest

# Push Docker image to private ECR
ecr-push-private:
    #!/usr/bin/env bash
    set -e
    VERSION=$(cat version.txt)
    docker push {{ECR_PRIVATE_REGISTRY}}/{{ECR_REPOSITORY}}:v${VERSION}

# Increment version
increment-version:
    #!/usr/bin/env bash
    set -e
    VERSION=$(cat version.txt)
    NEW_VERSION=$((VERSION + 1))
    echo $NEW_VERSION > version.txt
    echo "Version incremented to $NEW_VERSION"

# Update Lambda function
update-lambda:
    #!/usr/bin/env bash
    set -e
    VERSION=$(cat version.txt)
    aws lambda update-function-code \
    --function-name {{LAMBDA_FUNCTION}} \
    --image-uri {{ECR_PRIVATE_REGISTRY}}/{{ECR_REPOSITORY}}:v${VERSION} \
    --region {{REGION}} \
    --profile tre \
    --no-cli-pager

# Login, tag, push Docker image to public ECR
ecr-deploy-public: ecr-login-public ecr-tag-public ecr-push-public
    @echo "Image successfully pushed to public ECR"

# Login, tag, push Docker image to private ECR, and update Lambda
deploy: ecr-login-private ecr-tag-private ecr-push-private update-lambda increment-version
    @echo "Image successfully pushed to private ECR, Lambda updated, and version incremented"

# =================
# Docker Commands
# =================

# Build Docker image
docker-build:
    docker build --platform linux/arm64 -t royal-game-of-ur .

# Run the application in a Docker container with passed arguments
docker *ARGS:
    docker run --platform linux/arm64 -it --rm royal-game-of-ur {{ARGS}} icons=simple

# Build and run in Docker with passed arguments
docker-build-run *ARGS: ecr-login-public docker-build
    just docker play {{ARGS}}

# Build and deploy
docker-build-deploy *ARGS: ecr-login-private docker-build deploy
     @echo "Image successfully built and deployed"

