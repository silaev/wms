#!/usr/bin/env bash
attempt=0
while true; do
    attempt=$(( $attempt + 1 ))
    echo "Waiting for ssh server $1 to be up (attempt: $attempt)..."
    if [[ "$(nmap -p 22 $1 | grep "open  ssh")" == "22/tcp open  ssh" ]] ; then
    echo "ssh server is up";
    break;
    else
    sleep 2;
    fi
done