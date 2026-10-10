import { expect, test } from "@playwright/test";

test.describe("authentication smoke flow", () => {
  test("login form uses the backend login contract", async ({ page }) => {
    await page.goto("/login");

    await expect(
      page.getByRole("heading", { name: "Welcome back." }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Sign in" }),
    ).toBeVisible();
    await expect(page.getByLabel("Email or username")).toBeVisible();
    await expect(page.getByLabel("Password")).toHaveAttribute("maxlength", "128");
  });

  test("registration validates every field and a valid account reaches chats", async ({ page }) => {
    const unique = `${Date.now()}-${test.info().workerIndex}`;
    const email = `ci-${unique}@example.com`;
    const username = `ci${unique}`.replace(/[^a-zA-Z0-9]/g, "").slice(0, 40);

    await page.goto("/register");

    await expect(page.getByLabel("Username")).toBeVisible();
    await expect(page.getByLabel("Email address")).toBeVisible();
    await expect(page.getByLabel("Password")).toBeVisible();
    await expect(page.getByLabel("First name (optional)")).toBeVisible();
    await expect(page.getByLabel("Last name (optional)")).toBeVisible();

    await page.getByLabel("Username").fill("ab");
    await page.getByLabel("Email address").fill("not-an-email");
    await page.getByLabel("Password").fill("short");
    await page.getByRole("button", { name: "Create account" }).click();

    await expect(page.getByText("Username must be between 3 and 50 characters.")).toBeVisible();
    await expect(page.getByText("Enter a valid email address.")).toBeVisible();
    await expect(page.getByText("Password must be between 8 and 128 characters.")).toBeVisible();

    await page.getByLabel("Username").fill("invalid username");
    await page.getByLabel("Email address").fill(email);
    await page.getByLabel("Password").fill("password123");
    await page.getByRole("button", { name: "Create account" }).click();
    await expect(page.getByText("Username may contain only letters, numbers, dots, underscores and hyphens")).toBeVisible();

    await page.getByLabel("Username").fill(username);
    await page.getByLabel("Email address").fill(email);
    await page.getByLabel("Password").fill("password123");
    await page.getByLabel("First name (optional)").fill("CI Test");
    await page.getByLabel("Last name (optional)").fill("User");

    await page.getByRole("button", { name: "Create account" }).click();
    await expect(page).toHaveURL(/\/chats$/);
  });
});
