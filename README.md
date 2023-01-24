# VidyoPlatform Reference App ReactNative

## Clone Repository

git clone https://github.com/Vidyo/vidyoplatform-connector-react-native.git

## Acquire VidyoClient iOS and Android SDKs
> Note: Highlighted steps are very important because samples already contain configurations specified below and both SDK packages are linked as relative folders located in VidyoConnector-react-native directory.

1. Download the latest Vidyo.io Android & iOS SDK packages:

    > https://static.vidyo.io/22.6.0.4/package/VidyoClient-AndroidSDK.zip
    
    > https://static.vidyo.io/22.6.0.4/package/VidyoClient-iOSSDK.zip
    
2. **Unzip VidyoClient-AndroidSDK folder and move VidyoClient.aar from `VidyoClient-AndroidSDK/lib/android` 
   to `VidyoConnector-react-native/android/app/libs/`**
   
3. **Unzip VidyoClient-iOSSDK folder and move VidyoClientIOS.xcframework from `VidyoClient-iOSSDK/lib/ios` 
   to `VidyoConnector-react-native/ios/lib/ios/`**

## Preparing

Go to `./android/local.properties` and set location of the Android SDK

## Build and Run Application

1. Follow next link and install all described dependencies.

    `https://facebook.github.io/react-native/docs/getting-started.html`

2. Install dependencies.

    `yarn`
    
    `cd ios`
    
    `pod install`
    
    `cd ..`

3. Build and run the application on the iOS or Android device.

    `npx react-native run-ios`
    
    `npx react-native run-android`
