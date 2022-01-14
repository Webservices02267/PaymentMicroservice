# Token service

The service is a part of the total DTU Pay microservice system where where the **responsibility** of the service is to handle tokens. with regards to generation and verification.

## Build process

The build process is described with a series of scripts.

To do a complete build and deployment and test call the `./build_and_run.sh` script.

Each of the steps in this file is also described in the Jenkins file. where the steps is called in the order of and these scripts will go down in each project respectively compile/build/package/create docker images then deploy and test the service

- `./build.sh`
- `./deploy.sh`
- `./test.sh`

To stop the service run `./stop.sh`.
