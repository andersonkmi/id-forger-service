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

## 5. API Usage Examples

All endpoints are served under `/idgenerator/v1` and return `application/json`. The examples below assume the service is running on `localhost:8080`.

### 5.1. Generate the next ID

Requests the next unique ID for a given series. If no `format` query parameter is supplied, the plain numeric value is returned.

```bash
curl -s http://localhost:8080/idgenerator/v1/ids/default
```

### 5.2. Generate the next ID with a specific format

Use the `format` query parameter to control how the generated ID is encoded or decorated. See [Section 1](#1-id-generation-formats) for available formats.

```bash
# no format provided
curl -s http://localhost:8080/idgenerator/v1/ids/default

# base64-encoded ID
curl -s "http://localhost:8080/idgenerator/v1/ids/default?format=base64"

# prefixed with the series name
curl -s "http://localhost:8080/idgenerator/v1/ids/product?format=prefixed"

# Luhn-valid number
curl -s "http://localhost:8080/idgenerator/v1/ids/default?format=luhn"

# SHA-256 hash
curl -s "http://localhost:8080/idgenerator/v1/ids/default?format=sha256"

# ID with a millisecond timestamp
curl -s "http://localhost:8080/idgenerator/v1/ids/default?format=timestamped"
```

### 5.3. Update the last value of a sequence

Resets the sequence for a series so the next generated ID starts after the provided value. Useful for migrations or manual corrections.

```bash
curl -s -X PUT http://localhost:8080/idgenerator/v1/ids/default \
  -H "Content-Type: application/json" \
  -d '{"newLastValue": 1000}'
```

### 5.4. Get the current value without advancing the sequence

Returns the most recently generated ID for a series. The sequence is **not** incremented.

```bash
curl -s http://localhost:8080/idgenerator/v1/ids/default/currentValue
```

### 5.5. Get sequence details

Retrieves full metadata about the underlying database sequence for a series (e.g. current value, increment, min/max).

```bash
curl -s http://localhost:8080/idgenerator/v1/ids/default/details
```

> **Tip:** Replace `default` with any valid series name (e.g. `product`). Add the `-v` flag to any command to inspect HTTP headers for debugging.
