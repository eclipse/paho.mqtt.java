# Eclipse Paho Java Client for MQTTv5

_Warning: The Paho MQTTv5 client is under active development and so can expect breaking changes whilst in the develop branch._

This branch of the Paho Java client is the home of the new MQTTv5 client implementation. This is very much a work in progress, so any feedback / and contributions will be appreciated.

The Committee Specification for MQTT Version 5.0 is available to read here: http://docs.oasis-open.org/mqtt/mqtt/v5.0/cs01/mqtt-v5.0-cs01.html.

The v5 client is build on the same foundations as the v3 client is, however it is targeting Java 8 and above, allowing us to take advantages of more modern Java APIs to aid development and use. Any important fixes for the core engine can be ported between the two clients to take advantage of any performance or stability improvements. It is also being heavily refactored using lessons learnt from the v3 client and feedback from the community.

## Plan

#### Project Modules:
* `org.eclipse.paho.mqttv5.client` - A full client similar to the existing mqttv3 client
* `org.eclipse.paho.mqttv5.common` - A common library that could be used by both a client and server, contains a packet implementation that encodes and decodes all MQTTv5 packet types.
* `org.eclipse.paho.mqttv5.testClient` - A number of examples written that show off features of the v5 client.
* `org.eclipse.paho.mqttv5.server` - Not yet implemented. There has been some interest in the community for a Java MQTTv5 server using the vert.x framework. Contributions are very welcome.

## Help, something doesn't work! / This looks terrible! / What about x!

This client is under active development and as such may be incomplete / broken a lot of the time right now. However, the more feedback and help we get on it, the better it will get! If you have any issues, please raise a bug against the client [here](https://github.com/eclipse/paho.mqtt.java/issues), but **please** prefix it with 'MQTTv5' so we know that it's not an issue with the current v3.1.1 client.

If you have any ideas about how the API should be designed going forward, then please chip in on [this](https://github.com/eclipse/paho.mqtt.java/issues/389) issue.

And of course, if you think of an amazing new feature for the v5 client, have a go at implementing it and submit a Pull Request against the develop branch!