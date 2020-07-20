# HTTP API for small WMS (warehouse management system) based on Spring boot 2 Reactive WebFlux and MongoDB 4 demonstrating the use of document transactions.  
![Java CI with Gradle](https://github.com/silaev/wms/workflows/Java%20CI%20with%20Gradle/badge.svg?branch=master)
[![codecov](https://codecov.io/gh/silaev/wms/branch/master/graph/badge.svg)](https://codecov.io/gh/silaev/wms)

#### Prerequisite
- Java 8
- Docker (was tested on version 18.09.2) 

#### Article on DEV Community
[The Testcontainers’ MongoDB Module and Spring Data MongoDB in Action](https://dev.to/silaev/the-testcontainers-mongodb-module-and-spring-data-mongodb-in-action-53ng)
 
#### General info
The application allows:
- create new products with article verification 
    (duplicate key error collection: wms.product index: article occurs while 
    trying to insert products with an identical id);
- find products dto(user's representation) by either name or brand (full equality, may be soften in ProductDao
    via clear DSL);
- find all products (entity representation);
- upload xlsx file (other formats are not currently supported) in order to update current product quantities.
    Matching is done by article and size. The app informs about inconsistencies between the products that are supposed to 
    be patched and MongoDB. 
    After uploading, files are kept in a storage.bulk-upload-path/userName folder
    and then removed when the next call is performed. 
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

#### License
[The MIT License (MIT)](https://github.com/silaev/wms/blob/master/LICENSE/)

#### Copyright
Copyright (c) 2020 Konstantin Silaev <silaev256@gmail.com>