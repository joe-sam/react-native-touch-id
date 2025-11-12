'use strict';
import React, { Component, useCallback, useEffect, useState } from 'react';
import {
  Alert,
  StyleSheet,
  Text,
  TouchableHighlight,
  View,
  NativeModules
} from 'react-native';

import TouchID from "react-native-touch-id";

export const BiometricAuthComponent = () => {
   const [biometryType, setBiometryType] = useState(null);
   const clickHandler = useCallback(() => {
    TouchID.isSupported()
      .then(authenticate)
      .catch(error => {
        Alert.alert('TouchID not supported');
      });
  },[]);

   useEffect (()=>{
      TouchID.isSupported()
          .then((biometryType: any) => {
            setBiometryType( biometryType );
            console.log ('BiometryType'+biometryType);
          })
          .catch(error => {
            console.log('TouchID not supported');
          });
   },[]);

   return (
      <View style={styles.container}>

        <Text style={styles.welcome}>Welcome to AndroidX BiometricAuthComponent!</Text>

        <TouchableHighlight
          style={styles.btn}
          onPress={clickHandler}
          underlayColor="#0380BE"
          activeOpacity={1}
        >
          <Text style={{
            color: '#fff',
            fontWeight: '600'
          }}>
            {`Authenticate with ${biometryType}`}
          </Text>
        </TouchableHighlight>
      </View>
    );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF'
  },
  welcome: {
    margin: 10,
    fontSize: 20,
    fontWeight: '600',
    textAlign: 'center'
  },
  instructions: {
    marginBottom: 5,
    color: '#333333',
    fontSize: 13,
    textAlign: 'center'
  },
  btn: {
    borderRadius: 3,
    marginTop: 200,
    paddingTop: 15,
    paddingBottom: 15,
    paddingLeft: 15,
    paddingRight: 15,
    backgroundColor: '#0391D7'
  }
});

const errors = {
  "LAErrorAuthenticationFailed": "Authentication was not successful because the user failed to provide valid credentials.",
  "LAErrorUserCancel": "Authentication was canceled by the user—for example, the user tapped Cancel in the dialog.",
  "LAErrorUserFallback": "Authentication was canceled because the user tapped the fallback button (Enter Password).",
  "LAErrorSystemCancel": "Authentication was canceled by system—for example, if another application came to foreground while the authentication dialog was up.",
  "LAErrorPasscodeNotSet": "Authentication could not start because the passcode is not set on the device.",
  "LAErrorTouchIDNotAvailable": "Authentication could not start because Touch ID is not available on the device",
  "LAErrorTouchIDNotEnrolled": "Authentication could not start because Touch ID has no enrolled fingers.",
  "RCTTouchIDUnknownError": "Could not authenticate for an unknown reason.",
  "RCTTouchIDNotSupported": "Device does not support Touch ID."
};

const authenticate = async () => {
  try {
    const success = await TouchID.authenticate();
    Alert.alert('Authenticated Successfully');
  } catch (error: any) {
    console.log(error);
    Alert.alert(error.message);
  }
}


