import { readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const production = process.argv.includes('--production');
const templatePath = resolve(root, 'src/environments/environment.prod.ts');
const outputPath = resolve(root, 'src/environments/environment.generated.ts');
const developmentApiUrl = 'http://localhost:8080/api/v1';
const apiUrl = production ? process.env.API_URL?.trim() : developmentApiUrl;

if (!apiUrl) {
  console.error('API_URL is required for the production frontend build.');
  process.exit(1);
}

let parsedUrl;
try {
  parsedUrl = new URL(apiUrl);
} catch {
  console.error('API_URL must be an absolute HTTP or HTTPS URL.');
  process.exit(1);
}

if (!['http:', 'https:'].includes(parsedUrl.protocol)) {
  console.error('API_URL must use HTTP or HTTPS.');
  process.exit(1);
}

if (production && parsedUrl.protocol !== 'https:') {
  console.error('API_URL must use HTTPS for production builds.');
  process.exit(1);
}

const template = await readFile(templatePath, 'utf8');
const generated = template.replace('__API_URL__', apiUrl.replace(/\/+$/, ''));
await writeFile(outputPath, generated, 'utf8');
console.log(`Generated production environment for ${parsedUrl.origin}.`);
