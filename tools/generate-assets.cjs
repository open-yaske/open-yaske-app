const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const iconSource = path.join(__dirname, '../app-icon.png');
if (!fs.existsSync(iconSource)) {
	console.error('Source app-icon.png not found in the root directory!');
	process.exit(1);
}

const assetsDir = path.join(__dirname, '../assets');
fs.mkdirSync(assetsDir, { recursive: true });

// ============================================================
// Step 0: Determine dominant background color from the icon
// ============================================================
console.log('Determining icon background color...');
const bgColorResult = execSync(
	`convert "${iconSource}" -crop 100x100+206+206 +repage -scale 1x1! -format '%[pixel:u.p{0,0}]' info:-`,
	{ encoding: 'utf8' }
).trim();
// Extract hex color from result like "srgb(245,241,237)" or "#F5F1ED"
let bgColor = '#F5F1ED'; // default fallback
const srgbMatch = bgColorResult.match(/\((\d+),(\d+),(\d+)\)/);
if (srgbMatch) {
	const r = parseInt(srgbMatch[1]).toString(16).padStart(2, '0').toUpperCase();
	const g = parseInt(srgbMatch[2]).toString(16).padStart(2, '0').toUpperCase();
	const b = parseInt(srgbMatch[3]).toString(16).padStart(2, '0').toUpperCase();
	bgColor = `#${r}${g}${b}`;
} else if (bgColorResult.startsWith('#')) {
	bgColor = bgColorResult;
}
console.log(`  Detected background color: ${bgColor}`);

// ============================================================
// Step 1: Scale source to native-friendly resolution (1024x1024)
// ============================================================
console.log('Scaling source icon for native platforms...');
const iconNative = path.join(assetsDir, 'icon-native.png');
execSync(`convert "${iconSource}" -resize 1024x1024 "${iconNative}"`);

// ============================================================
// Step 2: Copy to assets/ (for @capacitor/assets consumption)
// ============================================================
console.log('Copying icons to assets/...');
fs.copyFileSync(iconNative, path.join(assetsDir, 'icon.png'));
fs.copyFileSync(iconNative, path.join(assetsDir, 'icon-only.png'));

// Generate icon-background.png: solid background color at 1254x1254
// (required by @capacitor/assets for Android adaptive icon background)
console.log('  Generating icon-background.png (solid color layer)...');
execSync(
	`convert -size 1254x1254 xc:"${bgColor}" "${path.join(assetsDir, 'icon-background.png')}"`
);

// Generate icon-foreground.png: icon with background removed (transparent)
// (required by @capacitor/assets for Android adaptive icon foreground)
console.log('  Generating icon-foreground.png (foreground layer)...');
execSync(
	`convert "${iconSource}" -resize 1254x1254 -fuzz 10% -transparent "${bgColor}" "${path.join(assetsDir, 'icon-foreground.png')}"`
);

// Generate splash.png and splash-dark.png from the transparent-bg foreground
console.log('  Generating splash screens...');
execSync(
	`convert -size 2732x2732 xc:"${bgColor}" \\( "${path.join(assetsDir, 'icon-foreground.png')}" -resize 1024x1024 -gravity center \\) -composite "${path.join(assetsDir, 'splash.png')}"`
);
const darkBg = '#1a1a2e';
execSync(
	`convert -size 2732x2732 xc:"${darkBg}" \\( "${path.join(assetsDir, 'icon-foreground.png')}" -resize 1024x1024 -gravity center \\) -composite "${path.join(assetsDir, 'splash-dark.png')}"`
);

// ============================================================
// Step 3: Generate Capacitor Assets (iOS + Android)
// ============================================================
console.log('Generating Capacitor mobile assets...');
try {
	execSync('npx capacitor-assets generate --ios --android', { stdio: 'inherit' });
} catch (e) {
	console.warn(
		'Failed to generate iOS/Android Capacitor assets. Native platforms might not be fully configured yet.'
	);
}

// ============================================================
// Step 4: Generate Web/Favicons using ImageMagick convert
// ============================================================
console.log('Generating Web and Favicon assets...');
const staticDir = path.join(__dirname, '../static');
const convertCmds = [
	`convert "${iconSource}" -resize 16x16 "${path.join(staticDir, 'favicon-16.png')}"`,
	`convert "${iconSource}" -resize 32x32 "${path.join(staticDir, 'favicon-32.png')}"`,
	`convert "${iconSource}" -resize 32x32 "${path.join(staticDir, 'favicon.png')}"`,
	`convert "${iconSource}" -resize 180x180 "${path.join(staticDir, 'apple-touch-icon.png')}"`,
	`convert "${iconSource}" -resize 192x192 "${path.join(staticDir, 'icon-192.png')}"`,
	`convert "${iconSource}" -resize 512x512 "${path.join(staticDir, 'icon-512.png')}"`,
	`convert "${iconSource}" -resize 1254x1254 "${path.join(staticDir, 'ogp.png')}"`
];

for (const cmd of convertCmds) {
	try {
		execSync(cmd);
	} catch (e) {
		console.error(`Failed to run command: ${cmd}`);
	}
}

// ============================================================
// Step 5: Generate src/lib/assets/favicon.svg
// ============================================================
const base64Data = fs.readFileSync(iconSource).toString('base64');
const svgContent = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512"><image href="data:image/png;base64,${base64Data}" x="0" y="0" width="512" height="512" /></svg>`;
fs.writeFileSync(path.join(__dirname, '../src/lib/assets/favicon.svg'), svgContent);

// ============================================================
// Step 6: Cleanup temp files
// ============================================================
try {
	fs.unlinkSync(iconNative);
} catch (e) {
	/* ignore */
}

console.log('All Web and Mobile assets generated successfully from app-icon.png.');
