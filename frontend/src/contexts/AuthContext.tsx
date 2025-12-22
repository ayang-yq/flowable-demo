import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { message } from 'antd';
import api from '../services/api';

interface User {
  id: string;
  username: string;
  email: string;
  fullName?: string;
  department?: string;
  phone?: string;
  status: string;
  roles: string[];
}

interface AuthContextType {
  user: User | null;
  login: (username: string, password: string) => Promise<boolean>;
  logout: () => void;
  loading: boolean;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const initAuth = () => {
      const token = localStorage.getItem('token');
      const savedUser = localStorage.getItem('user');

      if (token && savedUser) {
        try {
          setUser(JSON.parse(savedUser));
        } catch (error) {
          console.error('Failed to parse saved user:', error);
          localStorage.removeItem('token');
          localStorage.removeItem('user');
        }
      }
      setLoading(false);
    };

    initAuth();
  }, []);

  const login = async (username: string, password: string): Promise<boolean> => {
    try {
      setLoading(true);

      // 使用 Basic Auth 进行认证
      const credentials = btoa(`${username}:${password}`);

      // 设置请求头
      const originalHeaders = api.defaults.headers.common;
      api.defaults.headers.common['Authorization'] = `Basic ${credentials}`;

      try {
        const response = await api.get('/users/current');

        if (response.data) {
          const userData = response.data;

          // 保存用户信息和认证令牌
          localStorage.setItem('token', credentials);
          localStorage.setItem('user', JSON.stringify(userData));

          setUser(userData);
          api.defaults.headers.common['Authorization'] = `Basic ${credentials}`;

          message.success('登录成功');
          return true;
        }
        return false;
      } catch (error: any) {
        console.error('Login error:', error);

        if (error.response?.status === 401) {
          message.error('用户名或密码错误');
        } else if (error.response?.status === 403) {
          message.error('用户已被禁用');
        } else {
          message.error('登录失败，请稍后重试');
        }

        // 恢复原始请求头
        api.defaults.headers.common = originalHeaders;
        return false;
      }
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
    delete api.defaults.headers.common['Authorization'];
    message.info('已退出登录');
  };

  const value: AuthContextType = {
    user,
    login,
    logout,
    loading,
    isAuthenticated: !!user,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
