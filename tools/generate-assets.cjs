const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const iconSource = path.join(__dirname, '../app-icon.png');
if (!fs.existsSync(iconSource)) {
	console.error('Source app-icon.png not found in the root directory!');
	process.exit(1);
}

// 1. Copy source icon to assets/
const assetsDir = path.join(__dirname, '../assets');
fs.mkdirSync(assetsDir, { recursive: true });
fs.copyFileSync(iconSource, path.join(assetsDir, 'icon.png'));
fs.copyFileSync(iconSource, path.join(assetsDir, 'icon-only.png'));
fs.copyFileSync(iconSource, path.join(assetsDir, 'logo.png'));

// 2. Generate Capacitor Assets
console.log('Generating Capacitor mobile assets...');
try {
	execSync('npx capacitor-assets generate --ios --android', { stdio: 'inherit' });
} catch (e) {
	console.warn(
		'Failed to generate iOS/Android Capacitor assets. Native platforms might not be fully configured yet.'
	);
}

// 3. Generate Web/Favicons using ImageMagick convert
console.log('Generating Web and Favicon assets...');
const convertCmds = [
	`convert "${iconSource}" -resize 16x16 "${path.join(__dirname, '../static/favicon-16.png')}"`,
	`convert "${iconSource}" -resize 32x32 "${path.join(__dirname, '../static/favicon-32.png')}"`,
	`convert "${iconSource}" -resize 32x32 "${path.join(__dirname, '../static/favicon.png')}"`,
	`convert "${iconSource}" -resize 180x180 "${path.join(__dirname, '../static/apple-touch-icon.png')}"`,
	`convert "${iconSource}" -resize 192x192 "${path.join(__dirname, '../static/icon-192.png')}"`,
	`convert "${iconSource}" -resize 512x512 "${path.join(__dirname, '../static/icon-512.png')}"`,
	`convert "${iconSource}" -resize 1254x1254 "${path.join(__dirname, '../static/ogp.png')}"`
];

for (const cmd of convertCmds) {
	try {
		execSync(cmd);
	} catch (e) {
		console.error(`Failed to run command: ${cmd}`);
	}
}

// 4. Generate src/lib/assets/favicon.svg
const base64Data = fs.readFileSync(iconSource).toString('base64');
const svgContent = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512"><image href="data:image/png;base64,${base64Data}" x="0" y="0" width="512" height="512" /></svg>`;
fs.writeFileSync(path.join(__dirname, '../src/lib/assets/favicon.svg'), svgContent);

console.log('All Web and Mobile assets generated successfully from app-icon.png.');
