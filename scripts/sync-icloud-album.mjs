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
  deviceScaleFactor: 3
});
await page.goto(albumUrl, { waitUntil: "domcontentloaded", timeout: 90000 });
await page.waitForFunction(
  () => document.querySelectorAll('img[src^="blob:"]').length > 0,
  { timeout: 90000 }
);

const imageUrls = [];
const seenImageSources = new Set();
let unchangedPasses = 0;
while (seenImageSources.size < 500 && unchangedPasses < 4) {
  const beforePass = seenImageSources.size;
  const candidates = page.locator('img[src^="blob:"]');
  for (let candidateIndex = 0; candidateIndex < await candidates.count(); candidateIndex++) {
    const candidate = candidates.nth(candidateIndex);
    if (!(await candidate.isVisible())) continue;
    const imageSource = await candidate.getAttribute("src");
    if (!imageSource || seenImageSources.has(imageSource)) continue;
    seenImageSources.add(imageSource);
    const fileName = `icloud-${seenImageSources.size}.jpg`;
    await candidate.screenshot({
      path: path.join(outputDirectory, fileName),
      type: "jpeg",
      quality: 95
    });
    imageUrls.push(
      `https://raw.githubusercontent.com/dennisprantner17-hub/DennisOS/main/screensaver/${fileName}`
    );
  }
  unchangedPasses = seenImageSources.size == beforePass
    ? unchangedPasses + 1
    : 0;
  await page.mouse.wheel(0, 1200);
  await page.waitForTimeout(2000);
}

await browser.close();

await fs.writeFile(
  "screensaver.json",
  JSON.stringify({ album: albumUrl, images: imageUrls }, null, 2) + "\n"
);
