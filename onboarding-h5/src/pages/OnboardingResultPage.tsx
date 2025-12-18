import React from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { FormSection } from "../components/FormSection";
import { PrimaryButton } from "../components/PrimaryButton";
import { useSessionToken } from "../hooks/useSessionToken";

export const OnboardingResultPage: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { clearSessionToken } = useSessionToken();

  const searchParams = new URLSearchParams(location.search);
  const type = searchParams.get("type");

  let title = "操作完成";
  let message = "如需帮助，请联系平台运营。";

  if (type === "formal") {
    title = "代注册申请已提交";
    message =
      "我们已向微信提交小程序注册申请，后续会通过短信或运营联系你确认进度。";
  } else if (type === "auth") {
    title = "授权绑定完成";
    message =
      "如果刚刚已经在微信完成授权，请回到入驻页面刷新后即可使用。";
  }

  const handleBackToStart = () => {
    clearSessionToken();
    navigate("/onboarding/start", { replace: true });
  };

  return (
    <>
      <FormSection title={title}>
        <p
          style={{
            fontSize: 14,
            color: "#555",
            margin: 0
          }}
        >
          {message}
        </p>
      </FormSection>
      <FormSection title="后续操作">
        <p
          style={{
            fontSize: 12,
            color: "#777",
            margin: "0 0 8px"
          }}
        >
          如需修改信息，可以联系平台运营；后续我们也会提供商家自助管理入口。
        </p>
        <PrimaryButton onClick={handleBackToStart}>
          重新开始入驻流程
        </PrimaryButton>
      </FormSection>
    </>
  );
};

