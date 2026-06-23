import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockSanitize = vi.fn((html: string) => `SANITIZED:${html}`);

vi.mock('dompurify', () => ({
	default: {
		sanitize: mockSanitize
	}
}));

describe('html-renderer', () => {
	beforeEach(() => {
		mockSanitize.mockClear();
	});

	it('sanitize delegates to DOMPurify with allowlist config', async () => {
		const { sanitize } = await import('./html-renderer');
		const input = '<p>Hello</p><script>alert(1)</script>';
		sanitize(input);
		expect(mockSanitize).toHaveBeenCalledTimes(1);
		expect(mockSanitize).toHaveBeenCalledWith(
			input,
			expect.objectContaining({
				ALLOWED_TAGS: expect.arrayContaining(['a', 'p', 'div', 'img', 'table']),
				ALLOWED_ATTR: expect.arrayContaining(['href', 'src', 'alt', 'target', 'rel']),
				ALLOW_DATA_ATTR: false
			})
		);
	});

	it('sanitize strips disallowed tags via DOMPurify', async () => {
		mockSanitize.mockReturnValueOnce('<p>Safe content</p>');
		const { sanitize } = await import('./html-renderer');
		const result = sanitize('<p>Safe content</p><iframe src="evil.com"></iframe>');
		expect(result).toBe('<p>Safe content</p>');
	});
});

// safeTarget is pure string manipulation, test directly without needing mocks
describe('html-renderer.safeTarget', () => {
	it('adds target="_blank" when missing', async () => {
		const { safeTarget } = await import('./html-renderer');
		const input = '<a href="https://example.com">Link</a>';
		const result = safeTarget(input);
		expect(result).toBe(
			'<a href="https://example.com" target="_blank" rel="noopener noreferrer">Link</a>'
		);
	});

	it('adds rel="noopener noreferrer" when missing', async () => {
		const { safeTarget } = await import('./html-renderer');
		const input = '<a href="https://example.com" target="_blank">Link</a>';
		const result = safeTarget(input);
		expect(result).toBe(
			'<a href="https://example.com" target="_blank" rel="noopener noreferrer">Link</a>'
		);
	});

	it('preserves existing target and rel', async () => {
		const { safeTarget } = await import('./html-renderer');
		const input = '<a href="https://example.com" target="_self" rel="nofollow">Link</a>';
		const result = safeTarget(input);
		expect(result).toBe(input);
	});

	it('handles multiple links in HTML', async () => {
		const { safeTarget } = await import('./html-renderer');
		const input = '<p><a href="https://a.com">A</a> and <a href="https://b.com">B</a></p>';
		const result = safeTarget(input);
		expect(result).toContain(
			'<a href="https://a.com" target="_blank" rel="noopener noreferrer">A</a>'
		);
		expect(result).toContain(
			'<a href="https://b.com" target="_blank" rel="noopener noreferrer">B</a>'
		);
	});

	it('handles links with other attributes', async () => {
		const { safeTarget } = await import('./html-renderer');
		const input = '<a class="btn" href="https://example.com" title="Click">Link</a>';
		const result = safeTarget(input);
		expect(result).toBe(
			'<a class="btn" href="https://example.com" title="Click" target="_blank" rel="noopener noreferrer">Link</a>'
		);
	});

	it('does not modify non-anchor tags', async () => {
		const { safeTarget } = await import('./html-renderer');
		const input = '<div><span>text</span></div>';
		const result = safeTarget(input);
		expect(result).toBe(input);
	});

	it('prevents javascript: protocol XSS by relying on sanitize', async () => {
		mockSanitize.mockReturnValueOnce('<a>Click</a>');
		const { sanitize } = await import('./html-renderer');
		const input = '<a href="javascript:alert(1)">Click</a>';
		const result = sanitize(input);
		expect(result).toBe('<a>Click</a>');
	});

	it('prevents onerror XSS by relying on sanitize', async () => {
		mockSanitize.mockReturnValueOnce('<img src="x">');
		const { sanitize } = await import('./html-renderer');
		const input = '<img src="x" onerror="alert(1)">';
		const result = sanitize(input);
		expect(result).toBe('<img src="x">');
	});
});
