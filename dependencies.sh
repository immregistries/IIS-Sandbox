#!/bin/bash

# Configuration
JSON_FILE="dependencies.json"

# --- 1. PREREQUISITES CHECK ---
if [ ! -f "$JSON_FILE" ]; then
    echo "Error: $JSON_FILE not found in current directory."
    exit 1
fi

# --- 2. ARGUMENT PARSING ---
# Usage: ./manager.sh [clone|build] [root_dir] [-s|--skip-existing]

COMMAND=$1
ROOT_DIR=$2
SKIP_EXISTING=false

# Check Command
if [[ "$COMMAND" != "clone" && "$COMMAND" != "build" ]]; then
    echo "Error: First argument must be 'clone' or 'build'."
    echo "Usage: $0 [clone|build] [target_directory] [-s]"
    exit 1
fi

# Check Root Dir
if [ -z "$ROOT_DIR" ]; then
    echo "Error: Second argument must be the target root directory."
    exit 1
fi

# Check Optional Flag
if [[ "$3" == "-s" || "$3" == "--skip-existing" ]]; then
    SKIP_EXISTING=true
fi

# --- 3. PREPARE DIRECTORY ---
# Resolve absolute path for the JSON file because we are about to change directories
ABS_JSON_PATH=$(realpath "$JSON_FILE")

echo "Mode: $COMMAND"
echo "Target: $ROOT_DIR"
echo "Skip Existing: $SKIP_EXISTING"
echo "-------------------------------------"

# Create root dir if it doesn't exist
if [ ! -d "$ROOT_DIR" ]; then
    echo "Creating directory $ROOT_DIR..."
    mkdir -p "$ROOT_DIR"
fi

# Move into the root directory
cd "$ROOT_DIR" || exit 1

# --- 4. MAIN LOOP ---
COUNT=$(jq '.dependencies | length' "$ABS_JSON_PATH")

for ((i=0; i<$COUNT; i++)); do
    # Extract info
    LABEL=$(jq -r ".dependencies[$i].name" "$ABS_JSON_PATH")
    REPO=$(jq -r ".dependencies[$i].repository" "$ABS_JSON_PATH")
    SHA1=$(jq -r ".dependencies[$i].sha1" "$ABS_JSON_PATH")
    CMD=$(jq -r ".dependencies[$i].cmd" "$ABS_JSON_PATH")

    # Extract folder name (no .git handling)
    DIR_NAME=$(basename "$REPO")

    echo "Processing: $LABEL ($DIR_NAME)"

    # --- EXISTENCE CHECK ---
    if [ -d "$DIR_NAME" ]; then
        if [ "$SKIP_EXISTING" = true ]; then
            echo "  ! Directory '$DIR_NAME' exists. Skipping."
            continue
        else
            echo "  X Error: Directory '$DIR_NAME' already exists and skip flag is OFF."
            exit 1
        fi
    fi

    # --- CLONE (Only runs if directory did NOT exist) ---
    echo "  - Cloning $REPO..."
    git clone "$REPO" "$DIR_NAME"

    # Enter directory
    cd "$DIR_NAME" || exit 1

    # --- CHECKOUT ---
    echo "  - Checking out $SHA1..."
    git checkout "$SHA1" > /dev/null 2>&1
    if [ $? -ne 0 ]; then
        echo "  X Error: Failed to checkout commit $SHA1"
        exit 1
    fi

    # --- BUILD (Only if command is build) ---
    if [ "$COMMAND" == "build" ]; then
        echo "  - Running build: $CMD"
        eval "$CMD"
        if [ $? -ne 0 ]; then
            echo "  X Error: Build failed for $LABEL"
            exit 1
        fi
        echo "  - Build successful."
    fi

    # Leave directory
    cd ..
    echo "-------------------------------------"
done

echo "All tasks completed."