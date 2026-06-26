const fs = require('fs');
const path = require('path');

const configPath = path.join(__dirname, '../project.config.json');
if (!fs.existsSync(configPath)) {
	console.error('project.config.json not found!');
	process.exit(1);
}
const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));

// 1. Generate constants.ts
const constantsContent = `export const PUBLIC_APP_NAME = '${config.displayName}';
export const STORAGE_KEY_PREFIX = '${config.name}:';
export const SOURCE_URL = '${config.sourceUrl}';
export const PUBLIC_DATA_CDN_BASE = 'https://cdn.jsdelivr.net/gh/ueckoken/${config.name}-data@main';
`;
fs.writeFileSync(path.join(__dirname, '../src/lib/constants.ts'), constantsContent);

// 2. Replace capacitor.config.ts
const capPath = path.join(__dirname, '../capacitor.config.ts');
if (fs.existsSync(capPath)) {
	let capContent = fs.readFileSync(capPath, 'utf8');
	capContent = capContent.replace(/appId:\s*['"`].*?['"`]/, `appId: '${config.appId}'`);
	capContent = capContent.replace(/appName:\s*['"`].*?['"`]/, `appName: '${config.displayName}'`);
	fs.writeFileSync(capPath, capContent);
}

// 3. Info.plist
const plistPath = path.join(__dirname, '../ios/App/App/Info.plist');
if (fs.existsSync(plistPath)) {
	let plistContent = fs.readFileSync(plistPath, 'utf8');
	plistContent = plistContent.replace(
		/<key>CFBundleDisplayName<\/key>\s*<string>.*?<\/string>/,
		`<key>CFBundleDisplayName</key>\n        <string>${config.displayName}</string>`
	);
	fs.writeFileSync(plistPath, plistContent);
}

// 4. project.pbxproj
const pbxPath = path.join(__dirname, '../ios/App/App.xcodeproj/project.pbxproj');
if (fs.existsSync(pbxPath)) {
	let pbxContent = fs.readFileSync(pbxPath, 'utf8');
	pbxContent = pbxContent.replace(
		/PRODUCT_BUNDLE_IDENTIFIER = .*?;/g,
		`PRODUCT_BUNDLE_IDENTIFIER = ${config.appId};`
	);
	fs.writeFileSync(pbxPath, pbxContent);
}

// 5. manifest.webmanifest
const manifestPath = path.join(__dirname, '../static/manifest.webmanifest');
if (fs.existsSync(manifestPath)) {
	let manifestContent = fs.readFileSync(manifestPath, 'utf8');
	manifestContent = manifestContent.replace(/"name":\s*".*?"/, `"name": "${config.displayName}"`);
	manifestContent = manifestContent.replace(
		/"short_name":\s*".*?"/,
		`"short_name": "${config.displayName}"`
	);
	fs.writeFileSync(manifestPath, manifestContent);
}

// 6. android build.gradle
const gradlePath = path.join(__dirname, '../android/app/build.gradle');
if (fs.existsSync(gradlePath)) {
	let gradleContent = fs.readFileSync(gradlePath, 'utf8');
	gradleContent = gradleContent.replace(
		/namespace\s*=\s*['"`].*?['"`]/,
		`namespace = "${config.appId}"`
	);
	gradleContent = gradleContent.replace(
		/applicationId\s*['"`].*?['"`]/,
		`applicationId "${config.appId}"`
	);
	fs.writeFileSync(gradlePath, gradleContent);
}

// 7. strings.xml
const stringsPath = path.join(__dirname, '../android/app/src/main/res/values/strings.xml');
if (fs.existsSync(stringsPath)) {
	let stringsContent = fs.readFileSync(stringsPath, 'utf8');
	stringsContent = stringsContent.replace(
		/<string name="app_name">.*?<\/string>/,
		`<string name="app_name">${config.displayName}</string>`
	);
	stringsContent = stringsContent.replace(
		/<string name="title_activity_main">.*?<\/string>/,
		`<string name="title_activity_main">${config.displayName}</string>`
	);
	stringsContent = stringsContent.replace(
		/<string name="package_name">.*?<\/string>/,
		`<string name="package_name">${config.appId}</string>`
	);
	stringsContent = stringsContent.replace(
		/<string name="custom_url_scheme">.*?<\/string>/,
		`<string name="custom_url_scheme">${config.appId}</string>`
	);
	fs.writeFileSync(stringsPath, stringsContent);
}

// 8. deploy.yml
const deployPath = path.join(__dirname, '../.github/workflows/deploy.yml');
if (fs.existsSync(deployPath)) {
	let deployContent = fs.readFileSync(deployPath, 'utf8');
	deployContent = deployContent.replace(/>\s*open-.*?.jks/g, `> ${config.name}.jks`);
	deployContent = deployContent.replace(/--ks\s*open-.*?.jks/g, `--ks ${config.name}.jks`);
	deployContent = deployContent.replace(
		/name:\s*open-.*?-android-release-apk/g,
		`name: ${config.name}-android-release-apk`
	);
	deployContent = deployContent.replace(
		/name:\s*open-.*?-ios-archive/g,
		`name: ${config.name}-ios-archive`
	);
	fs.writeFileSync(deployPath, deployContent);
}

// 9. git config
const gitConfigPath = path.join(__dirname, '../.git/config');
if (fs.existsSync(gitConfigPath)) {
	let gitConfig = fs.readFileSync(gitConfigPath, 'utf8');
	gitConfig = gitConfig.replace(/name = .*?\n/, `name = ${config.displayName}\n`);
	gitConfig = gitConfig.replace(/email = .*?\n/, `email = ${config.email}\n`);
	gitConfig = gitConfig.replace(/github\.com\/.*?\/.*?-app\.git/, `github.com/${config.githubRepo}.git`);
	fs.writeFileSync(gitConfigPath, gitConfig);
}

// 10. app.html
const htmlPath = path.join(__dirname, '../src/app.html');
if (fs.existsSync(htmlPath)) {
	let htmlContent = fs.readFileSync(htmlPath, 'utf8');
	htmlContent = htmlContent.replace(
		/<meta property="og:title" content=".*?" \/>/,
		`<meta property="og:title" content="${config.displayName}" />`
	);
	htmlContent = htmlContent.replace(
		/<meta property="og:description" content=".*?" \/>/,
		`<meta property="og:description" content="${config.displayName}" />`
	);
	htmlContent = htmlContent.replace(
		/<meta property="og:url" content=".*?" \/>/,
		`<meta property="og:url" content="https://${config.name}.app/" />`
	);
	htmlContent = htmlContent.replace(
		/localStorage\.getItem\('.*?:user-settings'\)/,
		`localStorage.getItem('${config.name}:user-settings')`
	);
	fs.writeFileSync(htmlPath, htmlContent);
}

// 11. Android MainActivity.java package path dynamic updates
const javaBaseDir = path.join(__dirname, '../android/app/src/main/java');
if (fs.existsSync(javaBaseDir)) {
	function findFile(dir, fileName) {
		const files = fs.readdirSync(dir);
		for (const file of files) {
			const fullPath = path.join(dir, file);
			if (fs.statSync(fullPath).isDirectory()) {
				const found = findFile(fullPath, fileName);
				if (found) return found;
			} else if (file === fileName) {
				return fullPath;
			}
		}
		return null;
	}
	const mainActivityPath = findFile(javaBaseDir, 'MainActivity.java');
	if (mainActivityPath) {
		let activityContent = fs.readFileSync(mainActivityPath, 'utf8');
		activityContent = activityContent.replace(/package\s+.*?;/, `package ${config.appId};`);

		const packageParts = config.appId.split('.');
		const destDir = path.join(javaBaseDir, ...packageParts);
		const destFile = path.join(destDir, 'MainActivity.java');

		if (mainActivityPath !== destFile) {
			fs.mkdirSync(destDir, { recursive: true });
			fs.writeFileSync(destFile, activityContent);
			fs.unlinkSync(mainActivityPath);

			let currentDir = path.dirname(mainActivityPath);
			while (currentDir !== javaBaseDir) {
				if (fs.readdirSync(currentDir).length === 0) {
					fs.rmdirSync(currentDir);
					currentDir = path.dirname(currentDir);
				} else {
					break;
				}
			}
		} else {
			fs.writeFileSync(mainActivityPath, activityContent);
		}
	}
}

// 12. Update appVersion in pages to the current date
const today = new Date();
const yyyy = today.getFullYear();
const mm = String(today.getMonth() + 1).padStart(2, '0');
const dd = String(today.getDate()).padStart(2, '0');
const dateVersion = `${yyyy}.${mm}.${dd}`;

const morePagePath = path.join(__dirname, '../src/routes/more/+page.svelte');
if (fs.existsSync(morePagePath)) {
	let content = fs.readFileSync(morePagePath, 'utf8');
	content = content.replace(/const appVersion = '.*?';/, `const appVersion = '${dateVersion}';`);
	fs.writeFileSync(morePagePath, content);
}

const settingsPagePath = path.join(__dirname, '../src/routes/settings/+page.svelte');
if (fs.existsSync(settingsPagePath)) {
	let content = fs.readFileSync(settingsPagePath, 'utf8');
	content = content.replace(/const appVersion = '.*?';/, `const appVersion = '${dateVersion}';`);
	fs.writeFileSync(settingsPagePath, content);
}

console.log('Project configuration names synchronized successfully.');
