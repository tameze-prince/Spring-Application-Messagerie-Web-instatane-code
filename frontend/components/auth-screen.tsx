"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api/client";
import { useAuth } from "@/lib/context/AuthContext";

type Mode = "login" | "register" | "verify" | "forgot";
type FieldErrors = Record<string, string>;

const MAX_EMAIL_LENGTH = 255;
const MAX_USERNAME_LENGTH = 50;
const MAX_NAME_LENGTH = 80;
const MAX_PASSWORD_LENGTH = 128;
const USERNAME_PATTERN = /^[A-Za-z0-9._-]+$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const content: Record<Mode, { title: string; copy: string }> = {
  login: { title: "Welcome back.", copy: "Your conversations are right where you left them." },
  register: { title: "Make space for better conversations.", copy: "Create your Wavelength account in a minute." },
  verify: { title: "Check your inbox.", copy: "Enter the six-digit code sent to your email address." },
  forgot: { title: "Let's get you back in.", copy: "Password reset is not connected to the backend yet." },
};

function readFormValue(form: FormData, name: string): string {
  const value = form.get(name);
  return typeof value === "string" ? value : "";
}

function validateEmail(email: string): string | undefined {
  if (!email) return "Email is required.";
  if (email.length > MAX_EMAIL_LENGTH) return "Email must not exceed 255 characters.";
  if (!EMAIL_PATTERN.test(email)) return "Enter a valid email address.";
  return undefined;
}

function validatePassword(password: string): string | undefined {
  if (!password.trim()) return "Password is required.";
  if (password.length < 8 || password.length > MAX_PASSWORD_LENGTH) {
    return "Password must be between 8 and 128 characters.";
  }
  return undefined;
}

