# List all available commands
default:
    @just --list

# ======================
# Project Initialization
# ======================

# Initialize the project by configuring asdf, installing tools, setting up Git hooks
init: setup-asdf setup-hooks install-nbb install-graphviz install-graalvm
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

# Install and set up GraalVM
install-graalvm:
    #!/usr/bin/env bash
    set -euo pipefail
    
    echo "Installing GraalVM..."
    brew install --cask graalvm/tap/graalvm-ce-java11
    
    echo "Addressing macOS security measures..."
    sudo xattr -rd com.apple.quarantine /Library/Java/JavaVirtualMachines/graalvm-ce-java11-*
    
    # Set JAVA_HOME to point to GraalVM
    GRAALVM_HOME=$(find /Library/Java/JavaVirtualMachines -name "graalvm-ce-java11-*" | sort -V | tail -n 1)/Contents/Home
    echo "export JAVA_HOME=$GRAALVM_HOME" >> ~/.zshrc
    
    # Add GraalVM bin to PATH
    echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> ~/.zshrc
    
    # Reload shell configuration
    source ~/.zshrc 2>/dev/null || echo "Please restart your terminal or run 'source ~/.bashrc' (or ~/.zshrc) to apply changes"
    
    echo "Installing native-image..."
    $GRAALVM_HOME/bin/gu install native-image
    
    echo "GraalVM setup complete. Please restart your terminal or run 'source ~/.bashrc' (or ~/.zshrc) to apply changes."

# Verify GraalVM installation
verify-graalvm:
    #!/usr/bin/env bash
    set -euo pipefail
    
    echo "Verifying GraalVM installation..."
    
    if [ -n "$JAVA_HOME" ] && [[ "$JAVA_HOME" == *"graalvm"* ]]; then
        echo "JAVA_HOME is set correctly to: $JAVA_HOME"
    else
        echo "JAVA_HOME is not set correctly. Please run 'just setup-graalvm' again."
        exit 1
    fi
    
    if command -v native-image >/dev/null 2>&1; then
        echo "native-image is installed and accessible."
    else
        echo "native-image is not found. Please run 'just setup-graalvm' again."
        exit 1
    fi
    
    java -version
    native-image --version
    
    echo "GraalVM verification complete."

# Uninstall GraalVM
uninstall-graalvm:
    #!/usr/bin/env bash
    set -euo pipefail
    
    echo "Uninstalling GraalVM..."
    brew uninstall --cask graalvm-ce-java11
    
    echo "Removing JAVA_HOME and PATH modifications..."
    sed -i '' '/graalvm/d' ~/.bashrc ~/.zshrc
    
    echo "GraalVM uninstalled. Please restart your terminal for changes to take effect."

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

# Buld the project
build:
    #!/usr/bin/env bash
    set -euo pipefail
    
    echo "Creating target directory..."
    mkdir -p target
    
    echo "Building uberjar..."
    clojure -X:uberjar
    
    if [ ! -f target/royal-game-of-ur.jar ]; then
        echo "Error: Jar file not created. Check the compilation output above."
        exit 1
    fi
    
    echo "Creating native image..."
    clojure -M:native-image
    
    if [ ! -f royal-game-of-ur ]; then
        echo "Error: Native image not created. Check the native-image output above."
        exit 1
    fi
    
    echo "Build successful. You can run the game with ./royal-game-of-ur"

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

# Run a simulation with custom parameters
sim *args:
    yarn sim {{args}}
