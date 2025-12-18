import React from "react";
import { Outlet } from "react-router-dom";

export const OnboardingLayout: React.FC = () => {
  return (
    <div
      style={{
        maxWidth: 480,
        margin: "0 auto",
        minHeight: "100vh",
        padding: "16px",
        boxSizing: "border-box",
        backgroundColor: "#f7f7f7"
      }}
    >
      <header style={{ marginBottom: 16 }}>
        <h1 style={{ fontSize: 20, margin: 0 }}>BlueCone 商户入驻</h1>
        <p style={{ margin: "4px 0 0", fontSize: 12, color: "#666" }}>
          扫码 → 填写信息 → 开通/绑定小程序
        </p>
      </header>
      <main>
        <Outlet />
      </main>
    </div>
  );
};

