#!/bin/sh

# Ensure PATH includes common locations for tools
export PATH="/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin:$HOME/.local/bin:$HOME/.orbstack/bin:$PATH"

echo "Running pre-commit hooks..."

echo "Checking Clojure formatting..."
if ! just fmt; then
  echo "Error: just format failed"
  exit 1
fi

# Add the changes made by the format command
# git add -u

echo "Running tests..."
if ! just test; then
  echo "Error: tests failed"
  exit 1
fi

echo "Pre-commit checks passed."
