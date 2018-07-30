# Eclipse Paho mqtt-client CLI App.

## Examples

### General
 1. To connect with a will message, you will need to include the -wp (--will-payload) and -wt (--will-topic) arguments. If the connection to the broker is then lost, then after the keepalive has expired, the broker will forward on the will message to any subscribed clients. The arguments -wt (--will-retain) and -wq (--will-qos) can also be used. Example: `./mqtt-client -sub -h tcp://iot.eclipse.org:1883 -t world -q 0 -wt status -wp "I have disconnected"`
 

### Publishing

1. Publish a message to a topic with a QoS of 0: `./mqtt-client -pub -h tcp://iot.eclipse.org:1883 -t world -m "Hello World" -q 0`
2. Publish a file to a topic: `./mqtt-client -pub -h tcp://iot.eclipse.org:1883 -t world -f alice.txt`
3. Publish input from stdin line-by-line to a toipc: `tail -f /var/log/system.log | ./mqtt-client -pub -h tcp://iot.eclipse.org:1883 -t world --stdin-line`
4. Publish all input from stdin to a topic in one go: `cat alice.txt | ./mqtt-client -pub -h tcp://iot.eclipse.org:1883 -t world --stdin`
5. Publish a message using MQTTv5 to a compatible broker: `./mqtt-client -pub -h tcp://iot.eclipse.org:1883 -t world -m "This is an MQTTv5 message" -v 5 `


### Subscibing

1. Subscribe to a topic with a QoS of 0: `./mqtt-client -sub -h tcp://iot.eclipse.org:1883 -t world -q 0`
2. Subscribe to a topic with a QoS of 0 and print incoming topic name (good with wildcards): `./mqtt-client -sub -h tcp://iot.eclipse.org:1883 -t world/# -q 0 -V`


