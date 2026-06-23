/**
 * RemoteStorage.js クライアント (v2 API)
 *
 * /open-nexus/ モジュール以下にユーザー個別データを保存:
 * - courses/: 履修科目
 * - todos/: TODO
 * - events/: カレンダーイベント
 * - attendance/: 出席記録
 * - settings/: ユーザー設定
 * - custom-colors/: カスタムカラー
 * - custom-packs/: カスタムテーマパック
 *
 * 対応バージョン: remotestoragejs@2.0.0-beta.9
 * - `moduleNames` オプション削除 → 代わりに `rs.scope('/open-nexus/')` を使用
 * - `declareType` は `BaseClient` に移動
 * - `access.claimAccess` → `access.claim` (v0.10 で rename)
 * - `rs[moduleName]` のマジッククライアント削除
 */

import { browser } from '$app/environment';
import RemoteStorage_ from 'remotestoragejs';

const APP_NAME = 'open-nexus';

type RSInstance = InstanceType<typeof RemoteStorage_> & {
	scope: (path: string) => BaseClientShape;
	access: { claim: (scope: string, mode: 'rw' | 'r') => void };
	on: (event: string, handler: (...args: unknown[]) => void) => void;
	connected: boolean;
	remote: { online: boolean; userAddress?: string };
	connect?: (userAddress: string) => void;
	disconnect?: () => void;
};

type BaseClientShape = {
	declareType: (alias: string, schema: unknown) => void;
	getAll: (path: string) => Promise<unknown>;
	getObject: (path: string) => Promise<unknown>;
	storeObject: (typeAlias: string, path: string, value: unknown) => Promise<unknown>;
	remove: (path: string) => Promise<unknown>;
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	on?: (event: string, handler: (...args: any[]) => void) => void;
};

let _rs: RSInstance | null = null;
let _client: BaseClientShape | null = null;

/** 内部キャッシュのリセット（テスト専用） */
export function _resetRemoteStorageForTest(): void {
	_rs = null;
	_client = null;
}

function getClient(): BaseClientShape {
	if (_client) return _client;
	if (!_rs) {
		// v2: コンストラクタは config のみ受け取る
		_rs = new RemoteStorage_({ cache: true }) as RSInstance;
		_client = _rs.scope(`/${APP_NAME}/`) as BaseClientShape;

		// v2: declareType は BaseClient 上で呼び出す
		_client.declareType('courses', { type: 'object', description: '履修科目' });
		_client.declareType('todos', { type: 'object', description: 'TODO' });
		_client.declareType('events', { type: 'object', description: 'カレンダーイベント' });
		_client.declareType('attendance', { type: 'object', description: '出席記録' });
		_client.declareType('settings', { type: 'object', description: 'ユーザー設定' });
		_client.declareType('custom-colors', { type: 'object', description: 'カスタムカラー' });
		_client.declareType('custom-packs', { type: 'object', description: 'カスタムテーマパック' });
	}
	// _client は直前の if ブロックで必ず代入されるため non-null
	return _client as BaseClientShape;
}

function getRs(): RSInstance {
	if (!_rs) {
		getClient();
	}
	return _rs!;
}

/**
 * RemoteStorage シングルトン（遅延初期化）
 * テストや SSR で必要になったタイミングで初めて実体化される
 */
export function getRemoteStorage(): RSInstance {
	return getRs();
}

/**
 * RemoteStorage シングルトン（後方互換）
 * v1 コードから `import { rs } from '...'` で参照している箇所のために
 * 遅延取得プロキシを返す。直接メソッドは持たず、必要なら getRemoteStorage() を使う。
 */
export const rs: RSInstance = new Proxy({} as RSInstance, {
	get(_target, prop: string | symbol) {
		return getRs()[prop as keyof RSInstance];
	}
});

/** ブラウザ環境で RemoteStorage を初期化 */
export function initRemoteStorage(): void {
	if (!browser) return;
	const instance = getRs();
	// v2: claimAccess → claim
	instance.access.claim(APP_NAME, 'rw');
	instance.on('connected', () => {
		(globalThis as { window?: Window }).window?.dispatchEvent(new CustomEvent('rs:connected'));
	});
	instance.on('disconnected', () => {
		(globalThis as { window?: Window }).window?.dispatchEvent(new CustomEvent('rs:disconnected'));
	});
}

/** 接続されているか */
export function isConnected(): boolean {
	if (!_rs) return false;
	return _rs.connected;
}

/** 指定パスのオブジェクトを全件取得 */
export async function rsGetAll<T = unknown>(path: string): Promise<Record<string, T>> {
	const obj = (await getClient().getAll(path)) as Record<string, T> | undefined;
	return obj ?? {};
}

/** 指定パスのオブジェクトを 1 件取得 */
export async function rsGet<T = unknown>(path: string): Promise<T | null> {
	const v = (await getClient().getObject(path)) as T | undefined;
	return v ?? null;
}

/** 指定パスに保存（path の最初のセグメントを typeAlias として使用） */
export async function rsSet<T>(path: string, value: T): Promise<void> {
	const typeAlias = path.split('/')[0] ?? 'object';
	await getClient().storeObject(typeAlias, path, value as unknown);
}

/** 指定パスを削除 */
export async function rsDelete(path: string): Promise<void> {
	await getClient().remove(path);
}

/** リモートストレージサーバーに接続 */
export function connectRemoteStorage(userAddress: string): void {
	if (!browser) return;
	const instance = getRs();
	if (typeof instance.connect === 'function') {
		instance.connect(userAddress);
	}
}

/** 接続を切断 */
export function disconnectRemoteStorage(): void {
	if (!browser) return;
	const instance = getRs();
	if (typeof instance.disconnect === 'function') {
		instance.disconnect();
	}
}

/** 接続中のユーザーアドレスを取得 */
export function getConnectedUserAddress(): string | null {
	if (!_rs) return null;
	return _rs.remote.userAddress || null;
}

/** リモートデータの変更イベントを購読 */
export function rsOnChange(
	handler: (event: { path: string; origin: string; oldValue?: unknown; newValue?: unknown }) => void
): void {
	if (!browser) return;
	const client = getClient();
	if (client && typeof client.on === 'function') {
		client.on('change', handler);
	}
}
