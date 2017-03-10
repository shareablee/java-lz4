#!/bin/bash

cd $(dirname $0)

mvn deploy:deploy-file \
    -DgroupId=lz4 \
    -DartifactId=lz4 \
    -Dversion=1.0.0 \
    -Dpackaging=jar \
    -Dfile=../dist/lz4.jar \
    -Durl=file:repo
