import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { OnboardingLayout } from "./pages/OnboardingLayout";
import { OnboardingStartPage } from "./pages/OnboardingStartPage";
import { TenantBasicInfoPage } from "./pages/TenantBasicInfoPage";
import { StoreBasicInfoPage } from "./pages/StoreBasicInfoPage";
import { WechatOpenPage } from "./pages/WechatOpenPage";
import { OnboardingResultPage } from "./pages/OnboardingResultPage";

export const AppRouter: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/onboarding/start" replace />} />
      <Route path="/onboarding" element={<OnboardingLayout />}>
        <Route path="start" element={<OnboardingStartPage />} />
        <Route path="tenant" element={<TenantBasicInfoPage />} />
        <Route path="store" element={<StoreBasicInfoPage />} />
        <Route path="wechat" element={<WechatOpenPage />} />
        <Route path="result" element={<OnboardingResultPage />} />
      </Route>
    </Routes>
  );
};

