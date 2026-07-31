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

const albumText = await page.locator("body").innerText();
const albumCount = Math.max(
  75,
  Number(albumText.match(/(\d+)\s+(?:Elemente?|Items?)/i)?.[1] || 0)
);
const gridImages = page.locator('img[src^="blob:"]');
await gridImages.first().click({ force: true });
await page.waitForTimeout(3000);

const imageUrls = [];
for (let index = 0; index < albumCount; index++) {
  const fileName = `icloud-${index + 1}.jpg`;
  const candidates = page.locator('img[src^="blob:"]');
  let largest = candidates.first();
  let largestArea = 0;
  for (let candidateIndex = 0; candidateIndex < await candidates.count(); candidateIndex++) {
    const candidate = candidates.nth(candidateIndex);
    if (!(await candidate.isVisible())) continue;
    const box = await candidate.boundingBox();
    const area = box ? box.width * box.height : 0;
    if (area > largestArea) {
      largest = candidate;
      largestArea = area;
    }
  }
  await largest.screenshot({
    path: path.join(outputDirectory, fileName),
    type: "jpeg",
    quality: 88
  });
  imageUrls.push(
    `https://raw.githubusercontent.com/dennisprantner17-hub/DennisOS/main/screensaver/${fileName}`
  );
  await page.keyboard.press("ArrowRight");
  await page.waitForTimeout(2500);
}

await browser.close();

await fs.writeFile(
  "screensaver.json",
  JSON.stringify({ album: albumUrl, images: imageUrls }, null, 2) + "\n"
);
