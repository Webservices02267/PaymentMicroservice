#!/bin/bash
set -e

# Build the services
pushd PaymentService
./build.sh
popd