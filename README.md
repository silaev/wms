# HTTP API for small WMS (warehouse management system) based on Spring boot 2 Reactive WebFlux and Embedded MongoDB 
[![Build Status](https://travis-ci.org/silaev/wms.svg?branch=master)](https://travis-ci.org/silaev/wms)
[![codecov](https://codecov.io/gh/silaev/wms/branch/master/graph/badge.svg)](https://codecov.io/gh/silaev/wms)

#### Prerequisite
- Java 11

Install Mongo replica set in Docker: 
- docker network create mongo-cluster
- docker run --name mongo -p 27017:27017 -d --net mongo-cluster mongo:4.0.10 --replSet rs0
- docker exec -it mongo mongo --eval "printjson(rs.initiate())"

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