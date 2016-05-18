# Eclipse Paho Java Client

The Paho Java Client is an MQTT client library written in Java for developing applications that run on the JVM or other Java compatible platforms such as Android

The Paho Java Client provides two APIs: MqttAsyncClient provides a fully asychronous API where completion of activities is notified via registered callbacks. MqttClient is a synchronous wrapper around MqttAsyncClient where functions appear synchronous to the application.

## Features
|                     |                    |   |                      |                    |
|---------------------|--------------------|---|----------------------|--------------------|
| MQTT 3.1            | :heavy_check_mark: |   | Automatic Reconnect  | :heavy_multiplication_x: (Coming in 1.1.0) |
| MQTT 3.1.1          | :heavy_check_mark: |   | Offline Buffering    | :heavy_multiplication_x: (Coming in 1.1.0) |
| LWT                 | :heavy_check_mark: |   | WebSocket Support    | :heavy_multiplication_x: (Coming in 1.1.0) |
| SSL / TLS           | :heavy_check_mark: |   | Standard TCP Support | :heavy_check_mark: |
| Message Persistence | :heavy_check_mark: |   |


## Project description:

The Paho project has been created to provide reliable open-source implementations of open and standard messaging protocols aimed at new, existing, and emerging applications for Machine-to-Machine (M2M) and Internet of Things (IoT).
Paho reflects the inherent physical and cost constraints of device connectivity. Its objectives include effective levels of decoupling between devices and applications, designed to keep markets open and encourage the rapid growth of scalable Web and Enterprise middleware and applications.


## Links

- Project Website: [https://www.eclipse.org/paho](https://www.eclipse.org/paho)
- Eclipse Project Information: [https://projects.eclipse.org/projects/iot.paho](https://projects.eclipse.org/projects/iot.paho)
- Paho Java Client Page: [https://eclipse.org/paho/clients/java/](https://eclipse.org/paho/clients/java)
- GitHub: [https://github.com/eclipse/paho.mqtt.java](https://github.com/eclipse/paho.mqtt.java)
- Twitter: [@eclipsepaho](https://twitter.com/eclipsepaho)
- Issues: [https://github.com/eclipse/paho.mqtt.java/issues](https://github.com/eclipse/paho.mqtt.java/issues)
- Mailing-list: [https://dev.eclipse.org/mailman/listinfo/paho-dev](https://dev.eclipse.org/mailman/listinfo/paho-dev)

## Using the Paho Java Client

### Downloading

Eclipse hosts a Nexus repository for those who want to use Maven to manage their dependencies. The released libraries are also available in the Maven Central repository.

Add the repository definition and the dependency definition shown below to your pom.xml.

Replace %REPOURL% with either ``` https://repo.eclipse.org/content/repositories/paho-releases/ ``` for the official releases, or ``` https://repo.eclipse.org/content/repositories/paho-snapshots/  ``` for the nightly snapshots. Replace %VERSION% with the level required .
The latest release version is ```1.0.2``` and the current snapshot version is ```1.0.3```.

```
<project ...>
<repositories>
    <repository>
        <id>Eclipse Paho Repo</id>
        <url>%REPOURL%</url>
    </repository>
</repositories>
...
<dependencies>
    <dependency>
        <groupId>org.eclipse.paho</groupId>
        <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
        <version>%VERSION%</version>
    </dependency>
</dependencies>
</project>

```

If you find that there is functionality missing or bugs in the release version, you may want to try using the snapshot version to see if this helps before raising a feature request or an issue.

### Building from source

There are two active branches on the Paho Java git repository, ```master``` which is used to produce stable releases, and ```develop``` where active development is carried out. By default cloning the git repository will download the ```master``` branch, to build from ```develop``` make sure you switch to the remote branch: ``` git checkout -b develop remotes/origin/develop ```

To then build the library run the following maven command: ```mvn package -DskipTests```

This will build the client library without running the tests. The jars for the library, source and javadoc can be found in the ```org.eclipse.paho.client.mqttv3/target``` directory.

## Documentation
Reference documentation is online at: [http://www.eclipse.org/paho/files/javadoc/index.html](http://www.eclipse.org/paho/files/javadoc/index.html)

Log and Debug in the Java Client: [https://wiki.eclipse.org/Paho/Log_and_Debug_in_the_Java_client](https://wiki.eclipse.org/Paho/Log_and_Debug_in_the_Java_client)

## Getting Started

The included code below is a very basic sample that connects to a server and publishes a message using the MqttClient synchronous API. More extensive samples demonstrating the use of the Asynchronous API can be found in the ```org.eclipse.paho.sample.mqttv3app``` directory of the source.


```
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttPublishSample {

    public static void main(String[] args) {

        String topic        = "MQTT Examples";
        String content      = "Message from MqttPublishSample";
        int qos             = 2;
        String broker       = "tcp://iot.eclipse.org:1883";
        String clientId     = "JavaSample";
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: "+broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");
            System.out.println("Publishing message: "+content);
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            sampleClient.publish(topic, message);
            System.out.println("Message published");
            sampleClient.disconnect();
            System.out.println("Disconnected");
            System.exit(0);
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
    }
}
```
