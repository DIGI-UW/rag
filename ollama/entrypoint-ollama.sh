#!/bin/sh
set -e

# Start Ollama server in the background
ollama serve &

# Give the server a moment to start
sleep 10

# Pull the model (non-interactive)
ollama pull llama3

# Keep the container running
echo "Ollama server is running with llama3 model..."
tail -f /dev/null