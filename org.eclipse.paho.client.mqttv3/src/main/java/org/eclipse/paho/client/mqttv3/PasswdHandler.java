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
