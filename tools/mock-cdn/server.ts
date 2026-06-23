/**
 * tools/mock-cdn/server.ts
 *
 * 開発時に本番 CDN の代わりに `/public-data.json` を返すローカルモック。
 * 本番では jsDelivr などの CDN を使用し、本モックは使用しない。
 *
 * 起動: `pnpm dev:mock-cdn` (concurrently 経由で `pnpm dev` と並行起動)
 * 単体:  `tsx tools/mock-cdn/server.ts`
 */
import { serve } from '@hono/node-server';
import { serveStatic } from '@hono/node-server/serve-static';
import { Hono } from 'hono';
import { readFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = join(__dirname, '..', '..');
const PUBLIC_DATA_PATH = join(ROOT, 'static', 'data', 'public-data.json');

const PORT = Number(process.env.MOCK_CDN_PORT ?? 5174);

type PublicDataResponse = {
	schemaVersion: number;
	generatedAt: string;
	index: { version: number; generatedAt: string; count: number };
	courses: unknown[];
	dashboardCards: unknown[];
};

async function loadPublicData(): Promise<PublicDataResponse> {
	const raw = await readFile(PUBLIC_DATA_PATH, 'utf-8');
	return JSON.parse(raw) as PublicDataResponse;
}

const app = new Hono();

app.get('/health', (c) => c.json({ ok: true, service: 'mock-cdn' }));

app.get('/public-data.json', async (c) => {
	try {
		const data = await loadPublicData();
		return c.json(data, 200, {
			'cache-control': 'public, max-age=300'
		});
	} catch (err) {
		console.error('[mock-cdn] failed to load public-data.json', err);
		return c.json({ error: 'failed to load public data' }, 500);
	}
});

// それ以外: static ディレクトリを配信
app.use(
	'/*',
	serveStatic({
		root: './static',
		rewriteRequestPath: (path) => path
	})
);

serve({ fetch: app.fetch, port: PORT }, (info) => {
	console.log(`[mock-cdn] listening on http://localhost:${info.port}`);
	console.log(`[mock-cdn]   /public-data.json`);
	console.log(`[mock-cdn]   /health`);
});
