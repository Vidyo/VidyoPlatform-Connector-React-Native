import React, { Component } from 'react';

import { StyleSheet, View, Button, Image } from 'react-native';

export default class Home extends Component {
  render() {
    return (
      <View style={styles.container}>
        <Button title="Connect to the conference" onPress={() => this.props.navigation.navigate('Conference')} />
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
  },
});