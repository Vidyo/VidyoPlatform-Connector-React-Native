package com.vidyo.connector.view;

public enum VidyoConnectorCommands {
    NONE(-1),

    CONNECT(0),
    DISCONNECT(1),
    CYCLE_CAMERA(2);

    final int code;

    VidyoConnectorCommands(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static VidyoConnectorCommands match(int code) {
        for (VidyoConnectorCommands value : values()) if (value.code == code) return value;
        return NONE;
    }
}