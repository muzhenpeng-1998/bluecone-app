export interface StartSessionRequest {
  channelCode?: string;
}

export interface StartSessionResponse {
  sessionToken: string;
}

export interface TenantBasicInfoRequest {
  sessionToken: string;
  tenantName: string;
  legalName?: string;
  businessCategory?: string;
  sourceChannel?: string;
}

export interface TenantBasicInfoResponse {
  tenantId: number;
}

export interface StoreBasicInfoRequest {
  sessionToken: string;
  storeName: string;
  city?: string;
  district?: string;
  address?: string;
  bizScene?: string;
  contactPhone?: string;
}

export interface StoreBasicInfoResponse {
  storeId: number;
}

export type RegisterType = "FORMAL" | "TRIAL";

export interface WechatRegisterRequest {
  sessionToken: string;
  registerType: RegisterType;
  companyName?: string;
  companyCode?: string;
  companyCodeType?: number;
  legalPersonaWechat?: string;
  legalPersonaName?: string;
  trialMiniProgramName?: string;
  trialOpenId?: string;
  requestPayloadJson?: string;
}

export interface WechatRegisterResponse {
  taskId: number;
}

export interface WechatAuthUrlResponse {
  authorizeUrl: string;
}

const jsonHeaders = {
  "Content-Type": "application/json"
};

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP error ${res.status}`);
  }
  return res.json() as Promise<T>;
}

export async function startOnboardingSession(
  body: StartSessionRequest
): Promise<StartSessionResponse> {
  const res = await fetch("/api/onboarding/session/start", {
    method: "POST",
    headers: jsonHeaders,
    body: JSON.stringify(body)
  });
  return handleResponse<StartSessionResponse>(res);
}

export async function submitTenantBasicInfo(
  body: TenantBasicInfoRequest
): Promise<TenantBasicInfoResponse> {
  const res = await fetch("/api/onboarding/tenant/basic-info", {
    method: "POST",
    headers: jsonHeaders,
    body: JSON.stringify(body)
  });
  return handleResponse<TenantBasicInfoResponse>(res);
}

export async function submitStoreBasicInfo(
  body: StoreBasicInfoRequest
): Promise<StoreBasicInfoResponse> {
  const res = await fetch("/api/onboarding/store/basic-info", {
    method: "POST",
    headers: jsonHeaders,
    body: JSON.stringify(body)
  });
  return handleResponse<StoreBasicInfoResponse>(res);
}

export async function registerWechatMiniProgram(
  body: WechatRegisterRequest
): Promise<WechatRegisterResponse> {
  const res = await fetch("/api/onboarding/wechat/register", {
    method: "POST",
    headers: jsonHeaders,
    body: JSON.stringify(body)
  });
  return handleResponse<WechatRegisterResponse>(res);
}

export async function getWechatAuthUrl(
  sessionToken: string
): Promise<WechatAuthUrlResponse> {
  const res = await fetch(
    `/api/onboarding/wechat/auth-url?sessionToken=${encodeURIComponent(
      sessionToken
    )}`,
    {
      method: "GET"
    }
  );
  return handleResponse<WechatAuthUrlResponse>(res);
}

