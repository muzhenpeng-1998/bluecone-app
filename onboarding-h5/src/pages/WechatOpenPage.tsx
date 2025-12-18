import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { FormSection } from "../components/FormSection";
import { TextField } from "../components/TextField";
import { PrimaryButton } from "../components/PrimaryButton";
import {
  getWechatAuthUrl,
  registerWechatMiniProgram
} from "../api/onboarding";
import { useSessionToken } from "../hooks/useSessionToken";

export const WechatOpenPage: React.FC = () => {
  const navigate = useNavigate();
  const { sessionToken } = useSessionToken();

  const [companyName, setCompanyName] = useState("");
  const [companyCode, setCompanyCode] = useState("");
  const [legalPersonaName, setLegalPersonaName] = useState("");
  const [legalPersonaWechat, setLegalPersonaWechat] = useState("");

  const [formalLoading, setFormalLoading] = useState(false);
  const [authLoading, setAuthLoading] = useState(false);
  const [formalError, setFormalError] = useState<string | null>(null);
  const [authError, setAuthError] = useState<string | null>(null);

  const handleFormalSubmit = async () => {
    if (!sessionToken) {
      setFormalError("入驻会话已失效，请返回扫码页重新开始。");
      return;
    }

    if (
      !companyName.trim() ||
      !companyCode.trim() ||
      !legalPersonaName.trim() ||
      !legalPersonaWechat.trim()
    ) {
      setFormalError(
        "请完整填写企业名称、统一社会信用代码、法人姓名和法人微信号。"
      );
      return;
    }

    setFormalLoading(true);
    setFormalError(null);
    try {
      const resp = await registerWechatMiniProgram({
        sessionToken,
        registerType: "FORMAL",
        companyName: companyName.trim(),
        companyCode: companyCode.trim(),
        companyCodeType: 1,
        legalPersonaWechat: legalPersonaWechat.trim(),
        legalPersonaName: legalPersonaName.trim(),
        requestPayloadJson: undefined
      });
      navigate(
        `/onboarding/result?type=formal&taskId=${encodeURIComponent(
          String(resp.taskId)
        )}`
      );
    } catch (e) {
      const err = e as Error;
      setFormalError(err.message || "提交代注册申请失败，请稍后重试");
    } finally {
      setFormalLoading(false);
    }
  };

  const handleAuth = async () => {
    if (!sessionToken) {
      setAuthError("入驻会话已失效，请返回扫码页重新开始。");
      return;
    }

    setAuthLoading(true);
    setAuthError(null);
    try {
      const resp = await getWechatAuthUrl(sessionToken);
      if (!resp.authorizeUrl) {
        throw new Error("未获取到授权地址");
      }
      window.location.href = resp.authorizeUrl;
    } catch (e) {
      const err = e as Error;
      setAuthError(err.message || "获取授权地址失败，请稍后重试");
      setAuthLoading(false);
    }
  };

  return (
    <>
      <FormSection
        title="方式一：代注册正式小程序"
        subtitle="由平台帮你提交企业主体资料，快速生成小程序。"
      >
        <TextField
          label="企业/个体工商户名称"
          required
          placeholder="营业执照上的名称"
          value={companyName}
          onChange={(e) => setCompanyName(e.target.value)}
        />
        <TextField
          label="统一社会信用代码"
          required
          placeholder="18位统一社会信用代码"
          value={companyCode}
          onChange={(e) => setCompanyCode(e.target.value)}
        />
        <TextField
          label="法人姓名"
          required
          value={legalPersonaName}
          onChange={(e) => setLegalPersonaName(e.target.value)}
        />
        <TextField
          label="法人微信号"
          required
          value={legalPersonaWechat}
          onChange={(e) => setLegalPersonaWechat(e.target.value)}
        />
        {formalError && (
          <p
            style={{ color: "#e53935", fontSize: 12, margin: "0 0 8px" }}
          >
            {formalError}
          </p>
        )}
        <PrimaryButton onClick={handleFormalSubmit} loading={formalLoading}>
          提交代注册申请
        </PrimaryButton>
      </FormSection>

      <FormSection
        title="方式二：绑定已有小程序"
        subtitle="如果你已有小程序，可以直接授权给平台使用。"
      >
        {authError && (
          <p
            style={{ color: "#e53935", fontSize: 12, margin: "0 0 8px" }}
          >
            {authError}
          </p>
        )}
        <PrimaryButton onClick={handleAuth} loading={authLoading}>
          去微信授权绑定
        </PrimaryButton>
      </FormSection>
    </>
  );
};

