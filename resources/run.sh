#!/usr/bin/env bash

cd "$(dirname "$0")"

./java/bin/java -jar ./lib/bloqly-machine.jar -database db $@