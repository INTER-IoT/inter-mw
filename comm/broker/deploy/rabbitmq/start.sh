#/bin/bash

cd docker

docker build -t interiot-rabbit . 2>/dev/null
docker run -d --hostname interiot-rabbit --name interiot-rabbit -p 15672:15672 -p 5672:5672 interiot-rabbit 2>/dev/null

if [ $? != 0 ]; then
    docker start interiot-rabbit
fi