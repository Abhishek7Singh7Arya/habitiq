import React, { createContext, useState, useContext } from 'react';
import { Platform } from 'react-native';
import axios from 'axios';

import Constants from 'expo-constants';

export const getApiBaseUrl = () => {
  if (Platform.OS === 'web') {
    return 'http://localhost:8080';
  }
  const hostUri = Constants.expoConfig?.hostUri;
  if (hostUri) {
    const ip = hostUri.split(':')[0];
    return `http://${ip}:8080`;
  }
  if (Platform.OS === 'android') {
    return 'http://10.0.2.2:8080';
  }
  return 'http://localhost:8080';
};

interface UserProfile {
  name: string;
  age?: number;
  gender?: string;
  weightKg?: number;
  heightCm?: number;
  fitnessGoal?: string;
  activityLevel?: string;
  healthConditions?: string;
  dietaryPreferences?: string;
}

interface User {
  id: string;
  email: string;
  phone: string;
  role: string;
  profile?: UserProfile;
}

interface RegistrationData {
  name: string;
  email: string;
  phone: string;
  password: string;
  age?: number;
  gender?: string;
  weightKg?: number;
  heightCm?: number;
  fitnessGoal?: string;
  activityLevel?: string;
  healthConditions?: string;
  dietaryPreferences?: string;
}

interface AuthContextType {
  token: string | null;
  user: User | null;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (data: RegistrationData) => Promise<void>;
  updateProfile: (profile: UserProfile) => Promise<void>;
  logout: () => void;
  apiBaseUrl: string;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const apiBaseUrl = getApiBaseUrl();

  const login = async (email: string, password: string) => {
    setIsLoading(true);
    try {
      const response = await axios.post(`${apiBaseUrl}/api/auth/login`, { email, password });
      if (response.data?.success) {
        const { accessToken, user: userData } = response.data.data;
        setToken(accessToken);
        setUser(userData);
      } else {
        throw new Error(response.data?.message || 'Login failed');
      }
    } catch (e: any) {
      const errorMsg = e.response?.data?.message || e.message || 'Network error occurred';
      throw new Error(errorMsg);
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (data: RegistrationData) => {
    setIsLoading(true);
    try {
      const response = await axios.post(`${apiBaseUrl}/api/auth/register`, data);
      if (response.data?.success) {
        const { accessToken, user: userData } = response.data.data;
        setToken(accessToken);
        setUser(userData);
      } else {
        throw new Error(response.data?.message || 'Registration failed');
      }
    } catch (e: any) {
      const errorMsg = e.response?.data?.message || e.message || 'Network error occurred';
      throw new Error(errorMsg);
    } finally {
      setIsLoading(false);
    }
  };

  const updateProfile = async (profile: UserProfile) => {
    if (!token || !user) throw new Error('Not authenticated');
    setIsLoading(true);
    try {
      const response = await axios.put(
        `${apiBaseUrl}/api/users/me/profile`,
        profile,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            'X-User-Id': user.id,
          },
        }
      );
      if (response.data?.success) {
        setUser((prev) => prev ? { ...prev, profile: response.data.data } : null);
      } else {
        throw new Error(response.data?.message || 'Profile update failed');
      }
    } catch (e: any) {
      const errorMsg = e.response?.data?.message || e.message || 'Failed to update profile';
      throw new Error(errorMsg);
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        isLoading,
        login,
        register,
        updateProfile,
        logout,
        apiBaseUrl,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
};
