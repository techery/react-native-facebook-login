package com.magus.fblogin;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FacebookLoginModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private final String CALLBACK_TYPE_SUCCESS = "success";
    private final String CALLBACK_TYPE_ERROR = "error";
    private final String CALLBACK_TYPE_CANCEL = "cancel";

    private CallbackManager callbackManager;
    private Callback tokenCallback;

    public FacebookLoginModule(ReactApplicationContext reactContext) {
        super(reactContext);

        reactContext.addActivityEventListener(this);

        FacebookSdk.sdkInitialize(reactContext.getApplicationContext());

        callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(final LoginResult loginResult) {
                        if (tokenCallback != null) {
                            WritableMap map = Arguments.createMap();
                            map.putMap("credentials", buildCredentials(loginResult));
                            consumeCallback(CALLBACK_TYPE_SUCCESS, map);
                        }
                    }

                    @Override
                    public void onCancel() {
                        if (tokenCallback != null) {
                            WritableMap map = Arguments.createMap();
                            map.putString("message", "FacebookCallback onCancel event triggered");
                            map.putString("eventName", "onCancel");
                            consumeCallback(CALLBACK_TYPE_CANCEL, map);
                        }
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        if (tokenCallback != null) {
                            WritableMap map = Arguments.createMap();
                            map.putString("message", exception.getMessage());
                            map.putString("eventName", "onError");
                            consumeCallback(CALLBACK_TYPE_ERROR, map);
                        }
                    }
                });
    }

    private WritableMap buildCredentials(LoginResult loginResult) {
        final AccessToken accessToken = loginResult.getAccessToken();

        WritableMap credentials = Arguments.createMap();
        credentials.putString("token", accessToken.getToken());
        credentials.putString("tokenExpirationDate", formatExpirationDate(accessToken.getExpires()));
        credentials.putString("userId", Profile.getCurrentProfile().getId());
        credentials.putArray("permissions", convertSetToWritableArray(accessToken.getPermissions()));

        return credentials;
    }

    private WritableArray convertSetToWritableArray(Set<String> permissions) {
        return Arguments.fromArray(new ArrayList<>(permissions));
    }

    private String formatExpirationDate(Date expirationDate) {
        final String template = "yyyy-MM-dd'T'HH:mm:ssZZZZZ";
        final Locale locale = new Locale("en", "US", "POSIX");
        return new SimpleDateFormat(template, locale).format(expirationDate);
    }

    private void handleError(String value, String onPermissionsMissing, String callbackType) {
        WritableMap map = Arguments.createMap();
        map.putString("message", value);
        map.putString("eventName", onPermissionsMissing);
        consumeCallback(callbackType, map);
    }

    private void consumeCallback(String type, WritableMap map) {
        if (tokenCallback != null) {
            map.putString("type", type);
            map.putString("provider", "facebook");

            if (type.equals(CALLBACK_TYPE_SUCCESS)) {
                tokenCallback.invoke(null, map);
            } else {
                tokenCallback.invoke(map, null);
            }

            tokenCallback = null;
        }
    }

    @Override
    public String getName() {
        return "FBLoginManager";
    }

    @ReactMethod
    public void loginWithPermissions(ReadableArray permissions, final Callback callback) {
        if (tokenCallback != null) {
            AccessToken accessToken = AccessToken.getCurrentAccessToken();

            WritableMap map = Arguments.createMap();

            if (accessToken != null) {
                map.putString("token", AccessToken.getCurrentAccessToken().getToken());
                map.putString("eventName", "onLoginFound");
                map.putBoolean("cache", true);
                consumeCallback(CALLBACK_TYPE_SUCCESS, map);
            } else {
                map.putString("message", "Cannot register multiple callbacks");
                map.putString("eventName", "onCancel");
                consumeCallback(CALLBACK_TYPE_CANCEL, map);
            }
        }

        tokenCallback = callback;

        List<String> _permissions = getPermissions(permissions);
        if (_permissions != null && _permissions.size() > 0 && _permissions.contains("email")) {
            Log.i("FBLoginPermissions", "Using: " + _permissions.toString());

            Activity currentActivity = getCurrentActivity();

            if (currentActivity != null) {
                LoginManager.getInstance().logInWithReadPermissions(currentActivity, _permissions);
            } else {
                handleError("Activity doesn't exist", "onError", CALLBACK_TYPE_ERROR);
            }
        } else {
            handleError("Insufficient permissions", "onPermissionsMissing", CALLBACK_TYPE_ERROR);
        }

    }

    @ReactMethod
    public void logout(final Callback callback) {
        WritableMap map = Arguments.createMap();

        tokenCallback = callback;
        LoginManager.getInstance().logOut();

        map.putString("message", "Facebook Logout executed");
        map.putString("eventName", "onLogout");
        consumeCallback(CALLBACK_TYPE_SUCCESS, map);
    }

    private List<String> getPermissions(ReadableArray permissions) {
        List<String> _permissions = new ArrayList<>();
        if (permissions != null && permissions.size() > 0) {
            for (int i = 0; i < permissions.size(); i++) {
                if ("String".equals(permissions.getType(i).name())) {
                    String permission = permissions.getString(i);
                    _permissions.add(permission);
                }
            }
        }
        return _permissions;
    }

    @ReactMethod
    public void getCurrentToken(final Callback callback) {
        AccessToken currentAccessToken = AccessToken.getCurrentAccessToken();
        if (currentAccessToken != null) {
            callback.invoke(currentAccessToken.getToken());
        } else {
            //noinspection NullArgumentToVariableArgMethod
            callback.invoke(null);
        }
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
