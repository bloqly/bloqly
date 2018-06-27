#!/usr/bin/env bash

set -x

dropdb bloqly_main
dropdb bloqly_rhea
dropdb bloqly_loge
dropdb bloqly_ymir

dropdb bloqly_test

createdb bloqly_main
createdb bloqly_rhea
createdb bloqly_loge
createdb bloqly_ymir

createdb bloqly_test

psql -d bloqly_main -c "alter user "$USER" with password 'password';"
psql -d bloqly_rhea -c "alter user "$USER" with password 'password';"
psql -d bloqly_loge -c "alter user "$USER" with password 'password';"
psql -d bloqly_ymir -c "alter user "$USER" with password 'password';"