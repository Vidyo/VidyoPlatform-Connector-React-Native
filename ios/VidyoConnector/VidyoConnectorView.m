//
//  VidyoConnectorView.m
//  VidyoReactNative
//
//  Created by serhii benedyshyn on 4/21/19.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "VidyoConnectorView.h"


@interface VidyoConnectorView () {
@private
  VCLocalCamera *lastSelectedCamera;
  BOOL          devicesSelected;
  BOOL          cameraPrivacy;
  BOOL          microphonePrivacy;
}
@end

@implementation VidyoConnectorView {
  RCTBridge *_bridge;
  RCTEventDispatcher *_eventDispatcher;
}

- (instancetype)initWithBridge:(RCTBridge *)bridge {
  RCTAssertParam(bridge);
  RCTAssertParam(bridge.eventDispatcher);
  
  if ((self = [super initWithFrame:CGRectZero])) {
    _eventDispatcher = bridge.eventDispatcher;
  }
  
  [VCConnectorPkg vcInitialize];
  [self createVidyoConnector];
  return self;
}

- (void)didMoveToWindow
{
  [[NSNotificationCenter defaultCenter] addObserver:self
                                           selector:@selector(appWillResignActive:)
                                               name:UIApplicationWillResignActiveNotification
                                             object:nil];

  [[NSNotificationCenter defaultCenter] addObserver:self
                                           selector:@selector(appDidBecomeActive:)
                                               name:UIApplicationDidBecomeActiveNotification
                                             object:nil];
  
  [self selectDefaultDevices];
  [self reAssignView];
  [self showView];
  
  [[UIApplication sharedApplication] setIdleTimerDisabled:YES];
}

- (void)removeFromSuperview {
  [super removeFromSuperview];
  
  /* Shut down the renderer when we are moving away from view */
  [self hideView];
  [self releaseDevices];
  
  [[NSNotificationCenter defaultCenter] removeObserver:self];
  
  [[UIApplication sharedApplication] setIdleTimerDisabled:NO];
}

- (void)createVidyoConnector
{
  const char * logLevels = "debug@VidyoClient debug@VidyoConnector fatal error info";
  _connector = [[VCConnector alloc] init:(void *)&self
                               ViewStyle:VCConnectorViewStyleDefault
                      RemoteParticipants:8 // max participant titles (0 - only self view)
                           LogFileFilter:logLevels
                             LogFileName:[_logFileName  UTF8String]
                                UserData:_userData];
  
  [_connector registerParticipantEventListener:self];
  [_connector registerLocalCameraEventListener:self];
}

#pragma mark - Application Lifecycle

- (void)appWillResignActive:(NSNotification*)notification {
    if (_connector) {
      if ([_connector getState] == VCConnectorStateConnected) {
        // Connected or connecting to a resource.
        // Enable camera privacy so remote participants do not see a frozen frame.
        [_connector setCameraPrivacy:YES];
      } else {
        // Not connected to a resource.
        // Release camera, mic, and speaker from this app while backgrounded.
        [self releaseDevices];
      }
      
      [_connector setMode:VCConnectorModeBackground];
    }
}

- (void)appDidBecomeActive:(NSNotification*)notification {
    if (_connector) {
        [_connector setMode:VCConnectorModeForeground];

        if (!devicesSelected) {
            // Devices have been released when backgrounding (in appWillResignActive). Re-select them.
            // Select the previously selected local camera and default mic/speaker
            [self selectDefaultDevices];
            [_connector setMicrophonePrivacy:microphonePrivacy];
        }

        // Reestablish camera privacy states
        [_connector setCameraPrivacy: cameraPrivacy];
    }
}

- (void)selectDefaultDevices {
  if (lastSelectedCamera) {
    [_connector selectLocalCamera: lastSelectedCamera];
  } else {
    [_connector selectDefaultCamera];
  }
  
  [_connector selectDefaultMicrophone];
  [_connector selectDefaultSpeaker];
  devicesSelected = YES;
}

- (void)releaseDevices {
  [_connector selectLocalCamera: nil];
  [_connector selectLocalSpeaker: nil];
  [_connector selectLocalMicrophone: nil];
  devicesSelected = NO;
}

- (void)showView {
  dispatch_async(dispatch_get_main_queue(), ^{
    int screenWidth  = [[UIScreen mainScreen] bounds].size.width;
    int screenHeight = [[UIScreen mainScreen] bounds].size.height;
    [self.connector showViewAt:(void *)&self
                             X:0
                             Y:0
                         Width:screenWidth
                        Height:screenHeight];
  });
}

 /* Re-attach the renderer to the view */
- (void)reAssignView {
  dispatch_async(dispatch_get_main_queue(), ^{
    [self.connector assignViewToCompositeRenderer:(void *)&self
                                        ViewStyle:VCConnectorViewStyleDefault
                               RemoteParticipants:8];
  });
}

 /* Shut down the rendering */
