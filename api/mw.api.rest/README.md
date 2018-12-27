# Inter Middleware REST API

## Building

Run the following command inside `mw.api.rest` directory:
```
mvn clean package
```

To build the Docker image for the InterMW, run the following command from the `mw.api.rest` directory:
```
docker build -t docker.inter-iot.eu/intermw:<version> .
```
where the `<version>` parameter is desired version of the `intermw` image.

## Installation

### Configuration Directory

Create directory for configuration files, `/etc/inter-iot/intermw` by default (specified in the Jetty context deployment descriptor file) and copy there configuration files from installation package.
For now, the configuration package stands for the properties files located in `src/api/mw.api.rest/src/main/resources`.

### Logging Configuration

Log4j configuration file `log4j2.xml` is located by default in configuration directory `/etc/inter-iot/intermw` and contains the following:
```
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n" />
        </Console>
        <File name="LogFile" fileName="/var/log/inter-iot/intermw/mw.api.rest.log">
            <PatternLayout pattern="%d{yyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </File>
        <Console name="Debug" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %l - %msg%n" />
        </Console>
    </Appenders>
    <Loggers>
        <Root level="error">
            <AppenderRef ref="Console" />
            <AppenderRef ref="LogFile"/>
        </Root>
        <Logger name="eu.interiot" level="debug"/>
    </Loggers>
</Configuration>
```

Create directory for log files, `/var/log/inter-iot/intermw` by default (specified in the `LogFile` appender in `log4j2.xml`) and make sure the user running servlet container (Jetty, Tomcat) has write permissions.

### Deploying Intermw to Jetty

Download Jetty from https://eclipse.org/jetty/download.html

Default Jetty and intermw server port is 8080. If you want to chage it, you do that by setting `jetty.http.port` in `start.ini` in Jetty's log directory (THIS SHALL CHANGE) and you need to recompile intermw with `mw.api.rest/src/main/webapp/WEB-INF/web.xml`'s `swagger.api.basepath` changed and then use the resulting WAR file.

Copy the `mw.api.rest.war` file (from `src/api/mw.api.rest/target`) to `${jetty.base}/webapps` directory.

Create Jetty context deployment descriptor XML file named `mw.api.rest.xml` in the `${jetty.base}/webapps` directory containing the following content:
```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN" "http://www.eclipse.org/jetty/configure_9_0.dtd">

<Configure class="org.eclipse.jetty.webapp.WebAppContext">
    <Set name="contextPath">/mw.api.rest</Set>
    <Set name="war">./webapps/mw.api.rest.war</Set>
    <Call class="java.lang.System" name="setProperty">
        <Arg>intermw.config.location</Arg>
        <Arg>/etc/inter-iot/intermw</Arg>
    </Call>
    <Call class="java.lang.System" name="setProperty">
        <Arg>log4j.configurationFile</Arg>
        <Arg>/etc/inter-iot/intermw/log4j2.xml</Arg>
    </Call>
</Configure>
```

If necessary, set the following parameters according to your deployment environment:
- `intermw.config.location`: path to the intermw configuration directory, `/etc/inter-iot/intermw` by default
- `log4j.configurationFile`: path to the `log4j2.xml` configuration file, `/etc/inter-iot/intermw/log4j2.xml` by default

Run Jetty server by invoking `bin/jetty.sh`

## Testing and Running

To test the project you have to:

- Start an instance of the broker. In this case RabbitMQ [[1](https://git.inter-iot.eu/Inter-IoT/intermw/src/master/src/comm/broker/deploy/rabbitmq)], where you run `start.sh`.
- Start an instance of the IoT middleware. In this case there is a war file [[2](https://git.inter-iot.eu/Inter-IoT/intermw/src/master/src/api/mw.api.rest)]
- Check the \*.properties (probably in `/etc/inter-iot/intermw`) at [[3](https://git.inter-iot.eu/Inter-IoT/intermw/src/master/src/api/mw.api.rest/src/main/resources)] in case you need to change the RabbitMQ or other endpoints to point to your local instances.
- Start a Jetty web server with the compiled war file

* [1] https://git.inter-iot.eu/Inter-IoT/intermw/src/master/src/comm/broker/deploy/rabbitmq
* [2] https://git.inter-iot.eu/Inter-IoT/intermw/src/master/src/api/mw.api.rest
* [3] https://git.inter-iot.eu/Inter-IoT/intermw/src/master/src/api/mw.api.consumer/src/main/resources
* [4] https://www.eclipse.org/jetty

##Debugging
Check logs in `/var/log/inter-iot/intermw`.