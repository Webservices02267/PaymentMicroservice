#!/bin/bash
set -e

# Build and install the libraries
# abstracting away from using the
# RabbitMq message queue
pushd Messaging-utilities
./build.sh
popd

# Build the services
pushd PaymentService
./build.sh
popd