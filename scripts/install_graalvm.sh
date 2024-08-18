#!/bin/zsh
set -euo pipefail

GRAALVM_VERSION="22.3.1"
GRAALVM_DIR="/Library/Java/JavaVirtualMachines/graalvm-ce-java17-$GRAALVM_VERSION"
DOWNLOAD_URL="https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-$GRAALVM_VERSION/graalvm-ce-java17-darwin-amd64-$GRAALVM_VERSION.tar.gz"

echo "Downloading GraalVM..."
curl -L -o graalvm.tar.gz $DOWNLOAD_URL

echo "Extracting GraalVM..."
sudo mkdir -p "$GRAALVM_DIR"
sudo tar -xzf graalvm.tar.gz -C "$GRAALVM_DIR" --strip-components 1
rm graalvm.tar.gz

GRAALVM_HOME="$GRAALVM_DIR/Contents/Home"

echo "Addressing macOS security measures..."
sudo xattr -r -d com.apple.quarantine "$GRAALVM_DIR"

echo "Verifying installation..."
if ! "$GRAALVM_HOME/bin/java" -version; then
    echo "Failed to run java from GraalVM. Installation might be incomplete."
    exit 1
fi

echo "Updating zsh configuration..."
sed -i '' '/# GraalVM configuration/d' ~/.zshrc
sed -i '' '/GRAALVM_HOME/d' ~/.zshrc
sed -i '' '/export PATH=.*graalvm/d' ~/.zshrc

echo "# GraalVM configuration" >> ~/.zshrc
echo "export GRAALVM_HOME=$GRAALVM_HOME" >> ~/.zshrc
echo "export PATH=\$GRAALVM_HOME/bin:\$PATH" >> ~/.zshrc

echo "Installing native-image..."
if ! sudo "$GRAALVM_HOME/bin/gu" install native-image; then
    echo "Failed to install native-image. Please try installing it manually by running:"
    echo "sudo $GRAALVM_HOME/bin/gu install native-image"
    exit 1
fi

echo "GraalVM setup complete. Please restart your terminal or run 'source ~/.zshrc' to apply changes."
