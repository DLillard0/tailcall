#!/bin/bash

echo "Start publish"

cd @aftership/tailcall-core-linux-x64-musl
npm publish --registry=https://nexus.automizely.org/repository/npm-hosted/

cd ../tailcall-core-darwin-arm64
npm publish --registry=https://nexus.automizely.org/repository/npm-hosted/

cd ../tailcall
npm publish --registry=https://nexus.automizely.org/repository/npm-hosted/
