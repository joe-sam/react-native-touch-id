import { NativeModules, processColor } from 'react-native';
import { androidApiErrorMap, androidModuleErrorMap } from './data/errors';
import { getError, TouchIDError, TouchIDUnifiedError } from './errors';
const NativeTouchID = NativeModules.FingerprintAuth;
const { codes, errors } = require('./data/errors');
export default {
  isSupported(config) {
    return new Promise((resolve, reject) => {

          NativeTouchID.isSensorAvailable()
          .then((resultObject) => {
              const { available, biometryType, error } = resultObject;
                  if (available && biometryType === "Biometrics") { // this is the UNIVERSAL type in ANDROID (system selected biometric)
                      return resolve(biometryType);
                    } else {
                      return reject(createError(config, error, codes.androidModuleCodes.NOT_SUPPORTED));
                    }


          })
          .catch((error) => {
            console.log('Biometric', 'Failed');
            return reject(createError(config, error,  codes.androidModuleCodes.NOT_SUPPORTED));
          });

    });
  },

  authenticate(reason, config) {
    var DEFAULT_CONFIG = {
      title: 'Authentication Required',
      subtitle: 'Touch/Face Id',
      imageColor: '#1306ff',
      imageErrorColor: '#ff0000',
      sensorDescription: 'Touch sensor',
      sensorErrorDescription: 'Failed',
      cancelText: 'Cancel',
      unifiedErrors: false
    };
    var authReason = reason ? reason : ' ';
    var authConfig = Object.assign({}, DEFAULT_CONFIG, config);
    var imageColor = processColor(authConfig.imageColor);
    var imageErrorColor = processColor(authConfig.imageErrorColor);

    authConfig.imageColor = imageColor;
    authConfig.imageErrorColor = imageErrorColor;

    return new Promise((resolve, reject) => {
      NativeTouchID.simplePrompt({
        title: authConfig.title,
        subtitle:  authConfig.subtitle,
         cancel: authConfig.cancelText,
         description: authConfig.sensorDescription
         })
      .then((resultObject) => {
        const { success,error } = resultObject

        if (success) {
          console.log('Biometric', 'Success')
          return resolve(true);
        } else {
          console.log('Biometric', 'Cancelled');
          return reject(createError(authConfig, "Cancelled", codes.androidModuleCodes.AUTHENTICATION_CANCELED));
        }
      })
      .catch((sperror) => {
        console.log('Biometric XX Failed:');
        return reject(createError(authConfig, "Unknown error", codes.androidModuleCodes.AUTHENTICATION_FAILED));
      });

    });

  }
};

function createError(config, error, code) {
  const { unifiedErrors } = config || {};
  const errorCode = androidApiErrorMap[code] || androidModuleErrorMap[code];

  if (unifiedErrors) {
    return new TouchIDUnifiedError(getError(errorCode));
  }

  return new TouchIDError('Touch ID Error', error, errorCode);
}
