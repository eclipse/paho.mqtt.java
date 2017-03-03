/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * 	  Dave Locke   - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.server.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

public class MyFirstVerticle extends AbstractVerticle{

	@Override
	public void start(Future<Void> future){
		vertx.createHttpServer()
		.requestHandler(r -> {
			r.response().end("<h1>Hello from my first Vert.x 3 application - Awesome! Beans</h1>");
		})
		.listen(8080, result -> {
			if(result.succeeded()){
				future.complete();
			} else {
				future.fail(result.cause());
			}
			
		});
	}
}
