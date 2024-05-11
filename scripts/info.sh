#!/bin/bash

cd "$(dirname "$0")"

ENV_FILE="../config/env"

source $ENV_FILE

echo "--------------------------------------------"
echo "Urls"
echo "--------------------------------------------"

echo "Yaci Viewer                   : http://localhost:${HOST_VIEWER_PORT}"
echo "Yaci Store Swagger UI         : http://localhost:${HOST_STORE_API_PORT}/swagger-ui.html"

echo ""
echo "Yaci Store Api URL            : http://localhost:${HOST_STORE_API_PORT}/api/v1/"
echo ""
echo "Pool Id                       : pool1wvqhvyrgwch4jq9aa84hc8q4kzvyq2z3xr6mpafkqmx9wce39zy"
echo ""

echo "--------------------------------------------"
echo "Other Urls"
echo "--------------------------------------------"

echo "Ogmios Url (Optional)         : ws://localhost:${HOST_OGMIOS_PORT}"
echo "Kupo Url   (Optional)         : http://localhost:${HOST_KUPO_PORT}"

echo ""
echo "--------------------------------------------"
echo "Node Ports"
echo "--------------------------------------------"

echo "n2n port                      : localhost:${HOST_N2N_PORT}"
echo "n2c port for remote client    : localhost:${HOST_N2C_SOCAT_PORT}"
echo ""


