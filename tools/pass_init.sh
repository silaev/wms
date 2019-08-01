#!/bin/sh
cd "$(dirname "$0")" || exit
curl -fsSL "https://github.com/docker/docker-credential-helpers/releases/download/v0.6.0/docker-credential-pass-v0.6.0-amd64.tar.gz" | tar xv;
while [ ! -f docker-credential-pass ]; do sleep 1; done
chmod +x docker-credential-pass;
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
     # Do a commit here, so that we can later print "done" :-)
     %commit
     %echo done
EOF

gpg --batch --yes --passphrase="$PASSPH" --pinentry-mode loopback --generate-key g-key;
pass init $(gpg -k | awk 'NR==4');

mkdir -p "$HOME"/.docker;

cat > "$HOME"/.docker/config.json <<- EOM
{
        "credsStore": "pass",
        "auths": {},
        "HttpHeaders": {
                "User-Agent": "Docker-Client/19.03.1 (linux)"
        }
}
EOM
