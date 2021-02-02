package com.rnfingerprint;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

public class SimplePromptCallback extends BiometricPrompt.AuthenticationCallback {
    private Promise promise;

    public SimplePromptCallback(Promise promise) {
        super();
        this.promise = promise;
    }

    @Override
    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
        super.onAuthenticationError(errorCode, errString);
        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
            WritableMap resultMap = new WritableNativeMap();
            resultMap.putBoolean("success", false);
            resultMap.putString("error", "User cancellation");
            this.promise.resolve(resultMap);
        } else {
            this.promise.reject(Integer.toString(errorCode), errString.toString());
        }
    }

    @Override
    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
        super.onAuthenticationSucceeded(result);
        WritableMap resultMap = new WritableNativeMap();
        resultMap.putBoolean("success", true);
        this.promise.resolve(resultMap);
    }

    /*
    @Override not functional possibly undocumented
     BiometricPrompt.AuthenticationCallback onAuthenticationHelp not implemented ??
      */
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {

      //  super.onAuthenticationHelp(helpCode, helpString); // throws not overrideable
         WritableMap resultMap = new WritableNativeMap();
            resultMap.putBoolean("success", false);
            resultMap.putString("error", helpString.toString());
         this.promise.resolve(resultMap);
    }

    @Override
    public void onAuthenticationFailed() {
        super.onAuthenticationFailed();
        WritableMap resultMap = new WritableNativeMap();
            resultMap.putBoolean("success", false);
            resultMap.putString("error", "Authentication failed");
        this.promise.resolve(resultMap);
    }
}