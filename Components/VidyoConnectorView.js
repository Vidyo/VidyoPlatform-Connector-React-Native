import React, {Component} from 'react';
import { View, UIManager, requireNativeComponent, findNodeHandle } from 'react-native';

import PropTypes from 'prop-types';

const { Commands } = UIManager.getViewManagerConfig('VidyoConnectorView');

class VidyoConnectorView extends Component {

  constructor(props) {
    super(props);
  }

  componentDidMount() {
    this.vcHandle = findNodeHandle(this.vcRef);
  }

  connectToRoomAsGuest(portal, roomKey, roomPin, displayName) {
    UIManager.dispatchViewManagerCommand(this.vcHandle, Commands.connect, [portal, roomKey, roomPin, displayName]);
  }

  disconnect() {
    UIManager.dispatchViewManagerCommand(this.vcHandle, Commands.disconnect, []);
  }

  cycleCamera() {
    UIManager.dispatchViewManagerCommand(this.vcHandle, Commands.cycleCamera, []);
  }

  _onConnect = (event) => {
    if (this.props.onConnect) {
      const { ...payload } = event.nativeEvent
      this.props.onConnect(payload);
    }
  };

  _onFailure = (event) => {
    if (this.props.onFailure) {
      const { ...payload } = event.nativeEvent
      this.props.onFailure(payload);
    }
  }

  _onDisconnect = (event) => {
    if (this.props.onDisconnect) {
      const { ...payload } = event.nativeEvent
      this.props.onDisconnect(payload);
    }
  }

  _onParticipantJoined = (event) => {
    if (this.props.onParticipantJoined) {
      const { ...payload } = event.nativeEvent
      this.props.onParticipantJoined(payload);
    }
  }

  _onParticipantLeft = (event) => {
    if (this.props.onParticipantLeft) {
      const { ...payload } = event.nativeEvent
      this.props.onParticipantLeft(payload);
    }
  }

  _onDynamicParticipantChanged = (event) => {
    if (this.props.onDynamicParticipantChanged) {
      const { ...payload } = event.nativeEvent
      this.props.onDynamicParticipantChanged(payload);
    }
  }

  _onLoudestParticipantChanged = (event) => {
    if (this.props.onLoudestParticipantChange) {
      const { ...payload } = event.nativeEvent
      this.props.onLoudestParticipantChange(payload);
    }
  }

  _onAvailableResourcesChanged = (event) => {
    if (this.props.onAvailableResourcesChanged) {
      const { ...payload } = event.nativeEvent
      this.props.onAvailableResourcesChanged(payload);
    }
  }

  _onMaxRemoteSourcesChanged = (event) => {
    if (this.props.onMaxRemoteSourcesChanged) {
      const { ...payload } = event.nativeEvent
      this.props.onMaxRemoteSourcesChanged(payload);
    }
  }

  render() {
    return (
      <VidyoConnectorView_ ref = { _ => this.vcRef = _ } { ...this.props } 
      
        onConnect                   = {this._onConnect}
        onFailure                   = {this._onFailure}
        onDisconnect                = {this._onDisconnect}

        onParticipantJoined         = {this._onParticipantJoined}
        onParticipantLeft           = {this._onParticipantLeft}
        onDynamicParticipantChanged = {this._onDynamicParticipantChanged}
        onLoudestParticipantChanged = {this._onLoudestParticipantChanged}

        onAvailableResourcesChanged = {this._onAvailableResourcesChanged}
        onMaxRemoteSourcesChanged   = {this._onMaxRemoteSourcesChanged}
      />
    );
  }

}

VidyoConnectorView.propTypes = {
  viewStyle:                    PropTypes.string,
  remoteParticipants:           PropTypes.number,
  logFileFilter:                PropTypes.string,
  logFilename:                  PropTypes.string,
  userData:                     PropTypes.number,

  cameraPrivacy:                PropTypes.bool,
  microphonePrivacy:            PropTypes.bool,

  tradeOffProfile:              PropTypes.string,

  onConnect:                    PropTypes.func,
  onFailure:                    PropTypes.func,
  onDisconnect:                 PropTypes.func,

  onParticipantJoined:          PropTypes.func,
  onParticipantLeft:            PropTypes.func,
  onDynamicParticipantChanged:  PropTypes.func,
  onLoudestParticipantChanged:  PropTypes.func,

  onAvailableResourcesChanged:  PropTypes.func,
  onMaxRemoteSourcesChanged:    PropTypes.func,

  ...View.propTypes,
};

var VidyoConnectorView_ = requireNativeComponent(`VidyoConnectorView`, VidyoConnectorView, {
  nativeOnly: {
    onConnect:                    true,
    onFailure:                    true,
    onDisconnect:                 true,

    onParticipantJoined:          true,
    onParticipantLeft:            true,
    onDynamicParticipantChanged:  true,
    onLoudestParticipantChanged:  true,

    onAvailableResourcesChanged:  true,
    onMaxRemoteSourcesChanged:    true,
  },
});

export default VidyoConnectorView;