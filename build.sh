#!/bin/bash
set -eu
lein do clean, run -m jni/build, jar, pom
