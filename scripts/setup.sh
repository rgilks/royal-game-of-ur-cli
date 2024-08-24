#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

echo "Continuing setup for Royal Game of Ur..."

# Install Docker
if ! command_exists docker; then
    echo "Installing Docker..."
    brew install --cask docker
    echo "Please start Docker manually after the installation."
else
    echo "Docker is already installed."
fi

# Install Just
if ! command_exists just; then
    echo "Installing Just..."
    brew install just
else
    echo "Just is already installed."
fi

# Install asdf
if ! command_exists asdf; then
    echo "Installing asdf..."
    brew install asdf
    echo -e "\n. $(brew --prefix asdf)/libexec/asdf.sh" >> ~/.zshrc
    source ~/.zshrc
else
    echo "asdf is already installed."
fi

echo "Setup complete. You can now run 'just init' to initialize the project."
