import { chromium } from "playwright";
import fs from "node:fs/promises";
import path from "node:path";

const albumUrl = "https://photos.icloud.com/shared/album/0f3q_OWlNNey4KWellI67wgLw";
const outputDirectory = "screensaver";

await fs.mkdir(outputDirectory, { recursive: true });

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ locale: "de-AT" });
await page.goto(albumUrl, { waitUntil: "domcontentloaded", timeout: 90000 });
await page.waitForFunction(
  () => document.querySelectorAll('img[src^="blob:"]').length > 0,
  { timeout: 90000 }
);

const encodedImages = await page.locator('img[src^="blob:"]').evaluateAll(
  async (images) => Promise.all(images.map(async (image) => {
    const response = await fetch(image.src);
    const bytes = new Uint8Array(await response.arrayBuffer());
    let binary = "";
    const chunkSize = 8192;
    for (let offset = 0; offset < bytes.length; offset += chunkSize) {
      binary += String.fromCharCode(...bytes.subarray(offset, offset + chunkSize));
    }
    return btoa(binary);
  }))
);

await browser.close();

const oldEntries = await fs.readdir(outputDirectory).catch(() => []);
for (const name of oldEntries) {
  if (/^icloud-\d+\.jpg$/.test(name)) {
    await fs.unlink(path.join(outputDirectory, name));
  }
}

const imageUrls = [];
for (let index = 0; index < encodedImages.length; index++) {
  const fileName = `icloud-${index + 1}.jpg`;
  await fs.writeFile(
    path.join(outputDirectory, fileName),
    Buffer.from(encodedImages[index], "base64")
  );
  imageUrls.push(
    `https://raw.githubusercontent.com/dennisprantner17-hub/DennisOS/main/screensaver/${fileName}`
  );
}

await fs.writeFile(
  "screensaver.json",
  JSON.stringify({ album: albumUrl, images: imageUrls }, null, 2) + "\n"
);
