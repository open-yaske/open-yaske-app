/**
 * Markdown レンダラ (marked + DOMPurify)
 * 公開データの説明文 (Markdown) を HTML にしてサニタイズする
 */

import { marked } from 'marked';
import { sanitize, safeTarget } from './html-renderer';

marked.setOptions({
	gfm: true,
	breaks: true
});

/**
 * Markdown → サニタイズ済み HTML
 */
export function renderMarkdown(md: string): string {
	const html = marked.parse(md, { async: false }) as string;
	return safeTarget(sanitize(html));
}