- (void)hideView {
  dispatch_async(dispatch_get_main_queue(), ^{
    [self.connector hideView:(void *)&self];
  });
}

- (void)setViewStyle:(VCConnectorViewStyle)viewStyle
{
  _viewStyle = viewStyle;
}

- (void)setRemoteParticipants:(int)remoteParticipants
{
  _remoteParticipants = remoteParticipants;
}

- (void)setLogFileFilter:(NSString *)logFileFilter
{
  _logFileFilter = logFileFilter;
}

- (void)setLogFileName:(NSString *)logFileName
{
  _logFileName = logFileName;
}

- (void)setUserData:(int)userData
{
  _userData = userData;
}

- (void)setCameraPrivacy:(BOOL)privacy
{
  cameraPrivacy = privacy;
  [_connector setCameraPrivacy:privacy];
}

- (void)setMicrophonePrivacy:(BOOL)privacy
{
  microphonePrivacy = privacy;
  [_connector setMicrophonePrivacy:privacy];
}

- (void)cycleCamera
{
  [_connector cycleCamera];
}

- (void)connectToRoomAsGuest:(NSString *)portal RoomKey:(NSString *)roomKey RoomPin:(NSString *)roomPin DisplayName:(NSString *)displayName
{
  [self.connector connectToRoomAsGuest:[portal cStringUsingEncoding:NSASCIIStringEncoding]
                           DisplayName:[displayName cStringUsingEncoding:NSASCIIStringEncoding]
                               RoomKey:[roomKey cStringUsingEncoding:NSASCIIStringEncoding]
                               RoomPin:[roomPin cStringUsingEncoding:NSASCIIStringEncoding]
                     ConnectorIConnect:self];
}

- (void)disconnect
{
  [self.connector disconnect];
}

- (void)onSuccess
{
  if (!self.onConnect) {
    return;
  }
  self.onConnect(@{@"status": @"true", @"reason": @"Connected"});
}

- (void)onFailure:(VCConnectorFailReason)reason
{
  if (!self.onFailure) {
    return;
  }
  self.onFailure(@{@"reason": @"Failed: Connetion attempt failed"});
}

- (void)onDisconnected:(VCConnectorDisconnectReason)reason
{
  if (!self.onDisconnect) {
    return;
  }
  if (reason == VCConnectorDisconnectReasonDisconnected) {
    self.onDisconnect(@{@"reason": @"Disconnected: Succesfully disconnected"});
  } else {
    self.onDisconnect(@{@"reason": @"Disconnected: Unexpected disconnection"});
  }
}

#pragma mark - VCConnectorIRegisterLocalCameraEventListener

-(void) onLocalCameraAdded:(VCLocalCamera*)localCamera {
    if (localCamera != nil && [localCamera getPosition] == VCLocalCameraPositionFront) {
        [_connector selectLocalCamera: localCamera];
    }
}

-(void) onLocalCameraRemoved:(VCLocalCamera*)localCamera {}
    
-(void) onLocalCameraSelected:(VCLocalCamera*)localCamera {
      // If a camera is selected, then update lastSelectedCamera.
      // localCamera will be nil only when backgrounding app while disconnected.
      if (localCamera) {
          lastSelectedCamera = localCamera;
      }
}

-(void) onLocalCameraStateUpdated:(VCLocalCamera*)localCamera State:(VCDeviceState)state {}

- (void)onParticipantJoined:(VCParticipant*)participant
{
  if (!self.onParticipantJoined) {
    return;
  }
  NSDictionary *nsParticipant = @{@"id": participant.id, @"name": participant.name, @"userId": participant.userId};
  self.onParticipantJoined(@{@"participant": nsParticipant});
}

- (void)onParticipantLeft:(VCParticipant*)participant
{
  if (!self.onParticipantLeft) {
    return;
  }
  NSDictionary *nsParticipant = @{@"id": participant.id, @"name": participant.name, @"userId": participant.userId};
  self.onParticipantLeft(@{@"participant": nsParticipant});
}

- (void)onDynamicParticipantChanged:(NSMutableArray*)participants
{
  if (!self.onDynamicParticipantChanged) {
    return;
  }
  NSMutableArray *nsParticipants = [[NSMutableArray alloc] init];

  for (int i = 0; i < [participants count]; i++) {
    VCParticipant *participant = participants[i];
    nsParticipants[i] = @{@"id": participant.id, @"name": participant.name, @"userId": participant.userId};
  }

  NSArray * participantsResult = [NSArray arrayWithArray:nsParticipants];
  self.onDynamicParticipantChanged(@{@"participants": participantsResult});
}

- (void)onLoudestParticipantChanged:(VCParticipant*)participant AudioOnly:(BOOL)audioOnly
{
  if (!self.onLoudestParticipantChanged) {
    return;
  }
  NSDictionary *nsParticipant = @{@"id": participant.id, @"name": participant.name, @"userId": participant.userId};
  self.onLoudestParticipantChanged(@{@"participant": nsParticipant, @"audioOnly": @(audioOnly)});
}

@end
