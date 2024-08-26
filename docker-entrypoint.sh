#!/bin/bash

echo "Starting entrypoint script"
echo "AWS_LAMBDA_RUNTIME_API: ${AWS_LAMBDA_RUNTIME_API}"

if [ -z "${AWS_LAMBDA_RUNTIME_API}" ]; then
    echo "Running in local mode"
    echo "Command: /var/task/bootstrap $@"
    exec /var/task/bootstrap "$@"
else
    echo "Running in Lambda mode"
    echo "Command: /var/runtime/bootstrap"
    exec /var/runtime/bootstrap
fi