export default function AuthScreen({ mode }: { mode: Mode }) {
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [code, setCode] = useState(["", "", "", "", "", ""]);
  const [isLoading, setIsLoading] = useState(false);
  const router = useRouter();
  const { login, register } = useAuth();

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccess("");
    setFieldErrors({});

    const form = new FormData(event.currentTarget);
    const nextErrors: FieldErrors = {};

    if (mode === "login") {
      const loginValue = readFormValue(form, "login").trim();
      const password = readFormValue(form, "password");

      if (!loginValue) nextErrors.login = "Enter your email or username.";
      else if (loginValue.length > MAX_EMAIL_LENGTH) nextErrors.login = "Email or username must not exceed 255 characters.";

      const passwordError = validatePassword(password);
      if (passwordError) nextErrors.password = passwordError;

      if (Object.keys(nextErrors).length) {
        setFieldErrors(nextErrors);
        setError("Please correct the highlighted fields.");
        return;
      }

      setIsLoading(true);
      try {
        await login(loginValue, password);
        router.push("/chats");
      } catch (err: unknown) {
        const apiError = err instanceof ApiError ? err : new ApiError(0, "Something went wrong. Please try again.");
        if (apiError.fieldErrors) {
          setFieldErrors(apiError.fieldErrors);
          setError("Please correct the highlighted fields.");
        } else {
          setError(apiError.message);
        }
      } finally {
        setIsLoading(false);
      }
      return;
    }

    if (mode === "register") {
      const username = readFormValue(form, "username").trim();
      const email = readFormValue(form, "email").trim().toLowerCase();
      const password = readFormValue(form, "password");
      const firstName = readFormValue(form, "firstName").trim();
      const lastName = readFormValue(form, "lastName").trim();

      if (!username) nextErrors.username = "Username is required.";
      else if (username.length < 3 || username.length > MAX_USERNAME_LENGTH) nextErrors.username = "Username must be between 3 and 50 characters.";
      else if (!USERNAME_PATTERN.test(username)) nextErrors.username = "Use only letters, numbers, dots, underscores and hyphens.";

      const emailError = validateEmail(email);
      if (emailError) nextErrors.email = emailError;

      const passwordError = validatePassword(password);
      if (passwordError) nextErrors.password = passwordError;

      if (firstName.length > MAX_NAME_LENGTH) nextErrors.firstName = "First name must not exceed 80 characters.";
      else if (/[\u0000-\u001F\u007F]/.test(firstName)) nextErrors.firstName = "First name contains invalid characters.";

      if (lastName.length > MAX_NAME_LENGTH) nextErrors.lastName = "Last name must not exceed 80 characters.";
      else if (/[\u0000-\u001F\u007F]/.test(lastName)) nextErrors.lastName = "Last name contains invalid characters.";

      if (Object.keys(nextErrors).length) {
        setFieldErrors(nextErrors);
        setError("Please correct the highlighted fields.");
        return;
      }

      setIsLoading(true);
      try {
        await register({
          username,
          email,
          password,
          ...(firstName ? { firstName } : {}),
          ...(lastName ? { lastName } : {}),
        });
        router.push("/chats");
      } catch (err: unknown) {
        const apiError = err instanceof ApiError ? err : new ApiError(0, "Something went wrong. Please try again.");
        if (apiError.fieldErrors) {
          setFieldErrors(apiError.fieldErrors);
          setError("Please correct the highlighted fields.");
        } else {
          setError(apiError.message);
        }
      } finally {
        setIsLoading(false);
      }
      return;
    }

    if (mode === "forgot") {
      const email = readFormValue(form, "email").trim().toLowerCase();
      const emailError = validateEmail(email);
      if (emailError) {
        setFieldErrors({ email: emailError });
        setError("Please correct the highlighted fields.");
      } else {
        setError("Password reset is not available yet. No request was sent.");
      }
      return;
    }

    if (!/^\d{6}$/.test(code.join(""))) {
      setError("Enter all six digits of the verification code.");
      return;
    }
    setError("Email verification is not connected to the backend yet. No code was verified.");
  }

  function renderFieldError(name: string) {
    const message = fieldErrors[name];
    return message ? <p className="form-error field-error" id={`${name}-error`}>{message}</p> : null;
  }

  return (
    <main className="auth-page">
      <aside className="auth-aside">
        <Link href="/" className="landing-brand"><span>W</span> Wavelength</Link>
        <div>
          <p className="eyebrow">A MESSENGER ON YOUR TERMS</p>
          <h2>Stay close,<br /><em>at your own pace.</em></h2>
          <div className="auth-quote"><i>"</i><p>It feels like the first messaging app that lets me decide how much the interface asks of me.</p><b>— Early Wavelength member</b></div>
        </div>
        <p className="auth-foot">Your messages. Your rhythm. Your Frequency.</p>
      </aside>
      <section className="auth-content">
        <Link className="auth-back" href="/">← Back to Wavelength</Link>
        <div className="auth-card">
          <p className="eyebrow">
            {mode === "login" ? "SIGN IN" : mode === "register" ? "CREATE ACCOUNT" : mode === "verify" ? "VERIFY EMAIL" : "RESET PASSWORD"}
          </p>
          <h1>{content[mode].title}</h1>
          <p className="auth-copy">{content[mode].copy}</p>
          <form onSubmit={submit} noValidate>
            {(mode === "login" || mode === "forgot") && (
              <label>
                {mode === "login" ? "Email or username" : "Email address"}
                <input
                  name={mode === "login" ? "login" : "email"}
                  type={mode === "login" ? "text" : "email"}
                  autoComplete={mode === "login" ? "username" : "email"}
                  maxLength={MAX_EMAIL_LENGTH}
                  placeholder={mode === "login" ? "you@example.com or username" : "you@example.com"}
                  required
                  aria-invalid={Boolean(fieldErrors[mode === "login" ? "login" : "email"])}
                  aria-describedby={fieldErrors[mode === "login" ? "login" : "email"] ? `${mode === "login" ? "login" : "email"}-error` : undefined}
                />
                {renderFieldError(mode === "login" ? "login" : "email")}
              </label>
            )}

            {mode === "register" && (
              <>
                <label>
                  Username
                  <input name="username" type="text" autoComplete="username" minLength={3} maxLength={MAX_USERNAME_LENGTH} required aria-invalid={Boolean(fieldErrors.username)} aria-describedby={fieldErrors.username ? "username-error" : undefined} placeholder="alexmorgan" />
                  {renderFieldError("username")}
                </label>
                <label>
                  Email address
                  <input name="email" type="email" autoComplete="email" maxLength={MAX_EMAIL_LENGTH} required aria-invalid={Boolean(fieldErrors.email)} aria-describedby={fieldErrors.email ? "email-error" : undefined} placeholder="you@example.com" />
                  {renderFieldError("email")}
                </label>
                <label>
                  Password
                  <input name="password" type="password" autoComplete="new-password" minLength={8} maxLength={MAX_PASSWORD_LENGTH} required aria-invalid={Boolean(fieldErrors.password)} aria-describedby={fieldErrors.password ? "password-error" : undefined} placeholder="8 to 128 characters" />
                  {renderFieldError("password")}
                </label>
                <label>
                  First name <span className="field-optional">(optional)</span>
                  <input name="firstName" type="text" autoComplete="given-name" maxLength={MAX_NAME_LENGTH} aria-invalid={Boolean(fieldErrors.firstName)} aria-describedby={fieldErrors.firstName ? "firstName-error" : undefined} placeholder="Alex" />
                  {renderFieldError("firstName")}
                </label>
                <label>
                  Last name <span className="field-optional">(optional)</span>
                  <input name="lastName" type="text" autoComplete="family-name" maxLength={MAX_NAME_LENGTH} aria-invalid={Boolean(fieldErrors.lastName)} aria-describedby={fieldErrors.lastName ? "lastName-error" : undefined} placeholder="Morgan" />
                  {renderFieldError("lastName")}
                </label>
              </>
            )}

            {mode === "login" && (
              <label>
                Password
                <input name="password" type="password" autoComplete="current-password" minLength={8} maxLength={MAX_PASSWORD_LENGTH} required aria-invalid={Boolean(fieldErrors.password)} aria-describedby={fieldErrors.password ? "password-error" : undefined} placeholder="Your password" />
                {renderFieldError("password")}
              </label>
            )}

            {mode === "verify" && (
              <div className="code-inputs" aria-label="Six-digit verification code">
                {code.map((value, index) => (
                  <input
                    key={index}
                    aria-label={`Verification digit ${index + 1}`}
                    inputMode="numeric"
                    pattern="[0-9]"
                    maxLength={1}
                    value={value}
                    onChange={event => {
                      const digit = event.target.value.replace(/\D/g, "").slice(-1);
                      const next = [...code];
                      next[index] = digit;
                      setCode(next);
                      if (digit) {
                        const nextInput = event.currentTarget.nextElementSibling;
                        if (nextInput instanceof HTMLInputElement) nextInput.focus();
                      }
                    }}
                  />
                ))}
              </div>
            )}

            {error && <p className="form-error" role="alert">{error}</p>}
            {success && <p className="form-success" role="status">{success}</p>}
            <button type="submit" disabled={isLoading} className="btn-primary">
              {isLoading ? "Please wait..." : mode === "login" ? "Sign in" : mode === "register" ? "Create account" : mode === "verify" ? "Verify" : "Send link"}
            </button>
          </form>
          <div className="auth-switch">
            {mode === "login" && (
              <>
                <Link href="/forgot-password">Forgot password?</Link>
                <span>·</span>
                <Link href="/register">Create account</Link>
              </>
            )}
            {mode === "register" && <Link href="/login">Already have an account?</Link>}
          </div>
        </div>
      </section>
    </main>
  );
}
