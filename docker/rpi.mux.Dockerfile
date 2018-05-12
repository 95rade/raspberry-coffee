FROM resin/raspberrypi3-debian:latest
#
# NMEA Multiplexer running on the Raspberry PI.
# Reads a GPS, forward to a file and a small OLED screen
# Web and REST interfaces available.
#
LABEL maintainer="Olivier LeDiouris <olivier@lediouris.net>"

# ENV http_proxy http://www-proxy.us.oracle.com:80
# ENV https_proxy http://www-proxy.us.oracle.com:80

RUN echo "alias ll='ls -lisah'" >> $HOME/.bashrc

RUN apt-get update
RUN apt-get install -y sysvbanner
RUN apt-get install -y curl git build-essential
RUN apt-get install -y oracle-java8-jdk
RUN curl -sL https://deb.nodesource.com/setup_9.x | bash -
RUN apt-get install -y nodejs
RUN apt-get install -y librxtx-java

RUN echo "banner GPS-PI Mux" >> $HOME/.bashrc
RUN echo "git --version" >> $HOME/.bashrc
RUN echo "echo -n 'node:' && node -v" >> $HOME/.bashrc
RUN echo "echo -n 'npm:' && npm -v" >> $HOME/.bashrc
RUN echo "java -version" >> $HOME/.bashrc

RUN mkdir /workdir
WORKDIR /workdir
RUN git clone https://github.com/OlivierLD/raspberry-pi4j-samples.git
WORKDIR /workdir/raspberry-pi4j-samples
RUN ./gradlew tasks
# RUN ./gradlew tasks -Dhttp.proxyHost=www-proxy.us.oracle.com -Dhttp.proxyPort=80 -Dhttps.proxyHost=www-proxy.us.oracle.com -Dhttps.proxyPort=80
#
WORKDIR /workdir/raspberry-pi4j-samples/NMEA.multiplexer
#
RUN ../gradlew shadowJar
# RUN ../gradlew shadowJar -Dhttp.proxyHost=www-proxy.us.oracle.com -Dhttp.proxyPort=80 -Dhttps.proxyHost=www-proxy.us.oracle.com -Dhttps.proxyPort=80

# ENV http_proxy ""
# ENV https_proxy ""

EXPOSE 9999
# We are located in /workdir/raspberry-pi4j-samples/NMEA.multiplexer
CMD ["./mux.sh", "nmea.mux.gps.log.properties"]
