#!/usr/bin/env bash

function docker_image_tag_exists() {
    echo "image:&1, tag:$2"
    EXISTS=$(curl -s https://hub.docker.com/v2/repositories/$1/tags/?page_size=10000 | jq -r "[.results? | .[]? | .name == \"$2\"] | any")
    test $EXISTS = true
}

if docker_image_tag_exists $1 $2; then
    echo "true"
else
    echo "false"
fi