#!/usr/bin/env bash

rm -rf  ./release

mkdir ./release
mkdir ./release/lib

scripts/link.sh

gradle clean assemble

cp ./build/libs/bloqly-*.jar ./release/lib/bloqly.jar

cp ./scripts/run.sh ./release