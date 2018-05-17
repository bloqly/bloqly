#!/usr/bin/env bash

rm -rf  ./release

mkdir ./release
mkdir ./release/lib

./link.sh

gradle clean assemble

cp ./build/libs/bloqly-machine-*.jar ./release/lib/bloqly-machine.jar

cp ./resources/run.sh ./release