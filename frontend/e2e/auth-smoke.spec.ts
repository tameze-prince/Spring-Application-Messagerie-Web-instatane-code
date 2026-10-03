import { expect, test } from "@playwright/test";

test.describe("authentication smoke flow", () => {
  test("login page renders", async ({ page }) => {
    await page.goto("/login");

    await expect(
      page.getByRole("heading", { name: "Welcome back." }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Sign in" }),
    ).toBeVisible();
  });

  test("a new account can register and reach chats", async ({ page }) => {
    const unique = `${Date.now()}-${test.info().workerIndex}`;
    const email = `ci-${unique}@example.com`;
    const username = `ci${unique}`.replace(/[^a-zA-Z0-9]/g, "").slice(0, 40);

    await page.goto("/register");

    await page.getByLabel("Your name").fill("CI Test User");
    await page.getByLabel("Username").fill(username);
    await page.getByLabel("Email address").fill(email);
    await page.getByLabel("Password").fill("password123");

    await page.getByRole("button", { name: "Create account" }).click();

    await expect(page).toHaveURL(/\/chats$/);
  });
});
