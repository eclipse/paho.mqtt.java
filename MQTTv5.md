# Eclipse Paho Java Client for MQTTv5

The v5 client is build on the same foundations as the v3 client is, however it is being heavily refactored using lessons learnt from the v3 client and feedback from the community. Like v3 client, the Paho Java Client provides two APIs: MqttAsyncClient provides a fully asychronous API where completion of activities is notified via registered callbacks. MqttClient is a synchronous wrapper around MqttAsyncClient where functions appear synchronous to the application.

## Using the Paho Java MQTTv5 Client

### Downloading

Eclipse hosts a Nexus repository for those who want to use Maven to manage their dependencies. The released libraries are also available in the Maven Central repository.

Add the repository definition and the dependency definition shown below to your pom.xml.

Replace %REPOURL% with either ``` https://repo.eclipse.org/content/repositories/paho-releases/ ``` for the official releases, or ``` https://repo.eclipse.org/content/repositories/paho-snapshots/  ``` for the nightly snapshots. Replace %VERSION% with the level required .

The latest release version is ```1.2.5``` and the current snapshot version is ```1.2.6-SNAPSHOT```.

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
        <artifactId>org.eclipse.paho.mqttv5.client</artifactId>
        <version>%VERSION%</version>
    </dependency>
</dependencies>
</project>

```

If you find that there is functionality missing or bugs in the release version, you may want to try using the snapshot version to see if this helps before raising a feature request or an issue.

### Building from source

There are two active branches on the Paho Java git repository, ```master``` which is used to produce stable releases, and ```develop``` where active development is carried out. By default cloning the git repository will download the ```master``` branch, to build from ```develop``` make sure you switch to the remote branch: ``` git checkout -b develop remotes/origin/develop ```

To then build the library run the following maven command: ```mvn package -DskipTests```

This will build the client library without running the tests. The jars for the library, source and javadoc can be found in the ```org.eclipse.paho.mqttv5.client/target``` directory.

## Documentation
Reference documentation is online at: [http://www.eclipse.org/paho/files/javadoc/index.html](http://www.eclipse.org/paho/files/javadoc/index.html)

Log and Debug in the Java Client: [https://wiki.eclipse.org/Paho/Log_and_Debug_in_the_Java_client](https://wiki.eclipse.org/Paho/Log_and_Debug_in_the_Java_client)

## Getting Started

The included code below is a very basic sample that connects to a server and publishes a message using the MqttClient asynchronous API. More extensive samples demonstrating the use of the Asynchronous API will be added in the ```org.eclipse.paho.sample.mqttv5app``` directory of the source soon.


```
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

public class MqttPublishSample {

    public static void main(String[] args) {

        String topic        = "MQTT Examples";
        String content      = "Message from MqttPublishSample";
        int qos             = 2;
        String broker       = "tcp://iot.eclipse.org:1883";
        String clientId     = "JavaSample";
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttConnectionOptions connOpts = new MqttConnectionOptions();
            connOpts.setCleanStart(false);
            MqttAsyncClient sampleClient = new MqttAsyncClient(broker, clientId, persistence);
            System.out.println("Connecting to broker: " + broker);
            IMqttToken token = sampleClient.connect(connOpts);
            token.waitForCompletion();
            System.out.println("Connected");
            System.out.println("Publishing message: "+content);
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            token = sampleClient.publish(topic, message);
            token.waitForCompletion();
            System.out.println("Disconnected");
            System.out.println("Close client.");
            sampleClient.close();
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

