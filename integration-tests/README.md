# InterMW Integration Test

## Prerequisites

The integration test has the following prerequisites:

- Parliament
- RabbitMQ
- Inter-Platform Semantic Mediator (IPSM)

### Parliament Triple Store

Parliament can be started with the following command:
```
docker run -d --name parliament -p 8089:8089 daxid/parliament-triplestore
```

Create geospatial index in Parliament by navigating to:
```
http://localhost:8089/parliament/indexes.jsp
```

and click the `Create All` link next to the `Default Graph` entry.

### RabbitMQ
RabbitMQ can be started with the following command:
```
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 -e RABBITMQ_DEFAULT_USER=admin -e RABBITMQ_DEFAULT_PASS=admin rabbitmq:3.7-management-alpine
```

### IPSM
Install and start IPSM. See [ipsm-docker](https://github.com/INTER-IoT/ipsm-docker) repository for installation instructions.

Integation tests require Kafka client keystore and truststore for communicating with IPSM through secure connection. Location of these two files can be set in `intermw.properties` configuration file in `src/test/resources` directory inside `integration-tests-*` module. Default values are:
```
kafka.ssl.keystore.location=/etc/inter-iot/intermw/kafka-client.keystore.jks
kafka.ssl.truststore.location=/etc/inter-iot/intermw/kafka-client.truststore.jks
```

Furthermore, you have to specify the keystore, truststore and private key passwords by setting following properties in `intermw.properties` configuration file:
```
kafka.ssl.truststore.password=
kafka.ssl.keystore.password=
kafka.ssl.key.password=
```

Alternatively, you can set passwords using following ENV variables:
```
export intermw_kafka_ssl_truststore_password=
export intermw_kafka_ssl_keystore_password=
export intermw_kafka_ssl_key_password=
```