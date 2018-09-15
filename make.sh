#!/usr/bin/env bash

set -x

VERSION="0.1.5"

rm -rf ./release/mac
rm -rf ./release/linux

mkdir ./release/mac
mkdir ./release/mac/lib
mkdir ./release/mac/java

mkdir ./release/linux
mkdir ./release/linux/lib
mkdir ./release/linux/java

gradle clean assemble

cp ./build/libs/bloqly-*.jar ./release/mac/lib/bloqly.jar
cp ./build/libs/bloqly-*.jar ./release/linux/lib/bloqly.jar

cp ./scripts/bq.sh ./release/mac/
cp ./scripts/bq.sh ./release/linux/

# copy java to release folders
cp -r ./release/java-mac/* ./release/mac/java
cp -r ./release/java-linux/* ./release/linux/java

# copy demo dir
cp -r ./demo ./release/mac
cp -r ./demo ./release/linux

rm  -rf ./release/bin/*

tar -czf ./release/bin/bloqly-mac-${VERSION}.tar.gz -C ./release/mac/ .
tar -czf ./release/bin/bloqly-linux-${VERSION}.tar.gz -C ./release/linux/ .