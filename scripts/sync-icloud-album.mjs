import { chromium } from "playwright";
import fs from "node:fs/promises";
import path from "node:path";

const albumUrl = "https://photos.icloud.com/shared/album/0f3q_OWlNNey4KWellI67wgLw";
const outputDirectory = "screensaver";

await fs.mkdir(outputDirectory, { recursive: true });

const oldEntries = await fs.readdir(outputDirectory).catch(() => []);
for (const name of oldEntries) {
  if (/^icloud-\d+\.jpg$/.test(name)) {
    await fs.unlink(path.join(outputDirectory, name));
  }
}

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({
  locale: "de-AT",
  viewport: { width: 2400, height: 1600 },
  deviceScaleFactor: 1
});
await page.goto(albumUrl, { waitUntil: "domcontentloaded", timeout: 90000 });
await page.waitForFunction(
  () => document.querySelectorAll('img[src^="blob:"]').length > 0,
  { timeout: 90000 }
);

// iCloud lädt weitere Albumkacheln verzögert und teilweise erst beim Scrollen.
for (let pass = 0; pass < 4; pass++) {
  await page.mouse.wheel(0, 1400);
  await page.waitForTimeout(2000);
}
await page.waitForTimeout(5000);

const imageLocators = page.locator('img[src^="blob:"]');
const imageCount = await imageLocators.count();
const imageUrls = [];
for (let index = 0; index < imageCount; index++) {
  const fileName = `icloud-${index + 1}.jpg`;
  await imageLocators.nth(index).screenshot({
    path: path.join(outputDirectory, fileName),
    type: "jpeg",
    quality: 88
  });
  imageUrls.push(
    `https://raw.githubusercontent.com/dennisprantner17-hub/DennisOS/main/screensaver/${fileName}`
  );
}

await browser.close();

await fs.writeFile(
  "screensaver.json",
  JSON.stringify({ album: albumUrl, images: imageUrls }, null, 2) + "\n"
);
