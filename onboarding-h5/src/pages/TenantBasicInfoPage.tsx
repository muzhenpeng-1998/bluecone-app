import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { FormSection } from "../components/FormSection";
import { TextField } from "../components/TextField";
import { PrimaryButton } from "../components/PrimaryButton";
import { submitTenantBasicInfo } from "../api/onboarding";
import { useSessionToken } from "../hooks/useSessionToken";

export const TenantBasicInfoPage: React.FC = () => {
  const navigate = useNavigate();
  const { sessionToken } = useSessionToken();
  const [tenantName, setTenantName] = useState("");
  const [legalName, setLegalName] = useState("");
  const [businessCategory, setBusinessCategory] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    if (!sessionToken) {
      setError("入驻会话已失效，请返回扫码页重新开始。");
      return;
    }

    if (!tenantName.trim()) {
      setError("请填写品牌名称。");
      return;
    }

    setLoading(true);
    setError(null);
    try {
      await submitTenantBasicInfo({
        sessionToken,
        tenantName: tenantName.trim(),
        legalName: legalName.trim() || undefined,
        businessCategory: businessCategory.trim() || undefined
      });
      navigate("/onboarding/store");
    } catch (e) {
      const err = e as Error;
      setError(err.message || "提交品牌信息失败，请稍后重试");
    } finally {
      setLoading(false);
    }
  };

  return (
    <FormSection
      title="品牌信息"
      subtitle="请填写品牌和主体信息，用于后续小程序注册和展示。"
    >
      <TextField
        label="品牌名称（对外展示）"
        required
        placeholder="例如：Dont Worry Coffee"
        value={tenantName}
        onChange={(e) => setTenantName(e.target.value)}
      />
      <TextField
        label="主体名称（公司/个体户）"
        placeholder="例如：杭州某某咖啡有限公司"
        value={legalName}
        onChange={(e) => setLegalName(e.target.value)}
      />
      <TextField
        label="业态类别"
        placeholder="例如：COFFEE / RESTAURANT"
        value={businessCategory}
        onChange={(e) => setBusinessCategory(e.target.value)}
      />
      {error && (
        <p style={{ color: "#e53935", fontSize: 12, margin: "0 0 8px" }}>
          {error}
        </p>
      )}
      <PrimaryButton onClick={handleSubmit} loading={loading}>
        下一步：填写门店信息
      </PrimaryButton>
    </FormSection>
  );
};

