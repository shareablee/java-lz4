#!/bin/bash
set -eu

cd $(dirname $0)

ant clean
rm -rf target build dist
ant

cd test
rm -rf repo/ target/
mvn install:install-file \
    -DgroupId=lz4 \
    -DartifactId=lz4 \
    -Dversion=1.0.0 \
    -Dpackaging=jar \
    -Dfile=../dist/lz4.jar
lein test
