#!/bin/bash
set -e

# Wait for MariaDB
#/wait-for-it.sh mariadb:3306

# Start the Java application
exec java -jar /app.jar "$@"