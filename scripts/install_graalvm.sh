#!/bin/zsh
set -euo pipefail

echo "Checking if Homebrew is installed..."
if ! command -v brew &> /dev/null; then
    echo "Homebrew is not installed. Please install it first."
    echo "You can install Homebrew by running:"
    echo '/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"'
    exit 1
fi

echo "Installing GraalVM JDK 22..."
if brew install --cask graalvm/tap/graalvm-community-jdk22; then
    echo "GraalVM JDK 22 installed successfully."
else
    echo "Failed to install GraalVM JDK 22."
    exit 1
fi

GRAALVM_HOME="/Library/Java/JavaVirtualMachines/graalvm-community-openjdk-22/Contents/Home"

echo "Verifying installation..."
if java --version &> /dev/null; then
    echo "Java is installed and working correctly."
else
    echo "Failed to run java from GraalVM. Installation might be incomplete."
    exit 1
fi

echo "Updating zsh configuration..."
sed -i '' '/# GraalVM configuration/d' ~/.zshrc
sed -i '' '/GRAALVM_HOME/d' ~/.zshrc
sed -i '' '/export PATH=.*graalvm/d' ~/.zshrc
sed -i '' '/export JAVA_HOME/d' ~/.zshrc

echo "# GraalVM configuration" >> ~/.zshrc
echo "export GRAALVM_HOME=$GRAALVM_HOME" >> ~/.zshrc
echo "export JAVA_HOME=\$GRAALVM_HOME" >> ~/.zshrc
echo "export PATH=\$GRAALVM_HOME/bin:\$PATH" >> ~/.zshrc

echo "Verifying native-image installation..."
if command -v native-image &> /dev/null; then
    echo "native-image is installed. Verifying version:"
    native-image --version
else
    echo "native-image is not found. This is unexpected for GraalVM JDK 22."
    echo "Please check your installation or consult the GraalVM documentation."
fi

echo "GraalVM setup complete. Please restart your terminal or run 'source ~/.zshrc' to apply changes."

# Verify ARM64 installation
if java -XshowSettings:properties -version 2>&1 | grep -q "os.arch = aarch64"; then
    echo "Successfully installed ARM64 version of GraalVM JDK 22."
else
    echo "Warning: The installed version may not be ARM64. Please verify."
fi
