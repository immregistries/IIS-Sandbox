#!/bin/bash
# Check if .env file exists
if [ ! -f .env ]; then
  # Check if .env.example exists
  if [ -f .env.example ]; then
    # Copy .env.example to .env
    cp .env.example .env
    echo ".env file created from .env.example, consider changing the default configuration"
  else
    echo "Error: .env.example file not found."
    exit 1  # Exit with an error code
  fi
else
  echo ".env file already exists."
fi

docker compose up;

exit 0 # Exit with success code
