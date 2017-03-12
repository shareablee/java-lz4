#!/bin/bash
set -eu
lein do clean, jar, test
