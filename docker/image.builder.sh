#!/bin/bash
#
# Build and run a docker image
#
OK=false
DOCKER_FILE=
IMAGE_NAME=
RUN_CMD=
MESSAGE="Bye!"
#
while [ "$OK" = "false" ]
do
  # Menu
  echo -e "+-------------- D O C K E R  I M A G E  B U I L D E R ----------------+"
  echo -e "| 1. Nav Server, Debian                                               |"
  echo -e "| 2. Web Components, Debian                                           |"
  echo -e "| 3. To run on a Raspberry PI, Java, Raspberry Coffee, Web Components |"
  echo -e "| 4. Node PI, to run on a Raspberry PI                                |"
  echo -e "| 5. Node PI, to run on Debian                                        |"
  echo -e "| 6. GPS-mux, to run on a Raspberry PI                                |"
  echo -e "| 7. Golang, basics                                                   |"
  echo -e "| 8. Raspberry PI, MATE, with java, node, web comps, VNC              |"
  echo -e "+---------------------------------------------------------------------+"
  echo -e "| Q. Oops, nothing, thanks, let me out.                               |"
  echo -e "+---------------------------------------------------------------------+"
  echo -en "== You choose => "
  read a
  #
  case "$a" in
    "Q" | "q")
      OK=true
      ;;
    "1")
      OK=true
      DOCKER_FILE=navserver.Dockerfile
      IMAGE_NAME=oliv-nav
			RUN_CMD="docker run -p 8081:8080 -d $IMAGE_NAME:latest"
			#                      |    |
			#                      |    tcp port used in the image
			#                      tcp port as seen from outside
			#
      MESSAGE="Reach http://localhost:8081/oliv-components/index.html from your browser. You can also log in using: docker run -it $IMAGE_NAME:latest /bin/bash"
      ;;
    "2")
      OK=true
      DOCKER_FILE=webcomponents.Dockerfile
      IMAGE_NAME=oliv-webcomp
			RUN_CMD="docker run -p 9999:9999 -d $IMAGE_NAME:latest"
			#                      |    |
			#                      |    tcp port used in the image
			#                      tcp port as seen from outside
			#
      MESSAGE="Reach http://localhost:9999/index.html from your browser. You can also log in using: docker run -it $IMAGE_NAME:latest /bin/bash"
      ;;
    "3")
      OK=true
      DOCKER_FILE=rpi.Dockerfile
      IMAGE_NAME=oliv-rpi
			RUN_CMD="docker run -p 8081:8080 -d $IMAGE_NAME:latest"
			#                      |    |
			#                      |    tcp port used in the image
			#                      tcp port as seen from outside
			#
      MESSAGE="Reach http://localhost:8081/oliv-components/index.html from your browser. You can also log in using: docker run -it $IMAGE_NAME:latest /bin/bash"
      ;;
    "4")
      OK=true
      DOCKER_FILE=node-pi.Dockerfile
      IMAGE_NAME=oliv-nodepi
			# RUN_CMD="docker run -p 9876:9876 -t -i --device=/dev/ttyUSB0 $IMAGE_NAME:latest /bin/bash"
			RUN_CMD="docker run -p 9876:9876 -t -i --privileged -v /dev/ttyUSB0:/dev/ttyUSB0 -d $IMAGE_NAME:latest"
			#                      |    |            |             |             |
			#                      |    |            |             |             Device IN the docker image
			#                      |    |            |             Device name in the host (RPi) machine
			#                      |    |            sudo access to the Serial Port
			#                      |    tcp port IN the docker image
			#                      tcp port as seen from outside
			#
      # MESSAGE="See doc at https://github.com/OlivierLD/node.pi/blob/master/README.md"
			IP_ADDR=`ifconfig | grep 'inet ' | grep -v '127.0.0.1' | awk '{ print $2 }'`
			if [ "$IP_ADDR" = "" ]
			then
			  IP_ADDR="localhost"
			fi
      MESSAGE="Reach http://$IP_ADDR:9876/data/demos/gps.demo.html in your browser. You can also log in using: docker run -it $IMAGE_NAME:latest /bin/bash"
      ;;
    "5")
      OK=true
      DOCKER_FILE=Dockerfile.node-debian
      IMAGE_NAME=oliv-nodedebian
			# RUN_CMD="docker run -p 9876:9876 --privileged -v /dev/tty.usbserial:/dev/ttyUSB0 -d $IMAGE_NAME:latest"
			RUN_CMD="docker run -p 9876:9876 -d $IMAGE_NAME:latest"
			#                      |    |
			#                      |    tcp port used in the image
			#                      tcp port as seen from outside
			#
			IP_ADDR=`ifconfig | grep 'inet ' | grep -v '127.0.0.1' | awk '{ print $2 }'`
			if [ "$IP_ADDR" = "" ]
			then
			  IP_ADDR="localhost"
			fi
      MESSAGE="Reach http://$IP_ADDR:9876/data/demos/gps.demo.html in your browser. You can also log in using: docker run -it $IMAGE_NAME:latest /bin/bash"
      ;;
    "6")
      OK=true
      DOCKER_FILE=rpi.mux.Dockerfile
      IMAGE_NAME=oliv-node-mux
			# RUN_CMD="docker run -p 9876:9876 -t -i --device=/dev/ttyUSB0 $IMAGE_NAME:latest /bin/bash"
			RUN_CMD="docker run -p 9999:9999 -t -i --privileged -v /dev/ttyUSB0:/dev/ttyUSB0 -d $IMAGE_NAME:latest"
			#                      |    |            |             |             |
			#                      |    |            |             |             Device IN the docker image
			#                      |    |            |             Device name in the host (RPi) machine
			#                      |    |            sudo access to the Serial Port
			#                      |    tcp port IN the docker image
			#                      tcp port as seen from outside
			#
      # MESSAGE="See doc at https://github.com/OlivierLD/node.pi/blob/master/README.md"
			IP_ADDR=`ifconfig | grep 'inet ' | grep -v '127.0.0.1' | awk '{ print $2 }'`
			if [ "$IP_ADDR" = "" ]
			then
			  IP_ADDR="localhost"
			fi
      MESSAGE="Reach http://$IP_ADDR:9999/web/index.html in your browser. REST operations available: http://localhost:9999/mux/oplist. You can also log in using: docker run -it $IMAGE_NAME:latest /bin/bash"
      ;;
    "7")
      OK=true
      DOCKER_FILE=golang.Dockerfile
      IMAGE_NAME=oliv-go
      RUN_CMD="docker run -d $IMAGE_NAME:latest"
      #
      MESSAGE="Log in using: docker run -it $IMAGE_NAME:latest /bin/bash"
      ;;
    "8")
      OK=true
      DOCKER_FILE=rpidesktop.Dockerfile
      IMAGE_NAME=oliv-pi-vnc
      RUN_CMD="docker run -d $IMAGE_NAME:latest"
      #
      MESSAGE="Log in using: docker run -it --rm -p 5901:5901 -p 8080:8080 -e USER=root $IMAGE_NAME:latest /bin/bash, then run 'vncserver :1 -geometry 1280x800 (or 1440x900, etc) -depth 24', then use a vncviewer on localhost:1, password is 'mate'. then 'node server.js', and reach http://localhost:8080/oliv-components/index.html ..."
      ;;
    *)
      echo -e "What? Unknown command [$a]"
      ;;
  esac
  #
done
#
#
if [ "$DOCKER_FILE" != "" ]
then
  #
  # Proxies,if needed
  # export HTTP_PROXY=http://www-proxy.us.oracle.com:80
  # export HTTPS_PROXY=http://www-proxy.us.oracle.com:80
  #
  echo -e "Generating $IMAGE_NAME from $DOCKER_FILE"
  #
  docker build -f $DOCKER_FILE -t $IMAGE_NAME .
  #
  # Now run
  $RUN_CMD
fi
echo $MESSAGE
