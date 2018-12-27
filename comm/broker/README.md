This project contains a publish/subscribe API and implementations for different message brokers (ActiveMQ, Kafka, RabbitMQ, MQTT)

TODO:
- Add routing and binding support

DEVELOPMENT:
- Clone this repo
- From Eclipse JDT:
1. File > Import... > Existing Maven Projects
2. Go to this folder and import all the projects

BROKERS:
- There are shell scripts in the middleware-broker/deploy folder to start and stop Kafka, RabbitMQ, ActiveMQ, etc.
- You need docker and docker-compose 

LAUNCH EXAMPLE:
- From the middleware-broker-examples:
1. Check the brokers configuration from src/main/resources/example.broker.properties
2. Run as Java Application the Example.java class
3. To use a different message broker implementation, change the dependency in the pom.mxl of middleware-broker-examples
