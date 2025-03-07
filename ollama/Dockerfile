FROM ollama/ollama:latest

# Copy the initialization script
COPY ollama-init.sh /ollama-init.sh
RUN chmod +x /ollama-init.sh

# Set up a custom entrypoint script
COPY entrypoint-ollama.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]