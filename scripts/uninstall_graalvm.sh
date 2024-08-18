#!/bin/zsh
set -euo pipefail

echo "Uninstalling GraalVM and native-image..."

# Find GraalVM installation
GRAALVM_DIR=$(find /Library/Java/JavaVirtualMachines -name "graalvm-ce-java17-*" -type d | sort -V | tail -n 1)

if [ -z "$GRAALVM_DIR" ]; then
    echo "GraalVM installation not found."
else
    GRAALVM_HOME="$GRAALVM_DIR/Contents/Home"
    
    # Uninstall native-image if it exists
    if [ -f "$GRAALVM_HOME/bin/native-image" ]; then
        echo "Uninstalling native-image..."
        sudo "$GRAALVM_HOME/bin/gu" remove native-image
    else
        echo "native-image not found, skipping its uninstallation."
    fi

    # Remove GraalVM directory
    echo "Removing GraalVM directory..."
    sudo rm -rf "$GRAALVM_DIR"
    echo "GraalVM directory removed."
fi

# Remove Homebrew GraalVM cask if it exists
if brew list --cask | grep -q "graalvm"; then
    echo "Uninstalling GraalVM Homebrew cask..."
    brew uninstall --cask graalvm-ce-java17
fi

# Clean up .zshrc
echo "Cleaning up .zshrc..."
sed -i '' '/# GraalVM configuration/d' ~/.zshrc
sed -i '' '/GRAALVM_HOME/d' ~/.zshrc
sed -i '' '/export PATH=.*graalvm/d' ~/.zshrc

echo "Uninstallation complete. Please restart your terminal for changes to take effect."
