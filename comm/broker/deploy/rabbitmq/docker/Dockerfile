#docker build -t interiot-rabbit .
#docker run -d --hostname interiot-rabbit --name interiot-rabbit -p 15672:15672 -p 5672:5672 interiot-rabbit

FROM rabbitmq:3-management

COPY rabbitmq.config /etc/rabbitmq/
COPY rabbit-configuration.json /etc/rabbitmq/

CMD ["rabbitmq-server"]