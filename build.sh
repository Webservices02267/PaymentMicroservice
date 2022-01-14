#!/bin/bash
set -e

# Build and install the libraries
# abstracting away from using the
# RabbitMq message queue
pushd messaging-utilities-3.2
./build.sh
popd

# Build the services
pushd PaymentService
./build.sh
popd