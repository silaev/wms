#!/usr/bin/expect -f
#keeps pass in history, do not use in prod
set cmd [lindex $argv 0]
set user [lindex $argv 1]
set ip [lindex $argv 2]
set password [lindex $argv 3]
set timeout 3
spawn $cmd $user@$ip
expect "yes/no>\r"
send "yes\r"
expect "password>\r"
send "$password\r"
sleep 3
interact