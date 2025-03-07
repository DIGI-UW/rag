#!/bin/bash
set -e

# Wait a bit for Ollama service to fully start
sleep 10

# Run the llama3 model
ollama run llama3