#!/usr/bin/env bash

cd "$(dirname "$0")"

BLOQLY_CONFIG="${BLOQLY_CONFIG:-./config/config.yaml}"

./java/bin/java -Djava.net.preferIPv4Stack=true -Dspring.config.location=${BLOQLY_CONFIG},classpath:application.yaml -jar ./lib/bloqly.jar $@