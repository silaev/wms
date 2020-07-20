### Additional info
#### Install a 3 replica set MongoDB via Docker-Compose
Install a 3 replica set MongoDB via Docker-Compose, run in a terminal:
- docker-compose -f docker-compose.yml up -d
- docker-compose exec mongo1 /bin/sh -c "mongo --port 50001 < /scripts/init.js"
See .travis.yml for more details

#### Installing a 3 replica set MongoDB via just Docker (without compose)
- Set mongodb url in Spring boot app
`spring:
  data:
    mongodb:
      uri: mongodb://mongo1:50001,mongo2:50002,mongo3:50003/test?replicaSet=docker-rs`

- Add `127.0.0.1 mongo1 mongo2 mongo3` to host file

- Run in terminal:
`docker run --name mongo1 -d --net mongo-cluster -p 50001:50001 mongo:4.0.10 mongod --replSet docker-rs --port 50001
docker run --name mongo2 -d --net mongo-cluster -p 50002:50002 mongo:4.0.10 mongod --replSet docker-rs --port 50002
docker run --name mongo3 -d --net mongo-cluster -p 50003:50003 mongo:4.0.10 mongod --replSet docker-rs --port 50003`

- On Unix, you will get an error if your script has Dos/Windows end of lines (CRLF) instead of Unix end of lines (LF).
So run in terminal if scripts files were modifies:
`dos2unix scripts/**`

- Run in terminal:
`docker cp scripts/ mongo1:/scripts/`
`docker exec -it mongo1  /bin/sh -c "mongo --port 50001 < /scripts/init.js"`

Alternately, use Docker compose (see steps above)

#### Installing a single replica set MongoDB via Docker
- chmod +x wait-for-mongo.sh
- docker network create mongo-cluster
- docker run --name mongo -p 27017:27017 -d --net mongo-cluster mongo:4.0.10 --replSet rs0
- ./wait-for-mongo.sh
- docker exec -it mongo mongo --eval "printjson(rs.initiate())"

#### Corner cases on different hosts
in case of an error "bind to localhost" on Windows:
https://github.com/docker/for-win/issues/1804