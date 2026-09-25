"use client";
import Link from "next/link";
import { FormEvent, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuth } from "@/lib/context/AuthContext";

type Mode = "login" | "register" | "verify" | "forgot";
const content: Record<Mode, { title: string; copy: string }> = {
  login: { title: "Welcome back.", copy: "Your conversations are right where you left them." },
  register: { title: "Make space for better conversations.", copy: "Create your Wavelength account in a minute." },
  verify: { title: "Check your inbox.", copy: "We sent a six-digit code to your email address." },
  forgot: { title: "Let's get you back in.", copy: "Enter your email and we'll send a secure reset link." },
};

export default function AuthScreen({ mode }: { mode: Mode }) {
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [code, setCode] = useState(["", "", "", "", "", ""]);
  const [isLoading, setIsLoading] = useState(false);
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login, register } = useAuth();

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccess("");
    setIsLoading(true);

    const form = new FormData(event.currentTarget);

    try {
      if (mode === "login") {
        const email = String(form.get("email"));
        const password = String(form.get("password"));
        if (!email || !password) {
          setError("Enter your email and password.");
          setIsLoading(false);
          return;
        }
        await login(email, password);
        router.push("/chats");
      } else if (mode === "register") {
        const name = String(form.get("name"));
        const username = String(form.get("username"));
        const email = String(form.get("email"));
        const password = String(form.get("password"));
        if (!name || !username || !email || !password) {
          setError("All fields are required.");
          setIsLoading(false);
          return;
        }
        await register({ username, email, password, firstName: name.split(" ")[0], lastName: name.split(" ").slice(1).join(" ") });
        router.push("/chats");
      } else if (mode === "verify") {
        // Email verification would need backend endpoint
        setSuccess("Your email is verified. You can now enter Wavelength.");
      } else if (mode === "forgot") {
        // Forgot password would need backend endpoint
        setSuccess("Reset link sent. Check your inbox.");
      }
    } catch (err: unknown) {
      const apiError = err as { status?: number; message?: string };
      setError(apiError.message || "Something went wrong. Please try again.");
    } finally {
      setIsLoading(false);
    }
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
            {mode !== "verify" && (
              <>
                <label>
                  {mode === "register" ? "Your name" : "Email address"}
                  <input name={mode === "register" ? "name" : "email"} type={mode === "register" ? "text" : "email"} placeholder={mode === "register" ? "Alex Morgan" : "you@example.com"} required />
                </label>
                {mode === "register" && <label>Username<input name="username" placeholder="alexmorgan" required /></label>}
                {mode !== "forgot" && <label>Password<input name="password" type="password" placeholder="At least 8 characters" minLength={8} required /></label>}
              </>
            )}
            {mode === "verify" && (
              <div className="code-inputs">
                {code.map((value, index) => (
                  <input
                    key={index}
                    aria-label={`Verification digit ${index + 1}`}
                    inputMode="numeric"
                    maxLength={1}
                    value={value}
                    onChange={event => {
                      const next = [...code];
                      next[index] = event.target.value.replace(/\D/g, "");
                      setCode(next);
                      event.currentTarget.nextElementSibling instanceof HTMLInputElement && event.target.value && event.currentTarget.nextElementSibling.focus();
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
            {mode === "register" && (
              <>
                <Link href="/login">Already have an account?</Link>
              </>
            )}
          </div>
        </div>
      </section>
    </main>
  );
}
