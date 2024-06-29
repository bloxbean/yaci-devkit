#!/bin/bash

set -e

socat TCP-LISTEN:${SOCAT_PORT},reuseaddr,fork UNIX-CONNECT:node/node.sock
