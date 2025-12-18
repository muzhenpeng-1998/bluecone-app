import React from "react";

interface TextFieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  required?: boolean;
}

export const TextField: React.FC<TextFieldProps> = ({
  label,
  required,
  ...rest
}) => {
  return (
    <div style={{ marginBottom: 12 }}>
      <label
        style={{
          display: "block",
          marginBottom: 4,
          fontSize: 13,
          color: "#333"
        }}
      >
        {label}
        {required && (
          <span style={{ color: "red", marginLeft: 2 }}>*</span>
        )}
      </label>
      <input
        {...rest}
        style={{
          width: "100%",
          padding: "8px 10px",
          borderRadius: 6,
          border: "1px solid #ddd",
          fontSize: 14,
          boxSizing: "border-box"
        }}
      />
    </div>
  );
};

