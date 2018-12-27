#!/bin/bash

docker run --name='activemq' -d \
-e 'ACTIVEMQ_NAME=amqp-srv1' \
-e 'ACTIVEMQ_REMOVE_DEFAULT_ACCOUNT=true' \
-e 'ACTIVEMQ_ADMIN_LOGIN=admin' -e 'ACTIVEMQ_ADMIN_PASSWORD=admin' \
-e 'ACTIVEMQ_STATIC_TOPICS=default-topic;topic2' \
-e 'ACTIVEMQ_STATIC_QUEUES=queue1;queue2;queue3' \
-e 'ACTIVEMQ_MIN_MEMORY=512' -e  'ACTIVEMQ_MAX_MEMORY=1024' \
-e 'ACTIVEMQ_ENABLED_SCHEDULER=true' \
-v /data/activemq:/data/activemq \
-v /var/log/activemq:/var/log/activemq \
-p 1099:1099 \
-p 8161:8161 \
-p 61616:61616 \
-p 61613:61613 \
webcenter/activemq:latest 2>/dev/null

if [ $? != 0 ]; then
    docker start activemq
fi
