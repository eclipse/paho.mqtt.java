# Eclipse Paho Java Client for MQTTv5

_Warning: The Paho MQTTv5 client is in early stages of development and so is not ready to be used. Additionally, the MQTTv5 specification is a working draft and has not been formally published so aspects of this implementation may change over time._

This branch of the Paho Java client is the home of the new MQTTv5 client implementation. This is very much a work in progress, so any feedback / and contributions will be appreciated.

The working draft for MQTT Version 5.0 is available to read here: https://www.oasis-open.org/committees/document.php?document_id=59173&wg_abbrev=mqtt.

Rather than simply trying to bolt on the new v5 features to the existing mqttv3 client, it will be better to build a new client from the ground up that takes advantage of a modern version of Java, the lessons learnt from the existing client and feedback from the community.

## Plan

### Modular design
Similar to the C clients, it would be nice to build the client in a semi-modular way. This would allow people to pick and choose the parts of the library that they wanted for their particular implementation.

Of course, those who just want the client without having to worry about lower level implementation will still be able to pick up a jar like the current mqttv3 client that gives them access to similar APIs.

#### Current Planned Modules:
* `org.eclipse.paho.mqttv5.client` - A full client similar to the existing mqttv3 client
* `org.eclipse.paho.mqttv5.packet` - A lightweight packet implementation that encodes and decodes all MQTTv5 packet types.
* `org.eclipse.paho.mqttv5.util` - Utilitiy library, containing useful classes used by multiple modules.
* `org.eclipse.paho.mqttv5.server` - Maybe?


### JDK version

As this is being developed from scratch, it will be written using Java version 8. This will allow us to take advantage of many of the new and improved APIs that are available.



## Help, something doesn't work! / This looks terrible! / What about x!

This client is under active development and as such may be incomplete / broken a lot of the time right now. However, the more feedback and help we get on it, the better it will get! If you have any issues, please raise a bug against the client [here](https://github.com/eclipse/paho.mqtt.java/issues), but **please** prefix it with 'MQTTv5' so we know that it's not an issue with the current v3.1.1 client.

If you have any ideas about how the API should be designed going forward, then please chip in on [this](https://github.com/eclipse/paho.mqtt.java/issues/389) issue.

And of course, if you think of an amazing new feature for the v5 client, have a go at implementing it and submit a pr against the mqttv5-new branch!








## Links

- Project Website: [https://www.eclipse.org/paho](https://www.eclipse.org/paho)
- Eclipse Project Information: [https://projects.eclipse.org/projects/iot.paho](https://projects.eclipse.org/projects/iot.paho)
- Paho Java Client Page: [https://eclipse.org/paho/clients/java/](https://eclipse.org/paho/clients/java)
- GitHub: [https://github.com/eclipse/paho.mqtt.java](https://github.com/eclipse/paho.mqtt.java)
- Twitter: [@eclipsepaho](https://twitter.com/eclipsepaho)
- Issues: [https://github.com/eclipse/paho.mqtt.java/issues](https://github.com/eclipse/paho.mqtt.java/issues)
- Mailing-list: [https://dev.eclipse.org/mailman/listinfo/paho-dev](https://dev.eclipse.org/mailman/listinfo/paho-dev)
