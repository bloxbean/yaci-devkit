#!/bin/bash
payload=$( echo $1 | jq -r '.[]  | [.assetName, .quantity] | @csv')

final_payload=$( jq -n \
                  --arg payload "$payload" \
                  '{username: "yaci-cli", content: $payload}' )

echo $final_payload

curl -X POST -H "Content-Type: application/json" -d "$final_payload" "https://discord.com/api/webhooks/..."

status=$?

echo $1 > mint.txt
echo $final_payload > mint.txt
echo $status >> mint.txt
