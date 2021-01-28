package com.rnfingerprint;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import javax.crypto.Cipher;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricManager.Authenticators;
import androidx.biometric.BiometricPrompt;
import androidx.biometric.BiometricPrompt.CryptoObject;
import androidx.biometric.BiometricPrompt.AuthenticationCallback;
import androidx.biometric.BiometricPrompt.PromptInfo;
import androidx.biometric.BiometricPrompt.PromptInfo.Builder;
import androidx.fragment.app.FragmentActivity;


import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
public class FingerprintAuthModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private static final String FRAGMENT_TAG = "fingerprint_dialog";

    private KeyguardManager keyguardManager;
    private boolean isAppActive;

    public static boolean inProgress = false;

    public FingerprintAuthModule(final ReactApplicationContext reactContext) {
        super(reactContext);

        reactContext.addLifecycleEventListener(this);
    }

    private KeyguardManager getKeyguardManager() {
        if (keyguardManager != null) {
            return keyguardManager;
        }
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return null;
        }

        keyguardManager = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);

        return keyguardManager;
    }

    @Override
    public String getName() {
        return "FingerprintAuth";
    }

    @ReactMethod
    public void isSupported(final Callback reactErrorCallback, final Callback reactSuccessCallback) {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }

        int result = isFingerprintAuthAvailable();
        if (result == FingerprintAuthConstants.IS_SUPPORTED) {
            // TODO: once this package supports Android's Face Unlock,
            // implement a method to find out which type of biometry
            // (not just fingerprint) is actually supported
            reactSuccessCallback.invoke("Fingerprint");
        } else {
            reactErrorCallback.invoke("Not supported.", result);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @ReactMethod
    public void authenticate(final String reason, final ReadableMap authConfig, final Callback reactErrorCallback, final Callback reactSuccessCallback) {
        final Activity activity = getCurrentActivity();
        if (inProgress || !isAppActive || activity == null) {
            return;
        }
        inProgress = true;

        int availableResult = isFingerprintAuthAvailable();
        if (availableResult != FingerprintAuthConstants.IS_SUPPORTED) {
            inProgress = false;
            reactErrorCallback.invoke("Not supported", availableResult);
            return;
        }

        /* FINGERPRINT ACTIVITY RELATED STUFF */
        final Cipher cipher = new FingerprintCipher().getCipher();
        if (cipher == null) {
            inProgress = false;
            reactErrorCallback.invoke("Not supported", FingerprintAuthConstants.NOT_AVAILABLE);
            return;
        }

        // We should call it only when we absolutely sure that API >= 23.
        // Otherwise we will get the crash on older versions.
        // TODO: migrate to FingerprintManagerCompat
        final FingerprintManager.CryptoObject cryptoObject = new FingerprintManager.CryptoObject(cipher);

        final DialogResultHandler drh = new DialogResultHandler(reactErrorCallback, reactSuccessCallback);

        final FingerprintDialog fingerprintDialog = new FingerprintDialog();
        fingerprintDialog.setCryptoObject(cryptoObject);
        fingerprintDialog.setReasonForAuthentication(reason);
        fingerprintDialog.setAuthConfig(authConfig);
        fingerprintDialog.setDialogCallback(drh);

        if (!isAppActive) {
            inProgress = false;
            return;
        }

        fingerprintDialog.show(activity.getFragmentManager(), FRAGMENT_TAG);
    }

    private int isFingerprintAuthAvailable() {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            return FingerprintAuthConstants.NOT_SUPPORTED;
        }

        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return FingerprintAuthConstants.NOT_AVAILABLE; // we can't do the check
        }

        final KeyguardManager keyguardManager = getKeyguardManager();

        // We should call it only when we absolutely sure that API >= 23.
        // Otherwise we will get the crash on older versions.
        // TODO: migrate to FingerprintManagerCompat
        final FingerprintManager fingerprintManager = (FingerprintManager) activity.getSystemService(Context.FINGERPRINT_SERVICE);

        if (fingerprintManager == null || !fingerprintManager.isHardwareDetected()) {
            return FingerprintAuthConstants.NOT_PRESENT;
        }

        if (keyguardManager == null || !keyguardManager.isKeyguardSecure()) {
            return FingerprintAuthConstants.NOT_AVAILABLE;
        }

        if (!fingerprintManager.hasEnrolledFingerprints()) {
            return FingerprintAuthConstants.NOT_ENROLLED;
        }
        return FingerprintAuthConstants.IS_SUPPORTED;
    }

    @Override
    public void onHostResume() {
        isAppActive = true;
    }

    @Override
    public void onHostPause() {
        isAppActive = false;
    }

    @Override
    public void onHostDestroy() {
        isAppActive = false;
    }

    @ReactMethod
    public void isSensorAvailable(Promise promise) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ReactApplicationContext reactApplicationContext = getReactApplicationContext();
                BiometricManager biometricManager = BiometricManager.from(reactApplicationContext);
                int canAuthenticate = biometricManager.canAuthenticate();

                if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                    WritableMap resultMap = new WritableNativeMap();
                    resultMap.putBoolean("available", true);
                    resultMap.putString("biometryType", "Biometrics");
                    promise.resolve(resultMap);
                } else {
                    WritableMap resultMap = new WritableNativeMap();
                    resultMap.putBoolean("available", false);

                    switch (canAuthenticate) {
                        case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                            resultMap.putString("error", "BIOMETRIC_ERROR_NO_HARDWARE");
                            break;
                        case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                            resultMap.putString("error", "BIOMETRIC_ERROR_HW_UNAVAILABLE");
                            break;
                        case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                            resultMap.putString("error", "BIOMETRIC_ERROR_NONE_ENROLLED");
                            break;
                    }

                    promise.resolve(resultMap);
                }
            } else {
                WritableMap resultMap = new WritableNativeMap();
                resultMap.putBoolean("available", false);
                resultMap.putString("error", "Unsupported android version");
                promise.resolve(resultMap);
            }
        } catch (Exception e) {
            promise.reject("Error detecting biometrics availability: " + e.getMessage(), "Error detecting biometrics availability: " + e.getMessage());
        }
    }

    @ReactMethod
    public void simplePrompt(final ReadableMap params, final Promise promise) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            UiThreadUtil.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String cancel = params.getString("cancel");
                                String title = params.getString("title");
                                String subtitle = params.getString("subtitle");
                                String description = params.getString("description");
                                Boolean usecrypto = params.getBoolean("usecrypto");

                                AuthenticationCallback authCallback = new SimplePromptCallback(promise);
                                FragmentActivity fragmentActivity = (FragmentActivity) getCurrentActivity();
                                Executor executor = Executors.newSingleThreadExecutor();
                                BiometricPrompt biometricPrompt = new BiometricPrompt(fragmentActivity, executor, authCallback);


                                final BiometricPrompt.PromptInfo.Builder promptBuilder = new BiometricPrompt.PromptInfo.Builder()
                                                .setNegativeButtonText(cancel)
                                                .setTitle(title)
                                                .setSubtitle(subtitle)
                                                .setDescription(description)
                                                .setAllowedAuthenticators(usecrypto? Authenticators.BIOMETRIC_STRONG : Authenticators.BIOMETRIC_WEAK );

                                  PromptInfo promptInfo = promptBuilder.build();

                                if (usecrypto) {
                                    final Cipher cipher = new FingerprintCipher().getCipher();
                                            if (cipher == null) {
                                                inProgress = false;
                                                promise.reject("Cipher Not supported", "Cipher Not supported");
                                                return;
                                            }
                                    final CryptoObject crypto = new BiometricPrompt.CryptoObject(cipher);
                                    biometricPrompt.authenticate(promptInfo,crypto) ; // requires BIOMETRIC_STRONG e.g fingerprint only but not face/iris
                                }
                                else {
                                    biometricPrompt.authenticate(promptInfo); // requires BIOMETRIC_WEAK e.g fingerprint & or face/iris
                                }

                                // ELSE
                            } catch (Exception e) {
                                promise.reject("Error displaying local biometric prompt: " + e.getMessage(), "Error displaying local biometric prompt: " + e.getMessage());
                            }
                        }
                    });
        } else {
            promise.reject("Cannot display biometric prompt on android versions below 6.0", "Cannot display biometric prompt on android versions below 6.0");
        }
    }


}
