# ID Forger Service

ID Forger Service is targeted to generate unique ids in a concurrent fashion based on a simple principle: database sequences.

Distinct database sequences can be used for specific purposes so we can generate unique ids for different purposes without
interfering with each other.

## 1. Id generation formats

When requesting a new id, the following formats are currently supported:
- plain: returns the number as is without any formatting **(If no format is provided, this is the default format used to generated the id)**
- base64: returns a base-64 encoded version of the newly created id
- sha256: applies SHA-256 hashing on the id just generated
- luhn: returns a luhn valid number
- timestamped: returns the generated id with timestamp in milliseconds
- prefixed: returns a new generated id by placing a prefix based from the series name

## 2. Database config ##

### 2.1. Requirements ###

* [Docker Desktop](https://www.docker.com/products/docker-desktop/)

### 2.2. Local container configuration ###
To configure the local Postgresql container, run the commands below.

#### 2.2.1. Setup the image
```bash
cd docker/pgsql
docker image build -t codecraftlabs/idgenerator:1.0.0 .
```

#### 2.2.2. Start the container
```bash
docker container run --detach --name idgenerator --publish 5432:5432 codecraftlabs/idgenerator:1.0.0
```

## 3. Building and running the application

### 3.1. Build
```bash
gradle clean build
```

### 3.2. Run

```shell
java -jar ./build/libs/id-forger-service-1.2.0.jar
```

### 3.3. Run with remote debugging enabled 

```shell
java -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=n -jar ./build/libs/id-forger-service-1.2.0.jar
```

## 4. Running the application and the database using Docker compose

### 4.1. Create the application image

First create the application image using the command below:
```shell
docker image build -t codecraftlabs/idgeneratorapp:1.1.1 -f ./docker/app/Dockerfile .
```

### 4.2. Start the containers
```
cd docker
docker-compose up -d
```

### 4.3. Shutdown the containers
```
docker-compose down -v
```
