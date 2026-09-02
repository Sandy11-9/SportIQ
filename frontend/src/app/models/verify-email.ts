export interface VerifyEmailResponse {
  id: number;
  email: string;
  verified: boolean;
}

export interface ResendOtpResponse {
  message: string;
}
