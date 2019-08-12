#!/usr/bin/env bash
attempt=0
while [[ ${attempt} -le 59 ]]; do
    attempt=$(( $attempt + 1 ))
    echo "Waiting for server $1 to be up (attempt: $attempt)..."
    result=$(docker logs $1)
    if grep -q 'waiting for connections on port' <<< ${result} ; then
      echo "Mongodb is up!"
      break
    fi
    sleep 2
done