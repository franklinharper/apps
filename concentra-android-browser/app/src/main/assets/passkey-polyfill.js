// app/src/main/assets/passkey-polyfill.js
(function () {
  'use strict';

  if (typeof window.Android === 'undefined') {
    console.log('[PasskeyPolyfill] window.Android is undefined, skipping');
    return;
  }
  console.log('[PasskeyPolyfill] window.Android found, installing polyfill');

  // Sites detect WebAuthn support via window.PublicKeyCredential.
  // WebView doesn't provide it, so we define it here.
  if (typeof window.PublicKeyCredential === 'undefined') {
    window.PublicKeyCredential = function PublicKeyCredential() {};
    window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable = function () {
      return Promise.resolve(true);
    };
    window.PublicKeyCredential.isExternalCTAP2SecurityKeySupported = function () {
      return Promise.resolve(false);
    };
  }

  var pendingRequests = new Map();

  window.__passkeyResolve = function (requestId, resultJson) {
    var pending = pendingRequests.get(requestId);
    if (pending) {
      pendingRequests.delete(requestId);
      pending.resolve(resultJson);
    }
  };

  window.__passkeyReject = function (requestId, errorType, message) {
    var pending = pendingRequests.get(requestId);
    if (pending) {
      pendingRequests.delete(requestId);
      pending.reject(new DOMException(message, domExceptionName(errorType)));
    }
  };

  function domExceptionName(errorType) {
    if (errorType === 'cancelled') return 'NotAllowedError';
    if (errorType === 'not_supported') return 'NotSupportedError';
    return 'UnknownError';
  }

  function toBase64Url(buffer) {
    var bytes = new Uint8Array(buffer);
    var binary = '';
    for (var i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
  }

  function fromBase64Url(base64url) {
    var base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
    var binary = atob(base64);
    var bytes = new Uint8Array(binary.length);
    for (var i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return bytes.buffer;
  }

  function serializeCreateOptions(options) {
    var pk = options.publicKey;
    return JSON.stringify({
      rp: pk.rp,
      user: {
        id: toBase64Url(pk.user.id),
        name: pk.user.name,
        displayName: pk.user.displayName
      },
      challenge: toBase64Url(pk.challenge),
      pubKeyCredParams: pk.pubKeyCredParams,
      timeout: pk.timeout,
      excludeCredentials: (pk.excludeCredentials || []).map(function (c) {
        return { id: toBase64Url(c.id), type: c.type, transports: c.transports };
      }),
      authenticatorSelection: pk.authenticatorSelection,
      attestation: pk.attestation,
      extensions: pk.extensions
    });
  }

  function deserializeCreateResponse(json) {
    var r = JSON.parse(json);
    return {
      id: r.id,
      rawId: fromBase64Url(r.rawId),
      type: r.type,
      response: {
        clientDataJSON: fromBase64Url(r.response.clientDataJSON),
        attestationObject: fromBase64Url(r.response.attestationObject)
      },
      getClientExtensionResults: function () { return {}; }
    };
  }

  function serializeGetOptions(options) {
    var pk = options.publicKey;
    return JSON.stringify({
      challenge: toBase64Url(pk.challenge),
      timeout: pk.timeout,
      rpId: pk.rpId,
      allowCredentials: (pk.allowCredentials || []).map(function (c) {
        return { id: toBase64Url(c.id), type: c.type, transports: c.transports };
      }),
      userVerification: pk.userVerification,
      extensions: pk.extensions
    });
  }

  function deserializeGetResponse(json) {
    var r = JSON.parse(json);
    return {
      id: r.id,
      rawId: fromBase64Url(r.rawId),
      type: r.type,
      response: {
        clientDataJSON: fromBase64Url(r.response.clientDataJSON),
        authenticatorData: fromBase64Url(r.response.authenticatorData),
        signature: fromBase64Url(r.response.signature),
        userHandle: r.response.userHandle ? fromBase64Url(r.response.userHandle) : null
      },
      getClientExtensionResults: function () { return {}; }
    };
  }

  var originalCredentials = navigator.credentials;

  Object.defineProperty(navigator, 'credentials', {
    value: {
      create: function (options) {
        if (!options || !options.publicKey) {
          console.log('[PasskeyPolyfill] create: no publicKey, delegating to original');
          return originalCredentials.create(options);
        }
        return new Promise(function (resolve, reject) {
          var requestId = crypto.randomUUID();
          var serialized = serializeCreateOptions(options);
          console.log('[PasskeyPolyfill] create: requestId=' + requestId + ' origin=' + window.location.origin + ' json=' + serialized.substring(0, 200));
          pendingRequests.set(requestId, {
            resolve: function (json) {
              console.log('[PasskeyPolyfill] create resolved: ' + json.substring(0, 200));
              resolve(deserializeCreateResponse(json));
            },
            reject: function (err) {
              console.log('[PasskeyPolyfill] create rejected: ' + err.message);
              reject(err);
            }
          });
          window.Android.passkeyCreate(requestId, serialized, window.location.origin);
        });
      },
      get: function (options) {
        if (!options || !options.publicKey) {
          console.log('[PasskeyPolyfill] get: no publicKey, delegating to original');
          return originalCredentials.get(options);
        }
        return new Promise(function (resolve, reject) {
          var requestId = crypto.randomUUID();
          var serialized = serializeGetOptions(options);
          console.log('[PasskeyPolyfill] get: requestId=' + requestId + ' origin=' + window.location.origin + ' json=' + serialized.substring(0, 200));
          pendingRequests.set(requestId, {
            resolve: function (json) {
              console.log('[PasskeyPolyfill] get resolved: ' + json.substring(0, 200));
              resolve(deserializeGetResponse(json));
            },
            reject: function (err) {
              console.log('[PasskeyPolyfill] get rejected: ' + err.message);
              reject(err);
            }
          });
          window.Android.passkeyGet(requestId, serialized, window.location.origin);
        });
      },
      store: originalCredentials ? originalCredentials.store.bind(originalCredentials) : undefined,
      preventSilentAccess: originalCredentials ? originalCredentials.preventSilentAccess.bind(originalCredentials) : undefined
    },
    writable: false,
    configurable: false
  });
})();
