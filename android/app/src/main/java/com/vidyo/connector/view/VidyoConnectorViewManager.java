package com.vidyo.connector.view;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

import javax.annotation.Nullable;

public class VidyoConnectorViewManager extends SimpleViewManager<VidyoConnectorView> {

    public static final String REACT_CLASS = "VidyoConnectorView";

    private VidyoConnectorView vidyoConnectorView;
    private ThemedReactContext context;

    @NonNull
    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @NonNull
    @Override
    public VidyoConnectorView createViewInstance(@NonNull ThemedReactContext context) {
        /* Refresh */
        if (this.context != context) {
            this.context = context;

            if (vidyoConnectorView != null) vidyoConnectorView.dispose();
            vidyoConnectorView = new VidyoConnectorView(context);
        }

        return vidyoConnectorView;
    }

    @ReactProp(name = "viewStyle")
    public void setViewStyle(VidyoConnectorView vc, String viewStyle) {
        vc.setViewStyle(viewStyle);
    }

    @ReactProp(name = "remoteParticipants")
    public void setRemoteParticipants(VidyoConnectorView vc, int remoteParticipants) {
        vc.setRemoteParticipants(remoteParticipants);
    }

    @ReactProp(name = "logFileFilter")
    public void setLogFileFilter(VidyoConnectorView vc, String logFileFilter) {
        vc.setLogFileFilter(logFileFilter);
    }

    @ReactProp(name = "logFileName")
    public void setLogFileName(VidyoConnectorView vc, String logFileName) {
        vc.setLogFileName(logFileName);
    }

    @ReactProp(name = "userData")
    public void setUserData(VidyoConnectorView vc, double userData) {
        vc.setUserData((long) userData);
    }

    @ReactProp(name = "cameraPrivacy")
    public void setCameraPrivacy(VidyoConnectorView vc, boolean privacy) {
        vc.setCameraPrivacy(privacy);
    }

    @ReactProp(name = "microphonePrivacy")
    public void setMicrophonePrivacy(VidyoConnectorView vc, boolean privacy) {
        vc.setMicrophonePrivacy(privacy);
    }

    @ReactProp(name = "mode")
    public void setMode(VidyoConnectorView vc, String mode) {
        vc.setMode(mode);
    }

    @ReactProp(name = "tradeOffProfile")
    public void setTradeOffProfile(VidyoConnectorView vc, String profile) {
        vc.setProfile(profile);
    }

    @Nullable
    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder<String, Object> builder = MapBuilder.builder();

        return builder
                .put("onConnect", MapBuilder.of("registrationName", "onConnect"))
                .put("onFailure", MapBuilder.of("registrationName", "onFailure"))
                .put("onDisconnect", MapBuilder.of("registrationName", "onDisconnect"))

                .put("onParticipantJoined", MapBuilder.of("registrationName", "onParticipantJoined"))
                .put("onParticipantLeft", MapBuilder.of("registrationName", "onParticipantLeft"))
                .put("onDynamicParticipantChanged", MapBuilder.of("registrationName", "onDynamicParticipantChanged"))
                .put("onLoudestParticipantChanged", MapBuilder.of("registrationName", "onLoudestParticipantChanged"))

                .put("onAvailableResourcesChanged", MapBuilder.of("registrationName", "onAvailableResourcesChanged"))
                .put("onMaxRemoteSourcesChanged", MapBuilder.of("registrationName", "onMaxRemoteSourcesChanged"))

                .build();
    }

    @Nullable
    @Override
    public Map<String, Integer> getCommandsMap() {
        MapBuilder.Builder<String, Integer> builder = MapBuilder.builder();

        return builder
                .put("connect", VidyoConnectorCommands.CONNECT.getCode())
                .put("disconnect", VidyoConnectorCommands.DISCONNECT.getCode())
                .put("cycleCamera", VidyoConnectorCommands.CYCLE_CAMERA.getCode())
                .build();
    }

    @Override
    public void receiveCommand(@NonNull VidyoConnectorView vc, int commandId, @Nullable ReadableArray params) {
        super.receiveCommand(vc, commandId, params);

        VidyoConnectorCommands command = VidyoConnectorCommands.match(commandId);

        switch (command) {
            case CONNECT:
                vc.connect(params);
                break;
            case DISCONNECT:
                vc.disconnect();
                break;
            case CYCLE_CAMERA:
                vc.cycleCamera();
                break;
        }
    }
}