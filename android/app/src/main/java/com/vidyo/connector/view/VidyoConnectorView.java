package com.vidyo.connector.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.view.Window;
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
import com.vidyo.VidyoClient.Device.Device;
import com.vidyo.VidyoClient.Device.LocalCamera;
import com.vidyo.VidyoClient.Endpoint.Participant;

import java.util.ArrayList;

@SuppressLint("ViewConstructor")
public class VidyoConnectorView extends FrameLayout implements IConnect, IRegisterParticipantEventListener,
        Connector.IRegisterLocalCameraEventListener {

    private static final String TAG = VidyoConnectorView.class.getCanonicalName();

    private ThemedReactContext themedReactContext;
    private Connector connector;

    private ConnectorViewStyle viewStyle = ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default;

    private int remoteParticipants = 8;
    private String logFileFilter = "debug@VidyoClient debug@VidyoConnector fatal error info";
    private String logFileName = "";
    private long userData = 0;

    private LocalCamera lastSelectedCamera;

    private boolean _initialized = false;

    private static final int PERMISSIONS_REQUEST_ALL = 0x7c9;
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    public VidyoConnectorView(ThemedReactContext context) {
        super(context);
        this.themedReactContext = context;
        Log.i(TAG, "VidyoConnectorView created.");

        Activity currentActivity = context.getCurrentActivity();

        if (currentActivity != null) {
            currentActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            if (ConnectorPkg.initialize()) {
                ConnectorPkg.setApplicationUIContext(currentActivity);
                ActivityCompat.requestPermissions(currentActivity, PERMISSIONS, PERMISSIONS_REQUEST_ALL);

                _initialized = true;
                createVidyoConnector();
                refreshUi();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int childWidthMS = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY);
        int childHeightMS = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY);
        for (int child = 0; child < getChildCount(); child++)
            getChildAt(child).measure(childWidthMS, childHeightMS);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (int child = 0; child < getChildCount(); child++)
            getChildAt(child).layout(0, 0, getWidth(), getHeight());
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
        Log.i(TAG, "Called to dispose.");

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
        if (!isAvailable()) return;

        connector.disconnect();
    }

    public void setCameraPrivacy(boolean privacy) {
        if (!isAvailable()) return;

        connector.setCameraPrivacy(privacy);
    }

    public void setMicrophonePrivacy(boolean privacy) {
        if (!isAvailable()) return;

        connector.setMicrophonePrivacy(privacy);
    }

    public void selectDefaultCamera() {
        if (!isAvailable()) return;

        connector.selectDefaultCamera();
    }

    public void cycleCamera() {
        connector.cycleCamera();
    }

    public void setMode(String mode) {
        if (!isAvailable()) return;

        ConnectorMode background = ConnectorMode.VIDYO_CONNECTORMODE_Background;
        ConnectorMode foreground = ConnectorMode.VIDYO_CONNECTORMODE_Foreground;
        ConnectorMode connectorMode = mode.equals("VIDYO_CONNECTORMODE_Background") ? background : foreground;
        connector.setMode(connectorMode);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        selectDefaultDevices();
        refreshUi();
        keepScreenOn(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        releaseDevices();
        keepScreenOn(false);
    }

    private boolean isAvailable() {
        return _initialized && connector != null;
    }

    private void selectDefaultDevices() {
        if (lastSelectedCamera != null) {
            connector.selectLocalCamera(lastSelectedCamera);
        } else {
            connector.selectDefaultCamera();
        }
        connector.selectDefaultMicrophone();
        connector.selectDefaultSpeaker();
    }

    private void releaseDevices() {
        connector.selectLocalCamera(null);
        connector.selectLocalMicrophone(null);
        connector.selectLocalSpeaker(null);
    }

    private void refreshUi() {
        if (!isAvailable()) return;

        int width = getWidth();
        int height = getHeight();
        connector.showViewAt(this, 0, 0, getWidth(), getHeight());
        Log.i(VidyoConnectorView.class.getCanonicalName(), "Show view at: " + width + ":" + height);
    }

    private void emit(String event, WritableMap payload) {
        ReactContext reactContext = (ReactContext) getContext();
        RCTEventEmitter eventEmitter = reactContext.getJSModule(RCTEventEmitter.class);
        eventEmitter.receiveEvent(getId(), event, payload);
    }

    private void keepScreenOn(boolean keep) {
        Activity activity = themedReactContext.getCurrentActivity();
        if (activity == null) return;

        Window window = activity.getWindow();
        if (window == null) return;

        if (keep) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        payload.putString("reason", "Failed: Connection attempt failed");

        emit("onFailure", payload);
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

    @Override
    public void onLocalCameraAdded(LocalCamera localCamera) {
        if (localCamera != null && localCamera.getPosition() == LocalCamera.LocalCameraPosition.VIDYO_LOCALCAMERAPOSITION_Front)
            connector.selectLocalCamera(localCamera);
    }

    @Override
    public void onLocalCameraSelected(LocalCamera localCamera) {
        lastSelectedCamera = localCamera;
    }

    @Override
    public void onLocalCameraRemoved(LocalCamera localCamera) {

    }

    @Override
    public void onLocalCameraStateUpdated(LocalCamera localCamera, Device.DeviceState deviceState) {

    }
}