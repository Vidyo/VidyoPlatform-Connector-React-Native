import React, { Component } from "react";

import { NavigationContainer } from "@react-navigation/native";
import { createStackNavigator } from "@react-navigation/stack";

import Home from "./Page/Home";
import Conference from "./Page/Conference";

const Stack = createStackNavigator();

export default class App extends Component {
  render() {
    return (
      <NavigationContainer>
        <Stack.Navigator initialRouteName={"Home"}>
          <Stack.Screen
            name={"Home"}
            component={Home}
            options={{ headerShown: false }}
          />
          <Stack.Screen
            name={"Conference"}
            component={Conference}
            options={{
              /* Show the header on the conference */
              headerShown: true,
            }}
          />
        </Stack.Navigator>
      </NavigationContainer>
    );
  }
}
