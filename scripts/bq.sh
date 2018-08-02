#!/usr/bin/env bash

cd "$(dirname "$0")"

set -x

BLOQLY_HOME="${BLOQLY_HOME:-.}"

export LOG_PATH=${BLOQLY_HOME}/logs

./java/bin/java -Dspring.config.location=${BLOQLY_HOME}/config/bloqly-config.yaml,classpath:application.yaml -jar ./lib/bloqly.jar $@