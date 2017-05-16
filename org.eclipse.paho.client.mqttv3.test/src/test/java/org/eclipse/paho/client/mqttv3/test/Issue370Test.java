/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.paho.client.mqttv3.test;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Test;

/**
 * This is a reproducer for issue #370
 */
public class Issue370Test {
    @Test
    public void noOpenClose() throws MqttException {
        final MqttAsyncClient client = new MqttAsyncClient("tcp://localhost", "foo-bar");
        client.close();
    }
}
