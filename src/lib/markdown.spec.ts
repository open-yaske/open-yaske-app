import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockSanitize = vi.fn((html: string) => `SANITIZED:${html}`);
const mockSafeTarget = vi.fn((html: string) => `SAFE:${html}`);
const mockParse = vi.fn((md: string) => `<p>${md}</p>`);

vi.mock('dompurify', () => ({
	default: {
		sanitize: mockSanitize
	}
}));

vi.mock('./html-renderer', () => ({
	sanitize: mockSanitize,
	safeTarget: mockSafeTarget
}));

vi.mock('marked', () => ({
	marked: {
		setOptions: vi.fn(),
		parse: mockParse
	}
}));

describe('markdown', () => {
	beforeEach(() => {
		mockSanitize.mockClear();
		mockSafeTarget.mockClear();
		mockParse.mockClear();
	});

	it('renders markdown to HTML using marked', async () => {
		mockParse.mockReturnValueOnce('<h1>Title</h1>');
		mockSanitize.mockReturnValueOnce('<h1>Title</h1>');
		mockSafeTarget.mockReturnValueOnce('<h1>Title</h1>');

		const { renderMarkdown } = await import('./markdown');
		const result = renderMarkdown('# Title');
		expect(mockParse).toHaveBeenCalledWith('# Title', { async: false });
		expect(result).toBe('<h1>Title</h1>');
	});

	it('sanitizes rendered HTML', async () => {
		mockParse.mockReturnValueOnce('<p>Hello</p>');
		mockSanitize.mockReturnValueOnce('<p>Hello</p>');
		mockSafeTarget.mockReturnValueOnce('<p>Hello</p>');

		const { renderMarkdown } = await import('./markdown');
		renderMarkdown('Hello');
		expect(mockSanitize).toHaveBeenCalledWith('<p>Hello</p>');
	});

	it('applies safeTarget to sanitized HTML', async () => {
		mockParse.mockReturnValueOnce('<a href="https://example.com">Link</a>');
		mockSanitize.mockReturnValueOnce('<a href="https://example.com">Link</a>');
		mockSafeTarget.mockReturnValueOnce('<a href="https://example.com" target="_blank">Link</a>');

		const { renderMarkdown } = await import('./markdown');
		const result = renderMarkdown('[Link](https://example.com)');
		expect(mockSafeTarget).toHaveBeenCalledWith('<a href="https://example.com">Link</a>');
		expect(result).toBe('<a href="https://example.com" target="_blank">Link</a>');
	});

	it('handles empty markdown string', async () => {
		mockParse.mockReturnValueOnce('');
		mockSanitize.mockReturnValueOnce('');
		mockSafeTarget.mockReturnValueOnce('');

		const { renderMarkdown } = await import('./markdown');
		const result = renderMarkdown('');
		expect(mockParse).toHaveBeenCalledWith('', { async: false });
		expect(result).toBe('');
	});

	it('processes complex markdown with multiple elements', async () => {
		const md = '# Heading\n\n- item 1\n- item 2\n\n[link](https://example.com)';
		mockParse.mockReturnValueOnce(
			'<h1>Heading</h1><ul><li>item 1</li><li>item 2</li></ul><a href="https://example.com">link</a>'
		);
		mockSanitize.mockReturnValueOnce(
			'<h1>Heading</h1><ul><li>item 1</li><li>item 2</li></ul><a href="https://example.com">link</a>'
		);
		mockSafeTarget.mockReturnValueOnce(
			'<h1>Heading</h1><ul><li>item 1</li><li>item 2</li></ul><a href="https://example.com" target="_blank">link</a>'
		);

		const { renderMarkdown } = await import('./markdown');
		const result = renderMarkdown(md);
		expect(result).toContain('<h1>Heading</h1>');
		expect(result).toContain('target="_blank"');
	});

	it('strips script tags through sanitize step', async () => {
		mockParse.mockReturnValueOnce('<p>Hello</p><script>alert(1)</script>');
		mockSanitize.mockReturnValueOnce('<p>Hello</p>');
		mockSafeTarget.mockReturnValueOnce('<p>Hello</p>');

		const { renderMarkdown } = await import('./markdown');
		const result = renderMarkdown('Hello<script>alert(1)</script>');
		expect(result).toBe('<p>Hello</p>');
	});

	it('configures marked with gfm and breaks', async () => {
		const { marked } = await import('marked');
		await import('./markdown');
		expect(marked.setOptions).toHaveBeenCalledWith({
			gfm: true,
			breaks: true
		});
	});
});
