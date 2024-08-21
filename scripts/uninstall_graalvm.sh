#!/bin/zsh
set -euo pipefail

echo "Uninstalling GraalVM JDK 22 and native-image..."

# Check if GraalVM JDK 22 is installed via Homebrew
if brew list --cask | grep -q "graalvm-jdk22"; then
    echo "Uninstalling GraalVM JDK 22 Homebrew cask..."
    brew remove --cask graalvm/tap/graalvm-community-jdk22
else
    echo "GraalVM JDK 22 Homebrew cask not found."
fi

# Find GraalVM installation directory
GRAALVM_DIR="/Library/Java/JavaVirtualMachines/graalvm-community-openjdk-22"

if [ -d "$GRAALVM_DIR" ]; then
    # Remove GraalVM directory
    echo "Removing GraalVM directory..."
    sudo rm -rf "$GRAALVM_DIR"
    echo "GraalVM directory removed."
else
    echo "GraalVM JDK 22 installation directory not found."
fi

# Clean up .zshrc
echo "Cleaning up .zshrc..."
sed -i '' '/# GraalVM configuration/d' ~/.zshrc
sed -i '' '/GRAALVM_HOME/d' ~/.zshrc
sed -i '' '/export PATH=.*graalvm/d' ~/.zshrc
sed -i '' '/export JAVA_HOME.*graalvm/d' ~/.zshrc

echo "Uninstallation complete. Please restart your terminal for changes to take effect."
