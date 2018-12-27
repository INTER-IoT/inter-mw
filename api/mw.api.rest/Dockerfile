FROM tomcat:8.5.23-alpine
RUN rm -r /usr/local/tomcat/webapps/*
COPY target/mw.api.rest.war /usr/local/tomcat/webapps/ROOT.war
RUN mkdir -p /etc/inter-iot/intermw
COPY src/main/config/* /etc/inter-iot/intermw/
ENV CATALINA_OPTS -Dintermw.config.location=/etc/inter-iot/intermw -Dlog4j.configurationFile=/etc/inter-iot/intermw/log4j2.xml -Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true
EXPOSE 8080 8980