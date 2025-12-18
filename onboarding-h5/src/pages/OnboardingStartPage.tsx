import React, { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { FormSection } from "../components/FormSection";
import { PrimaryButton } from "../components/PrimaryButton";
import { startOnboardingSession } from "../api/onboarding";
import { useSessionToken } from "../hooks/useSessionToken";

export const OnboardingStartPage: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { sessionToken, setSessionToken } = useSessionToken();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const searchParams = new URLSearchParams(location.search);
  const channelCode = searchParams.get("channel") || undefined;

  const handleStart = async () => {
    if (sessionToken) {
      navigate("/onboarding/tenant");
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const resp = await startOnboardingSession({
        channelCode
      });
      setSessionToken(resp.sessionToken);
      navigate("/onboarding/tenant");
    } catch (e) {
      const err = e as Error;
      setError(err.message || "创建入驻会话失败，请稍后重试");
    } finally {
      setLoading(false);
    }
  };

  return (
    <FormSection
      title="开始入驻"
      subtitle="点击下方按钮，我们会为您创建一个入驻会话，后续填写品牌和门店信息。"
    >
      {sessionToken && (
        <p
          style={{
            fontSize: 12,
            color: "#666",
            margin: "0 0 8px"
          }}
        >
          检测到已有进行中的入驻流程，可直接继续。
        </p>
      )}
      {error && (
        <p style={{ color: "#e53935", fontSize: 12, margin: "0 0 8px" }}>
          {error}
        </p>
      )}
      <PrimaryButton onClick={handleStart} loading={loading}>
        {sessionToken ? "继续入驻" : "开始入驻"}
      </PrimaryButton>
    </FormSection>
  );
};

