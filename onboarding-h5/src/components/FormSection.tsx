import React from "react";

interface FormSectionProps {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
}

export const FormSection: React.FC<FormSectionProps> = ({
  title,
  subtitle,
  children
}) => {
  return (
    <section
      style={{
        backgroundColor: "#fff",
        borderRadius: 12,
        padding: 16,
        marginBottom: 16,
        boxShadow: "0 1px 3px rgba(0,0,0,0.04)"
      }}
    >
      <h2 style={{ fontSize: 16, margin: 0, marginBottom: 4 }}>{title}</h2>
      {subtitle && (
        <p style={{ fontSize: 12, color: "#888", margin: "0 0 8px" }}>
          {subtitle}
        </p>
      )}
      {children}
    </section>
  );
};

