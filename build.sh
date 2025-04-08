#!/bin/bash

echo "Start build target: darwin-arm64"

cargo build --release

echo "Build darwin-arm64 completed"

cp target/release/tailcall @aftership/tailcall-core-darwin-arm64/bin

echo "Start build target: linux-x64-musl"

cargo build --release --target x86_64-unknown-linux-musl

echo "Build linux-x64-musl completed"

cp target/x86_64-unknown-linux-musl/release/tailcall @aftership/tailcall-core-linux-x64-musl/bin
