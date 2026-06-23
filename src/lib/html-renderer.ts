/**
 * DOMPurify ラッパー: 公開データから取得した HTML をサニタイズ
 */

import DOMPurify from 'dompurify';
import type { Config } from 'dompurify';

const CONFIG: Config = {
	ALLOWED_TAGS: [
		'a',
		'b',
		'blockquote',
		'br',
		'code',
		'div',
		'em',
		'h1',
		'h2',
		'h3',
		'h4',
		'h5',
		'h6',
		'hr',
		'i',
		'img',
		'li',
		'ol',
		'p',
		'pre',
		'span',
		'strong',
		'table',
		'tbody',
		'td',
		'th',
		'thead',
		'tr',
		'ul'
	],
	ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class', 'target', 'rel'],
	ALLOW_DATA_ATTR: false
};

/**
 * 信頼できない HTML をサニタイズして信頼できる文字列に
 */
export function sanitize(html: string): string {
	return DOMPurify.sanitize(html, CONFIG) as unknown as string;
}

/**
 * URL を新規タブで開く安全設定に
 */
export function safeTarget(html: string): string {
	return html.replace(/<a\s+([^>]*?)>/g, (match, attrs) => {
		// target=_blank が無ければ追加
		if (!/target=/.test(attrs)) {
			return `<a ${attrs} target="_blank" rel="noopener noreferrer">`;
		}
		// rel=noopener が無ければ追加
		if (!/rel=/.test(attrs)) {
			return `<a ${attrs} rel="noopener noreferrer">`;
		}
		return match;
	});
}
