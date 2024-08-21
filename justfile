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
    asdf plugin add clojure nodejs yarn || true
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
    ./scripts/concat_files.sh . justfile .cljc .cljs .clj .md .dot .edn .json -- node_modules/ .clj-kondo/ reflect-config.json resource-config.json build.clj .cljfmt.edn

# Concatenate just the program files (for an LLM to read)
cc:
    ./scripts/concat_files.sh . README.md .cljc .cljs .clj -- build.clj ./test/

# ==================
# Update and Upgrade
# ==================

# Update all dependencies and tools
update: update-deps update-tools

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

# Run the CLI application (nbb)
run:
    yarn cli

# Run the CLI application (Clojure)
run-clj:
    clojure -M:run

# Run a simulation with custom parameters (nbb)
# Usage: just sim-nbb num-games=<num> strategy-A=<strategy> strategy-A-<param>=<value> ... strategy-B=<strategy> strategy-B-<param>=<value> ... debug?=<bool> show?=<bool> delay=<num> parallel=<num> validate=<bool>
sim-nbb *args:
    yarn sim {{args}}

# Run a simulation with custom parameters (Clojure)
# Usage: just sim num-games=<num> strategy-A=<strategy> strategy-A-<param>=<value> ... strategy-B=<strategy> strategy-B-<param>=<value> ... debug?=<bool> show?=<bool> delay=<num> parallel=<num> validate=<bool>
sim *args:
    clojure -M:sim {{args}}

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
# Docker Commands
# =================

# Build Docker image
docker-build:
    docker build --platform linux/arm64 -t royal-game-of-ur .

# Run the application in a Docker container with passed arguments
docker-run *args:
    docker run --platform linux/arm64 -it --rm royal-game-of-ur {{args}}

# Build and run in Docker with passed arguments
docker-build-run *args:
    just docker-build
    just docker-run {{args}}

# Run a simulation with custom parameters in Docker
# Usage: just sim-docker num-games=<num> strategy-A=<strategy> strategy-A-<param>=<value> ... strategy-B=<strategy> strategy-B-<param>=<value> ... debug=<bool> show=<bool> delay=<num> parallel=<num> validate=<bool>
sim-docker *args:
    just docker-run {{args}}

