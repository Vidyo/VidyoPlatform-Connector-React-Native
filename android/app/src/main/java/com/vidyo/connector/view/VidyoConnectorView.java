package com.vidyo.connector.view;

import android.Manifest;
import android.app.Activity;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.vidyo.VidyoClient.Connector.Connector;
import com.vidyo.VidyoClient.Connector.Connector.ConnectorDisconnectReason;
import com.vidyo.VidyoClient.Connector.Connector.ConnectorFailReason;
import com.vidyo.VidyoClient.Connector.Connector.ConnectorMode;
import com.vidyo.VidyoClient.Connector.Connector.ConnectorViewStyle;
import com.vidyo.VidyoClient.Connector.Connector.IConnect;
import com.vidyo.VidyoClient.Connector.Connector.IRegisterParticipantEventListener;
import com.vidyo.VidyoClient.Connector.ConnectorPkg;
import com.vidyo.VidyoClient.Endpoint.Participant;

import java.util.ArrayList;

public class VidyoConnectorView extends FrameLayout implements IConnect, IRegisterParticipantEventListener {

    private Activity currentActivity;
    private ThemedReactContext reactContext;

    private Connector connector;

    private ConnectorViewStyle viewStyle = ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default;

    private int remoteParticipants = 8;
    private String logFileFilter = "debug@VidyoClient debug@VidyoConnector fatal error info";
    private String logFileName = "";
    private long userData = 0;

    private boolean _initialized = false;

    private static final String[] mPermissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public VidyoConnectorView(ThemedReactContext context) {
        super(context);
        reactContext = context;

        currentActivity = reactContext.getCurrentActivity();

        if (currentActivity != null) {
            currentActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (ConnectorPkg.initialize()) {
                ConnectorPkg.setApplicationUIContext(currentActivity);
                ActivityCompat.requestPermissions(currentActivity, mPermissions, 1);

                createVidyoConnector();
                _initialized = true;
            }
        }
    }

    public void setViewStyle(String viewStyle) {
        ConnectorViewStyle viewStyleTiles = ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Tiles;
        ConnectorViewStyle viewStyleDefault = ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default;
        ConnectorViewStyle connectorViewStyle = viewStyle.equals("ViewStyleTiles") ? viewStyleTiles : viewStyleDefault;

        this.viewStyle = connectorViewStyle;
    }

    public void setRemoteParticipants(int remoteParticipants) {
        this.remoteParticipants = remoteParticipants;
    }

    public void setLogFileFilter(String logFileFilter) {
        this.logFileFilter = logFileFilter;
    }

    public void setLogFileName(String logFileName) {
        this.logFileName = logFileName;
    }

    public void setUserData(long userData) {
        this.userData = userData;
    }

    public void createVidyoConnector() {
        if (connector != null) {
            dispose();
        }

        connector = new Connector(this, viewStyle, remoteParticipants, logFileFilter, logFileName, userData);
        connector.registerParticipantEventListener(this);
    }

    public void dispose() {
        if (this._initialized) {
            connector.unregisterParticipantEventListener();
            connector.disable();

            ConnectorPkg.setApplicationUIContext(null);
            ConnectorPkg.uninitialize();

            connector = null;
        }
    }

    public void connect(ReadableArray params) {
        String portal = params.getString(0);
        String roomKey = params.getString(1);
        String roomPin = params.getString(2);
        String displayName = params.getString(3);

        connector.connectToRoomAsGuest(portal, displayName, roomKey, roomPin, this);
    }

    public void disconnect() {
        connector.disconnect();
    }

    public void setCameraPrivacy(boolean privacy) {
        connector.setCameraPrivacy(privacy);
    }

    public void setMicrophonePrivacy(boolean privacy) {
        connector.setMicrophonePrivacy(privacy);
    }

    public void selectDefaultCamera() {
        connector.selectDefaultCamera();
    }

    public void cycleCamera() {
        connector.cycleCamera();
    }

    public void setMode(String mode) {
        ConnectorMode background = ConnectorMode.VIDYO_CONNECTORMODE_Background;
        ConnectorMode foreground = ConnectorMode.VIDYO_CONNECTORMODE_Foreground;
        ConnectorMode connectorMode = mode.equals("VIDYO_CONNECTORMODE_Background") ? background : foreground;
        connector.setMode(connectorMode);
    }

    private void emit(String event, WritableMap payload) {
        ReactContext reactContext = (ReactContext) getContext();
        RCTEventEmitter eventEmitter = reactContext.getJSModule(RCTEventEmitter.class);
        eventEmitter.receiveEvent(getId(), event, payload);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (connector != null) {
            connector.showViewAt(this, 0, 0, getWidth(), getHeight());
        }
    }

    @Override
    public void onSuccess() {
        WritableMap payload = Arguments.createMap();

        payload.putInt("target", getId());
        payload.putBoolean("status", true);
        payload.putString("reason", "Connected");

        emit("onConnect", payload);
    }

    @Override
    public void onFailure(ConnectorFailReason reason) {
        WritableMap payload = Arguments.createMap();

        payload.putInt("target", getId());
        payload.putBoolean("status", false);
        payload.putString("reason", "Failed: Connection attempt failed");

        emit("onConnect", payload);
    }

    @Override
    public void onDisconnected(ConnectorDisconnectReason reason) {
        WritableMap payload = Arguments.createMap();

        payload.putInt("target", getId());

        if (reason == ConnectorDisconnectReason.VIDYO_CONNECTORDISCONNECTREASON_Disconnected) {
            payload.putString("reason", "Disconnected: Successfully disconnected");
        } else {
            payload.putString("reason", "Disconnected: Unexpected disconnection");
        }

        emit("onDisconnect", payload);
    }

    @Override
    public void onParticipantJoined(Participant participant) {
        WritableMap payload = Arguments.createMap();
        WritableMap participantMap = Arguments.createMap();

        participantMap.putString("id", participant.id);
        participantMap.putString("name", participant.name);
        participantMap.putString("userId", participant.userId);

        payload.putMap("participant", participantMap);

        emit("onParticipantJoined", payload);
    }

    @Override
    public void onParticipantLeft(Participant participant) {
        WritableMap payload = Arguments.createMap();
        WritableMap participantMap = Arguments.createMap();

        participantMap.putString("id", participant.id);
        participantMap.putString("name", participant.name);
        participantMap.putString("userId", participant.userId);

        payload.putMap("participant", participantMap);

        emit("onParticipantLeft", payload);
    }

    @Override
    public void onDynamicParticipantChanged(ArrayList<Participant> arrayList) {
        WritableMap payload = Arguments.createMap();
        WritableArray participants = Arguments.createArray();

        for (Participant participant : arrayList) {
            WritableMap participantMap = Arguments.createMap();

            participantMap.putString("id", participant.id);
            participantMap.putString("name", participant.name);
            participantMap.putString("userId", participant.userId);

            participants.pushMap(participantMap);
        }

        payload.putArray("participants", participants);

        emit("onDynamicParticipantChanged", payload);
    }

    @Override
    public void onLoudestParticipantChanged(Participant participant, boolean b) {
        WritableMap payload = Arguments.createMap();
        WritableMap participantMap = Arguments.createMap();

        participantMap.putString("id", participant.id);
        participantMap.putString("name", participant.name);
        participantMap.putString("userId", participant.userId);

        payload.putMap("participant", participantMap);
        payload.putBoolean("audioOnly", b);

        emit("onLoudestParticipantChanged", payload);
    }
}