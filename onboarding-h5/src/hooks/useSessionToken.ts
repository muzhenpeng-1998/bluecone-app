import { useEffect, useState } from "react";

const STORAGE_KEY = "bluecone_onboarding_session_token";

export const useSessionToken = () => {
  const [sessionToken, setSessionTokenState] = useState<string | null>(null);

  useEffect(() => {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    if (stored) {
      setSessionTokenState(stored);
    }
  }, []);

  const setSessionToken = (token: string) => {
    window.localStorage.setItem(STORAGE_KEY, token);
    setSessionTokenState(token);
  };

  const clearSessionToken = () => {
    window.localStorage.removeItem(STORAGE_KEY);
    setSessionTokenState(null);
  };

  return { sessionToken, setSessionToken, clearSessionToken };
};

