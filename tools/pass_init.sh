#!/bin/sh

echo "works in bionic, but doesn't in xenial/trusty. so employ it once travis supports bionic"

cd /usr/bin/ || exit
curl -fsSL "https://github.com/docker/docker-credential-helpers/releases/download/v0.6.0/docker-credential-pass-v0.6.0-amd64.tar.gz" | tar xv
chmod +x docker-credential-pass
cat >g-key <<EOF
     %echo Generating a basic OpenPGP key
     Key-Type: DSA
     Key-Length: 1024
     Subkey-Type: ELG-E
     Subkey-Length: 1024
     Name-Real: s256
     Name-Comment: with passphrase
     Name-Email: s256@gmail.com
     Expire-Date: 0
     Passphrase: $1
     # Do a commit here, so that we can later print "done" :-)
     %commit
     %echo done
EOF

gpg --batch --generate-key g-key; pass init $(gpg -k | awk 'NR==4')