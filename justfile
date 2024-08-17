# List all available commands
default:
    @just --list

# ======================
# Project Initialization
# ======================

# Initialize the project by configuring asdf, installing tools, setting up Git hooks
init: setup-asdf setup-hooks install-graphviz install-nbb
    yarn install
    @echo "Project initialized successfully!"

# Set up asdf and install required plugins and versions
setup-asdf:
    asdf plugin add clojure || true
    asdf plugin add java || true
    asdf plugin add just || true
    asdf plugin add nodejs || true
    asdf plugin add yarn || true
    
    asdf install

# Install the latest version of nbb
install-nbb:
    yarn global add nbb

# Set up Git hooks and install the latest version of cljfmt
setup-hooks:
    clj -Ttools install io.github.weavejester/cljfmt '{:git/tag "0.12.0"}' :as cljfmt
    mkdir -p .git/hooks
    cp scripts/pre-commit.sh .git/hooks/pre-commit
    chmod +x .git/hooks/pre-commit
    @echo "Git hooks have been set up successfully with the latest cljfmt and test runner."

# Install Graphviz for generating state diagrams
install-graphviz:
    brew install graphviz

# =================
# Development Tasks
# =================

# Format clojure
fmt:
    @echo "Formatting Clojure files..."
    clj -Tcljfmt fix

# Concatinate all relevant files (for an LLM to read)
concat:
    chmod +x ./scripts/concat-files.sh
    set -euo pipefail
    ./scripts/concat-files.sh . cljc cljs clj md dot edn json

# Generate a state diagram
state-diagram:
    dot -Tpng -Gdpi=300 ./docs/rgou-fsm.dot -o ./docs/rgou-fsm.png

# Update the node dependencies
update:
    yarn update

# Update all tools to their latest versions
update-tools:
    #!/usr/bin/env bash
    set -euo pipefail
    while read -r tool version; do
        if [ -n "$tool" ] && [ -n "$version" ]; then
            echo "Updating $tool..."
            if [ "$tool" = "java" ]; then
                # Extract the major version and distribution from the current version
                distribution=$(echo $version | cut -d'-' -f1)
                major_version=$(echo $version | cut -d'-' -f2 | cut -d'.' -f1)
                latest_version=$(asdf latest $tool $distribution-$major_version)
            else
                latest_version=$(asdf latest $tool)
            fi
            asdf install $tool $latest_version
            asdf local $tool $latest_version
        fi
    done < .tool-versions
    echo "All tools updated. New versions:"
    cat .tool-versions

# =======
# Testing
# =======

# Run unit tests only
test:
    yarn test

# Run unit tests and watch for changes
watch:
    yarn test:watch

# ===
# Run
# ===

# Run the cli application
cli:
    yarn cli

# Simulate a full game running in autopilot mode
sim:
    yarn sim
