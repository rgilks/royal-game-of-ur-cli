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
    ./scripts/concat_files.sh

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

# Run all unit tests
test:
    yarn test

# Run unit tests and watch for changes
test-watch:
    yarn test:watch

# =================
# Build and Execute
# =================

# Build the project
build:
    ./scripts/build_project.sh

# Run the CLI application
run:
    yarn cli

# Run a simulation with custom parameters
sim *args:
    yarn sim {{args}}
