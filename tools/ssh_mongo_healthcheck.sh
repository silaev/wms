#!/usr/bin/env bash

ssh_check=$(service --status-all 2>&1 | grep -w 'ssh' | awk '{print $2}')
mongo_check=$(mongo --port 50001 --quiet --eval 'db.runCommand("ping").ok')
echo "ssh_check: $ssh_check, mongo_check: $mongo_check"
if [ "$ssh_check" == '+' ] && [ "$mongo_check" == "1" ]; then
  exit 0
else
  exit 1
fi
