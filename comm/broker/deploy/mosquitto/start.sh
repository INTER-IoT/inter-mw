#!/bin/bash

docker run -ti -p 1883:1883 -p 9001:9001 \
-v config:/mqtt/config \
-v log:/mqtt/log \
-v data:/mqtt/data/ \
--name mqtt toke/mosquitto 2>/dev/null

#docker run -ti -p 1883:1883 -p 9001:9001 --name mqtt toke/mosquitto 2>/dev/null

if [ $? != 0 ]; then
    docker start mqtt
fi