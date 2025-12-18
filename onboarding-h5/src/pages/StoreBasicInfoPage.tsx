import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { FormSection } from "../components/FormSection";
import { TextField } from "../components/TextField";
import { PrimaryButton } from "../components/PrimaryButton";
import { submitStoreBasicInfo } from "../api/onboarding";
import { useSessionToken } from "../hooks/useSessionToken";

export const StoreBasicInfoPage: React.FC = () => {
  const navigate = useNavigate();
  const { sessionToken } = useSessionToken();
  const [storeName, setStoreName] = useState("");
  const [city, setCity] = useState("");
  const [district, setDistrict] = useState("");
  const [address, setAddress] = useState("");
  const [bizScene, setBizScene] = useState("");
  const [contactPhone, setContactPhone] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    if (!sessionToken) {
      setError("入驻会话已失效，请返回扫码页重新开始。");
      return;
    }

    if (!storeName.trim()) {
      setError("请填写门店名称。");
      return;
    }

    setLoading(true);
    setError(null);
    try {
      await submitStoreBasicInfo({
        sessionToken,
        storeName: storeName.trim(),
        city: city.trim() || undefined,
        district: district.trim() || undefined,
        address: address.trim() || undefined,
        bizScene: bizScene.trim() || undefined,
        contactPhone: contactPhone.trim() || undefined
      });
      navigate("/onboarding/wechat");
    } catch (e) {
      const err = e as Error;
      setError(err.message || "提交门店信息失败，请稍后重试");
    } finally {
      setLoading(false);
    }
  };

  return (
    <FormSection
      title="门店信息"
      subtitle="这些信息会用于后续点单、配送范围等配置。"
    >
      <TextField
        label="门店名称"
        required
        placeholder="例如：Dont Worry Coffee 君品店"
        value={storeName}
        onChange={(e) => setStoreName(e.target.value)}
      />
      <TextField
        label="城市"
        placeholder="例如：杭州"
        value={city}
        onChange={(e) => setCity(e.target.value)}
      />
      <TextField
        label="区域/商圈"
        placeholder="例如：钱江世纪城"
        value={district}
        onChange={(e) => setDistrict(e.target.value)}
      />
      <TextField
        label="详细地址"
        placeholder="例如：某某路某某号"
        value={address}
        onChange={(e) => setAddress(e.target.value)}
      />
      <TextField
        label="业务场景"
        placeholder="例如：堂食 / 外卖"
        value={bizScene}
        onChange={(e) => setBizScene(e.target.value)}
      />
      <TextField
        label="门店电话"
        placeholder="例如：13800000000"
        value={contactPhone}
        onChange={(e) => setContactPhone(e.target.value)}
      />
      {error && (
        <p style={{ color: "#e53935", fontSize: 12, margin: "0 0 8px" }}>
          {error}
        </p>
      )}
      <PrimaryButton onClick={handleSubmit} loading={loading}>
        下一步：开通/绑定微信小程序
      </PrimaryButton>
    </FormSection>
  );
};

