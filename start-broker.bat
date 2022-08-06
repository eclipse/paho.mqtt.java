@echo off
rem Simplified Windows version of start-broker.sh
rem Moquitto broker not supported, only Python Interopability Broker
echo "Installing and starting Python Interop Broker."
if not exist "paho.mqtt.testing" (
	git clone https://github.com/eclipse/paho.mqtt.testing.git
)
copy "java_client_testing.conf" "paho.mqtt.testing/interoperability/java_client_testing.conf" /y
cd paho.mqtt.testing/interoperability
start python3 startbroker.py -c java_client_testing.conf
cd ../..