#!/bin/bash

# Check if at least two arguments are provided (root folder and at least one file extension)
if [ $# -lt 2 ]; then
    echo "Usage: $0 <root_folder> <file_extension1> [file_extension2] [file_extension3] ..."
    echo "Example: $0 /path/to/folder yml yaml json"
    exit 1
fi

# Root folder
root_folder="$1"
shift

# File extensions
file_extensions=("$@")

# Output file name (using the first extension)
output_file="_compiled.${file_extensions[0]}"

# Temporary file for compilation
temp_file=$(mktemp)

# Function to clean up temporary file
cleanup() {
    rm -f "$temp_file"
}

# Set up trap to ensure cleanup on script exit
trap cleanup EXIT

# Get the absolute path of the output file
output_file_abs=$(realpath "$output_file")

# Function to check if a file should be ignored based on .gitignore patterns
should_ignore() {
    local file="$1"
    local relative_path="${file#$root_folder/}"
    
    while IFS= read -r pattern; do
        # Ignore empty lines and comments
        [[ -z "$pattern" || "$pattern" == \#* ]] && continue
        
        # Remove leading and trailing slashes
        pattern="${pattern#/}"
        pattern="${pattern%/}"
        
        # Convert gitignore pattern to grep pattern
        grep_pattern=$(echo "$pattern" | sed -e 's/\./\\./g' -e 's/\*/.*/g' -e 's/^/^/' -e 's/$/($|\/)/g')
        
        if echo "$relative_path" | grep -Eq "$grep_pattern"; then
            return 0  # Should ignore
        fi
    done < "$root_folder/.gitignore"
    
    return 1  # Should not ignore
}

# Find all files with the specified extensions recursively in the specified root folder, excluding the output file and .gitignore files, then sort them
for ext in "${file_extensions[@]}"; do
    find "$root_folder" -type f -name "*.$ext" -print0
done | sort -z | while IFS= read -r -d '' file; do
    # Get the absolute path of the current file
    file_abs=$(realpath "$file")
    
    # Skip the output file
    if [ "$file_abs" = "$output_file_abs" ]; then
        continue
    fi
    
    # Check if the file should be ignored based on .gitignore
    if should_ignore "$file"; then
        echo "Ignored: $file"
        continue
    fi
    
    # Add a separator with the file path
    echo -e "\n\n;; File: $file\n" >> "$temp_file"
    
    # Append the contents of the file
    cat "$file" >> "$temp_file"
    
    # Add a separator
    # echo -e "\n---\n" >> "$temp_file"
    
    echo "Added: $file"
done

# Move the temporary file to the final output file
mv "$temp_file" "$output_file"

echo "Compilation complete. Output saved to $output_file"
