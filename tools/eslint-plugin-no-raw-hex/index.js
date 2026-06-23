// Local ESLint plugin: no-raw-hex
// Bans raw HEX color literals (#xxx, #xxxxxx, #xxxxxxxx) outside of src/lib/theme/**.
//
// Why a local plugin?
//   - `no-restricted-syntax` cannot easily catch:
//       * HEX inside JS string literals (e.g. `color: '#ff0000'`)
//       * HEX inside Svelte template attribute values (e.g. style="color: #ff0000",
//         class="bg-[#ff0000]") where the AST node type is SvelteAttribute
//     so a tiny custom rule is the simplest reliable approach.
//
// Scope: .svelte, .ts, .js files. The eslint.config.js applies it with an ignore
// for `src/lib/theme/**` so theme definitions can keep their HEX literals.

import rule from './rules/no-raw-hex.js';

const plugin = {
	meta: {
		name: 'eslint-plugin-no-raw-hex',
		version: '1.0.0'
	},
	rules: {
		'no-raw-hex': rule
	}
};

export default plugin;
