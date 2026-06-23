// For more info, see https://github.com/storybookjs/eslint-plugin-storybook#configuration-flat-config-format
import prettier from 'eslint-config-prettier';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import js from '@eslint/js';
import svelte from 'eslint-plugin-svelte';
import { defineConfig, includeIgnoreFile } from 'eslint/config';
import globals from 'globals';
import ts from 'typescript-eslint';

import noRawHexPlugin from './tools/eslint-plugin-no-raw-hex/index.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const gitignorePath = path.resolve(__dirname, '.gitignore');

export default defineConfig(
	includeIgnoreFile(gitignorePath),
	js.configs.recommended,
	ts.configs.recommended,
	svelte.configs.recommended,
	prettier,
	svelte.configs.prettier,
	{
		languageOptions: { globals: { ...globals.browser, ...globals.node } },
		rules: {
			// typescript-eslint strongly recommend that you do not use the no-undef lint rule on TypeScript projects.
			// see: https://typescript-eslint.io/troubleshooting/faqs/eslint/#i-get-errors-from-the-no-undef-rule-about-global-variables-not-being-defined-even-though-there-are-no-typescript-errors
			'no-undef': 'off'
		}
	},
	{
		files: ['**/*.svelte', '**/*.svelte.ts', '**/*.svelte.js'],
		languageOptions: {
			parserOptions: {
				projectService: true,
				extraFileExtensions: ['.svelte'],
				parser: ts.parser
			}
		}
	},
	{
		// theme-tokens/no-raw-hex equivalent: ban raw HEX color literals
		// (#xxx / #xxxxxx / #xxxxxxxx) in .svelte / .ts / .js files,
		// but allow them inside the theme package itself.
		files: ['**/*.svelte', '**/*.ts', '**/*.js'],
		ignores: ['src/lib/theme/**', 'tools/eslint-plugin-no-raw-hex/**'],
		plugins: {
			'no-raw-hex': noRawHexPlugin
		},
		rules: {
			'no-raw-hex/no-raw-hex': 'warn'
		}
	},
	{
		// Override or add rule settings here, such as:
		// 'svelte/button-has-type': 'error'
		rules: {}
	},
	{
		// Button.svelte is a generic UI primitive that accepts any href (including
		// external URLs). svelte/no-navigation-without-resolve only applies to
		// application routes, so exempt this primitive.
		files: ['src/lib/components/Button.svelte'],
		rules: {
			'svelte/no-navigation-without-resolve': 'off'
		}
	}
);
