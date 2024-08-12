# Use the official MongoDB image as the base
FROM mongo:latest

# Copy the custom entrypoint script into the container
COPY docker-entrypoint.sh /docker-entrypoint.sh

# Make the script executable
RUN chmod +x /docker-entrypoint.sh

# Override the default entrypoint with the custom script
ENTRYPOINT ["/docker-entrypoint.sh"]

# Optionally specify CMD if you want to pass additional arguments
CMD ["mongod", "--bind_ip_all"]
