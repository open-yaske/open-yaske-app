// Rule: no-raw-hex
// Bans raw HEX color literals like #fff, #ffffff, #ffffffff in:
//   - JS/TS string literals (Literal with string value)
//   - JSX/Svelte template attribute string values (JSXAttribute, SvelteAttribute)
//   - Template literal raw segments
//
// Use theme CSS variables (var(--color-primary-500)) or Tailwind tokens instead.

const HEX_RE = /#(?:[0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})\b/g;

const rule = {
	meta: {
		type: 'problem',
		docs: {
			description: 'Disallow raw HEX color literals (e.g. #fff, #ffffff) outside src/lib/theme/**',
			recommended: false
		},
		schema: [],
		messages: {
			noRawHex:
				'Raw HEX color literal "{{value}}" is forbidden outside src/lib/theme/**. Use a theme CSS variable (e.g. var(--color-primary-500)) or a Tailwind token instead.'
		}
	},

	create(context) {
		const filename = context.filename ?? context.getFilename?.() ?? '';

		function scanString(value, node) {
			if (typeof value !== 'string' || value.length === 0 || !value.includes('#')) return;
			HEX_RE.lastIndex = 0;
			const seen = new Set();
			let m;
			while ((m = HEX_RE.exec(value)) !== null) {
				const matched = m[0];
				const key = matched.toLowerCase();
				if (seen.has(key)) continue;
				seen.add(key);
				context.report({
					node,
					messageId: 'noRawHex',
					data: { value: matched }
				});
			}
		}

		const baseVisitor = {
			Literal(node) {
				if (typeof node.value === 'string') scanString(node.value, node);
			},
			TemplateElement(node) {
				if (node.value && typeof node.value.raw === 'string') {
					scanString(node.value.raw, node);
				}
			},
			JSXAttribute(node) {
				const v = node.value;
				if (v && v.type === 'Literal' && typeof v.value === 'string') {
					scanString(v.value, v);
				}
			}
		};

		// Svelte template attribute strings are exposed as SvelteAttribute nodes
		// by svelte-eslint-parser. Only scan style/class/className attribute values
		// from the template (the <script> block strings are already covered by the
		// baseVisitor above via the standard JS AST walk).
		if (filename.endsWith('.svelte')) {
			return {
				...baseVisitor,
				SvelteAttribute(node) {
					const attrName = node.key?.name ?? node.name;
					if (!attrName) return;
					if (!['style', 'class', 'className'].includes(attrName)) return;
					const values = node.value ?? [];
					for (const v of values) {
						const raw =
							typeof v.value === 'string' ? v.value : typeof v.raw === 'string' ? v.raw : null;
						if (raw) scanString(raw, v);
					}
				}
			};
		}

		return baseVisitor;
	}
};

export default rule;
