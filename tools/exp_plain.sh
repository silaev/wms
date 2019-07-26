#!/usr/bin/expect -f
#keeps pass in history, do not use in prod
set cmd1 [lindex $argv 0]
set user [lindex $argv 1]
set ip [lindex $argv 2]
set cmd2 [lindex $argv 3]
set password [lindex $argv 4]
spawn $cmd1 $user@$ip $cmd2
expect "password>\r"
send "$password\r"
sleep 3
interact