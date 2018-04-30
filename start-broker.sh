#!/bin/bash
#
# start-broker.sh
#
# This script can be used locally or on a travis-ci instance to
# download, install and run an MQTT broker configured for testing
# the Eclipse Paho Clients.
#
# By default, it will run the Python Interopability Broker,
# however, if you set $BROKER to "MOSQUITTO", and set "TRAVIS_OS_NAME"
# to either "linux" or "osx" depending on your environment, then it will
# install and run Mosquitto. (For OSX users, you will need homebrew installed)


if [ "$BROKER" == "MOSQUITTO" ]; then
	if [ "$TRAVIS_OS_NAME" == "linux" ]; then
		echo "Installing and starting Mosquitto Broker on Linux."
		pwd
		sudo service mosquitto stop
		mosquitto -h
		mosquitto -c test/tls-testing/mosquitto.conf &
	fi

	if [ "$TRAVIS_OS_NAME" == "osx" ]; then
		echo "Installing and starting Mosquitto Broker on OSX."
		pwd
		brew update
		brew install openssl mosquitto
		brew services stop mosquitto
		/usr/local/sbin/mosquitto -h
		/usr/local/sbin/mosquitto -c test/tls-testing/mosquitto.conf &
	fi
else 
	echo "Installing and starting Python Interop Broker."
	sudo service mosquitto stop
	git clone https://github.com/eclipse/paho.mqtt.testing.git
	cd paho.mqtt.testing/interoperability
	python3 startbroker.py -c client_testing.conf &
fi


