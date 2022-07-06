//
//  VidyoConnectorView.m
//  VidyoReactNative
//
//  Created by serhii benedyshyn on 4/21/19.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "VidyoConnectorView.h"


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
  [self showView];
}

- (void)removeFromSuperview {
  [super removeFromSuperview];

  /* Shut down the renderer when we are moving away from view */
  [self hideView];
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

- (void)setCameraPrivacy:(BOOL)cameraPrivacy
{
  [_connector setCameraPrivacy:cameraPrivacy];
}

- (void)setMicrophonePrivacy:(BOOL)microphonePrivacy
{
  [_connector setMicrophonePrivacy:microphonePrivacy];
}

- (void)cycleCamera
{
  [_connector cycleCamera];
}

- (void)setMode:(VCConnectorMode)mode
{
  [self.connector setMode:mode];

  if (mode == VCConnectorModeBackground) {
    /* Background: shut down the rendering */
    [self hideView];
  } else {
    /* Foreground: Re-attach renderer and start rendering back */
    [self reAssignView];
    [self showView];
  }
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
