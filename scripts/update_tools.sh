#!/usr/bin/env zsh
set -euo pipefail
while read -r tool version; do
    if [ -n "$tool" ] && [ -n "$version" ]; then
        echo "Updating $tool..."
        if [ "$tool" = "java" ]; then
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
