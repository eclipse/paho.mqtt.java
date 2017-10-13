#!/bin/bash
git clone -b mqttv5 https://github.com/eclipse/paho.mqtt.testing.git
python3 paho.mqtt.testing/interoperability/startbroker5.py &
