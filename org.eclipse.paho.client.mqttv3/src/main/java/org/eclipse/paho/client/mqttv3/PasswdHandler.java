/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at
 *   https://www.eclipse.org/org/documents/edl-v10.php
 *
 */
package org.eclipse.paho.client.mqttv3;


import org.eclipse.paho.client.mqttv3.internal.NetworkModule;

/**
 *  In order to use other lib to handler password like JWT etc.
 *  Get networkModule real info to generate signature.
 */
public interface PasswdHandler {

    String handler(NetworkModule networkModule);

    class Factory{
        public static PasswdHandler passwdHandler;

        public static void setPasswdHandler(PasswdHandler passwdHandler){
            Factory.passwdHandler = passwdHandler;
        }

        public static PasswdHandler getInstance(){
            return passwdHandler;
        }
    }
}
