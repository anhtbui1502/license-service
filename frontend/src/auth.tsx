import { createContext, ReactNode, useCallback, useContext, useEffect, useState } from 'react';
import { api, Role, User } from './api';

interface AuthState {
  user: User | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  hasRole: (...roles: Role[]) => boolean;
}

const AuthContext = createContext<AuthState>(null as unknown as AuthState);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Gọi /me để biết đã đăng nhập chưa và để server phát cookie XSRF-TOKEN
    api.get<User>('/auth/me')
      .then((r) => setUser(r.data))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));

    const id = api.interceptors.response.use(undefined, (err) => {
      if (err?.response?.status === 401 && !err.config?.url?.startsWith('/auth/')) {
        setUser(null);
      }
      return Promise.reject(err);
    });
    return () => api.interceptors.response.eject(id);
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const r = await api.post<User>('/auth/login', { username, password });
    setUser(r.data);
  }, []);

  const logout = useCallback(async () => {
    await api.post('/auth/logout').catch(() => undefined);
    setUser(null);
  }, []);

  const hasRole = useCallback((...roles: Role[]) => !!user && roles.includes(user.role), [user]);

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, hasRole }}>{children}</AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
