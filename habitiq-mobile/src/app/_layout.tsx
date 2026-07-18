import React from 'react';
import { ThemeProvider, DarkTheme, DefaultTheme } from '@react-navigation/native';
import { useColorScheme } from 'react-native';
import { Tabs } from 'expo-router';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { AuthFlowScreen } from '../components/AuthFlowScreen';
import { StatusBar } from 'expo-status-bar';

export default function RootLayout() {
  return (
    <AuthProvider>
      <LayoutContent />
    </AuthProvider>
  );
}

function LayoutContent() {
  const colorScheme = useColorScheme() ?? 'dark';
  const { token } = useAuth();

  const theme = colorScheme === 'dark' ? DarkTheme : DefaultTheme;

  if (!token) {
    return (
      <ThemeProvider value={theme}>
        <StatusBar style="light" />
        <AuthFlowScreen />
      </ThemeProvider>
    );
  }

  return (
    <ThemeProvider value={theme}>
      <StatusBar style="auto" />
      <Tabs
        screenOptions={{
          headerShown: false,
          tabBarStyle: {
            backgroundColor: colorScheme === 'dark' ? '#090d16' : '#FFFFFF',
            borderTopColor: colorScheme === 'dark' ? 'rgba(255,255,255,0.08)' : '#E5E7EB',
            paddingBottom: 5,
            height: 60,
          },
          tabBarActiveTintColor: '#10B981',
          tabBarInactiveTintColor: '#9CA3AF',
          tabBarLabelStyle: {
            fontSize: 12,
            fontWeight: 'bold',
          },
        }}
      >
        <Tabs.Screen
          name="index"
          options={{
            title: 'Timeline',
          }}
        />
        <Tabs.Screen
          name="chat"
          options={{
            title: 'AI Coach',
          }}
        />
        <Tabs.Screen
          name="upload"
          options={{
            title: 'Upload',
          }}
        />
        <Tabs.Screen
          name="explore"
          options={{
            title: 'Progress',
          }}
        />
      </Tabs>
    </ThemeProvider>
  );
}
