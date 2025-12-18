import React from "react";

interface PrimaryButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  loading?: boolean;
  children: React.ReactNode;
}

export const PrimaryButton: React.FC<PrimaryButtonProps> = ({
  loading,
  children,
  disabled,
  ...rest
}) => {
  const isDisabled = disabled || loading;
  return (
    <button
      {...rest}
      disabled={isDisabled}
      style={{
        width: "100%",
        padding: "10px 16px",
        borderRadius: 8,
        border: "none",
        backgroundColor: isDisabled ? "#ccc" : "#007aff",
        color: "#fff",
        fontSize: 16,
        fontWeight: 500,
        cursor: isDisabled ? "not-allowed" : "pointer",
        marginTop: 8
      }}
    >
      {loading ? "提交中..." : children}
    </button>
  );
};

