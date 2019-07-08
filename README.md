# HTTP API for small WMS (warehouse management system) based on Spring boot 2 Reactive WebFlux and MongoDB 4 demonstrating the use of document transactions.  
[![Build Status](https://travis-ci.org/silaev/wms.svg?branch=master)](https://travis-ci.org/silaev/wms)
[![codecov](https://codecov.io/gh/silaev/wms/branch/master/graph/badge.svg)](https://codecov.io/gh/silaev/wms)

#### Prerequisite
- Java 11
- Docker v18.09.2 (was tested on this version) 
- docker-compose v1.23.2 (compose file version: "3.7", was tested on this version) 
(if you'd prefer just Docker, see `Installing a 3 replica set MongoDB via just Docker`)

Install a 3 replica set MongoDB via Docker-Compose, run in a terminal: 
- docker-compose -f docker-compose.yml up -d
- docker-compose exec mongo1 /bin/sh -c "mongo --port 50001 < /scripts/init.js"
See .travis.yml for more details
 
#### General info
The application lets:
- create new products with article verification 
    (duplicate key error collection: wms.product index: article occurs while 
    trying to insert products with identical id);
- find products dto(user's representation) by either name or brand (full equality, may be soften in ProductDao
    via clear DSL);
- find all products (entity representation);
- upload xlsx file (other formats are not currently supported) in order to update current product quantities.
    Matching is done by article and size. The products that are supposed to 
    be patched but not present in MongoDB, are properly logged. 
    After uploading, files are kept in a storage.bulk-upload-path/userName folder
    until he next call is performed. 
- download product in xlsx format is not currently supported.     
          
#### Requirements to consider before using the app 
1. The project ought to be built by means of Gradle. For that reason, run `gradlew clean build`.
Subsequently, to start the application make use of `gradlew bootRun`
2. All the examples of possible requests might be found in  
`integration` package including tests in accordance with basic commands.
These classes use a bunch of predefined files located in `test\resources`.
3. The app provides BasicAuth with predefined 2 users admin and user,
see more in SecurityConfig.java.   

#### Future improvements
1. Recon the use of Spring Security Oath2.
2. Draw attention to the limitation of user space on the server. Frankly speaking,
server space is a limited resource.
3. Modern web browsers prohibit HTTP in favour of HTTPS. That is why, make use of
Spring config files (server.ssl.key*) and a proper type of certificate
to satisfy modern constrains. Whereas it makes sense for front-end applications, it
may not be relevant to REST API as used here. Nevertheless, you should NEVER transmit 
sensitive data such as tokens over a non-HTTPS connection.
Remember, Man in the middle attacks are real.
4. Employ MongoDB transaction support for patching products quantity to 
avoid inconsistent state.
5. Optimistic Locking requires to set the WriteConcern to ACKNOWLEDGED.
Otherwise OptimisticLockingFailureException can be silently swallowed.

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