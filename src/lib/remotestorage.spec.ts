import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('$app/environment', () => ({
	browser: true,
	dev: true,
	building: false,
	version: 'test'
}));

const mockClient = {
	declareType: vi.fn(),
	getAll: vi.fn(),
	getObject: vi.fn(),
	storeObject: vi.fn(),
	remove: vi.fn(),
	on: vi.fn()
};

const mockRs = {
	scope: vi.fn(() => mockClient),
	access: { claim: vi.fn() },
	on: vi.fn(),
	remote: { online: false, userAddress: undefined as string | undefined },
	get connected() {
		return this.remote.online;
	},
	connect: vi.fn(),
	disconnect: vi.fn()
};

vi.mock('remotestoragejs', () => ({
	default: vi.fn(function () {
		return mockRs;
	})
}));

const dispatchEventSpy = vi.fn();
// Vitest node 環境では `globalThis.window` が undefined のため
// initRemoteStorage が globalThis.window を読むよう、後段の beforeEach で
// セットアップする。

describe('remotestorage', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		mockRs.remote.online = false;
		mockRs.remote.userAddress = undefined;
		mockRs.connect.mockClear();
		mockRs.disconnect.mockClear();
		mockRs.access.claim.mockClear();
		mockRs.on.mockClear();
		mockRs.scope.mockClear();
		mockRs.scope.mockImplementation(() => mockClient);
		mockClient.declareType.mockClear();
		mockClient.getAll.mockReset();
		mockClient.getObject.mockReset();
		mockClient.storeObject.mockReset();
		mockClient.remove.mockReset();
		dispatchEventSpy.mockClear();
		// globalThis.window が SSR 環境では undefined のため、テスト用にスタブ
		(globalThis as unknown as { window?: { dispatchEvent: typeof dispatchEventSpy } }).window = {
			dispatchEvent: dispatchEventSpy
		};
	});

	it('initializes RemoteStorage with cache only and declares types on scoped client', async () => {
		const RemoteStorage = (await import('remotestoragejs')).default;
		const { getRemoteStorage } = await import('./remotestorage');
		const instance = getRemoteStorage();
		expect(instance).toBe(mockRs);
		expect(RemoteStorage).toHaveBeenCalledWith({ cache: true });
		expect(mockRs.scope).toHaveBeenCalledWith('/open-nexus/');
		expect(mockClient.declareType).toHaveBeenCalledTimes(7);
		expect(mockClient.declareType).toHaveBeenCalledWith('courses', {
			type: 'object',
			description: '履修科目'
		});
		expect(mockClient.declareType).toHaveBeenCalledWith('todos', {
			type: 'object',
			description: 'TODO'
		});
		expect(mockClient.declareType).toHaveBeenCalledWith('attendance', {
			type: 'object',
			description: '出席記録'
		});
	});

	it('initRemoteStorage does nothing in non-browser environment', async () => {
		// 動的に $app/environment の browser を false に切替え、テスト後の
		// 後始末で戻す。先に _rs キャッシュをクリアして新規 instance を
		// 作らせない（mock 参照が保たれるようにするため）。
		const envMod = await import('$app/environment');
		const original = envMod.browser;
		Object.defineProperty(envMod, 'browser', { configurable: true, get: () => false });
		const { initRemoteStorage, _resetRemoteStorageForTest } = await import('./remotestorage');
		_resetRemoteStorageForTest();
		initRemoteStorage();
		expect(mockRs.access.claim).not.toHaveBeenCalled();
		_resetRemoteStorageForTest();
		Object.defineProperty(envMod, 'browser', { configurable: true, get: () => original });
	});

	it('initRemoteStorage claims access and registers event listeners', async () => {
		const { initRemoteStorage } = await import('./remotestorage');
		initRemoteStorage();
		expect(mockRs.access.claim).toHaveBeenCalledWith('open-nexus', 'rw');
		expect(mockRs.on).toHaveBeenCalledWith('connected', expect.any(Function));
		expect(mockRs.on).toHaveBeenCalledWith('disconnected', expect.any(Function));
	});

	it('dispatches rs:connected event on connected', async () => {
		const { initRemoteStorage } = await import('./remotestorage');
		initRemoteStorage();
		const connectedHandler = mockRs.on.mock.calls.find((call) => call[0] === 'connected')?.[1];
		expect(connectedHandler).toBeDefined();
		connectedHandler();
		expect(dispatchEventSpy).toHaveBeenCalledTimes(1);
		const event = dispatchEventSpy.mock.calls[0][0];
		expect(event.type).toBe('rs:connected');
	});

	it('dispatches rs:disconnected event on disconnected', async () => {
		const { initRemoteStorage } = await import('./remotestorage');
		initRemoteStorage();
		const disconnectedHandler = mockRs.on.mock.calls.find(
			(call) => call[0] === 'disconnected'
		)?.[1];
		expect(disconnectedHandler).toBeDefined();
		disconnectedHandler();
		expect(dispatchEventSpy).toHaveBeenCalledTimes(1);
		const event = dispatchEventSpy.mock.calls[0][0];
		expect(event.type).toBe('rs:disconnected');
	});

	it('isConnected returns remote.online status', async () => {
		const { isConnected } = await import('./remotestorage');
		expect(isConnected()).toBe(false);
		mockRs.remote.online = true;
		expect(isConnected()).toBe(true);
	});

	it('rsGetAll returns all objects at path', async () => {
		mockClient.getAll.mockResolvedValue({ a: 1, b: 2 });
		const { rsGetAll } = await import('./remotestorage');
		const result = await rsGetAll('courses');
		expect(result).toEqual({ a: 1, b: 2 });
		expect(mockClient.getAll).toHaveBeenCalledWith('courses');
	});

	it('rsGetAll returns empty object when no data', async () => {
		mockClient.getAll.mockResolvedValue(undefined);
		const { rsGetAll } = await import('./remotestorage');
		const result = await rsGetAll('todos');
		expect(result).toEqual({});
	});

	it('rsGet returns single object at path', async () => {
		mockClient.getObject.mockResolvedValue({ title: 'Test' });
		const { rsGet } = await import('./remotestorage');
		const result = await rsGet('todos/task-1');
		expect(result).toEqual({ title: 'Test' });
		expect(mockClient.getObject).toHaveBeenCalledWith('todos/task-1');
	});

	it('rsGet returns null when no data', async () => {
		mockClient.getObject.mockResolvedValue(undefined);
		const { rsGet } = await import('./remotestorage');
		const result = await rsGet('todos/missing');
		expect(result).toBeNull();
	});

	it('rsSet stores object with short type alias derived from path', async () => {
		mockClient.storeObject.mockResolvedValue(undefined);
		const { rsSet } = await import('./remotestorage');
		await rsSet('todos/task-1', { title: 'Buy milk' });
		expect(mockClient.storeObject).toHaveBeenCalledWith('todos', 'todos/task-1', {
			title: 'Buy milk'
		});
	});

	it('rsDelete removes object at path', async () => {
		mockClient.remove.mockResolvedValue(undefined);
		const { rsDelete } = await import('./remotestorage');
		await rsDelete('todos/task-1');
		expect(mockClient.remove).toHaveBeenCalledWith('todos/task-1');
	});

	it('connectRemoteStorage calls instance.connect', async () => {
		const { connectRemoteStorage } = await import('./remotestorage');
		connectRemoteStorage('test@example.com');
		expect(mockRs.connect).toHaveBeenCalledWith('test@example.com');
	});

	it('disconnectRemoteStorage calls instance.disconnect', async () => {
		const { disconnectRemoteStorage } = await import('./remotestorage');
		disconnectRemoteStorage();
		expect(mockRs.disconnect).toHaveBeenCalled();
	});

	it('getConnectedUserAddress returns userAddress from instance', async () => {
		const { getConnectedUserAddress } = await import('./remotestorage');
		expect(getConnectedUserAddress()).toBeNull();
		mockRs.remote.userAddress = 'test@example.com';
		expect(getConnectedUserAddress()).toBe('test@example.com');
	});

	it('rsOnChange registers change handler on client', async () => {
		const { rsOnChange } = await import('./remotestorage');
		const handler = vi.fn();
		mockClient.on = vi.fn();
		rsOnChange(handler);
		expect(mockClient.on).toHaveBeenCalledWith('change', handler);
	});
});
