
import React, { Component } from 'react';

import { createAppContainer } from 'react-navigation';
import { createStackNavigator } from 'react-navigation-stack';
import Home from '../Page/Home';
import Conference from '../Page/Conference';

const AppNavigator = createStackNavigator(
  {
    Home: { screen: Home },
    Conference: { screen: Conference},
  },
  {
    initialRouteName: 'Home',
  }
);

export default createAppContainer(AppNavigator);