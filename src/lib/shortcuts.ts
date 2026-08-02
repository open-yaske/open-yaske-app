import { Capacitor } from '@capacitor/core';
import { App } from '@capacitor/app';
import { SiriShortcuts } from '@bubble-labs/capacitor-plugin-siri-shorts';
import { m } from '$lib/paraglide/messages';

export type ShortcutFeature = 'timetable' | 'todo' | 'calendar' | 'attendance';

const SHORTCUT_FEATURES: ShortcutFeature[] = ['timetable', 'todo', 'calendar', 'attendance'];

const FEATURE_ROUTES: Record<ShortcutFeature, string> = {
	timetable: '/timetable',
	todo: '/todo',
	calendar: '/calendar',
	attendance: '/attendance'
};

/** Get localized display name for a feature. */
function getFeatureTitle(feature: ShortcutFeature): string {
	switch (feature) {
		case 'timetable':
			return m.nav_timetable();
		case 'todo':
			return m.nav_todo();
		case 'calendar':
			return m.nav_calendar();
		case 'attendance':
			return m.nav_attendance();
	}
}

/** Get localized Siri invocation phrase for a feature. */
function getInvocationPhrase(feature: ShortcutFeature): string {
	switch (feature) {
		case 'timetable':
			return m.shortcut_timetable_invocation();
		case 'todo':
			return m.shortcut_todo_invocation();
		case 'calendar':
			return m.shortcut_calendar_invocation();
		case 'attendance':
			return m.shortcut_attendance_invocation();
	}
}

/** Navigate to a feature page (shared between iOS Siri + Android App Actions). */
function navigateToFeature(feature: ShortcutFeature): void {
	const route = FEATURE_ROUTES[feature];
	if (!route) return;

	// Use dynamic import to avoid SSR issues with $app/navigation
	import('$app/navigation')
		.then(({ goto }) => goto(route))
		.catch((err) => console.error('Failed to navigate via shortcut:', err));
}

/**
 * Parse the feature name from a deep-link URL.
 * Expects URLs like `yaske://shortcuts/timetable`.
 */
function parseFeatureFromUrl(url: string): ShortcutFeature | null {
	try {
		const parsed = new URL(url);
		const segments = parsed.pathname.replace(/^\/+/, '').split('/');
		if (segments[0] === 'shortcuts' && segments.length >= 2) {
			const feature = segments[1] as ShortcutFeature;
			if (FEATURE_ROUTES[feature]) return feature;
		}
	} catch {
		// Fallback: simple string matching
		for (const feature of SHORTCUT_FEATURES) {
			if (url.includes(`shortcuts/${feature}`)) return feature;
		}
	}
	return null;
}

/**
 * Donate all feature shortcuts to Siri (iOS only).
 * Call this after the locale is initialized so titles/phrases are localized.
 */
export async function donateShortcuts(): Promise<void> {
	if (!Capacitor.isNativePlatform() || Capacitor.getPlatform() === 'android') {
		return;
	}

	try {
		for (const feature of SHORTCUT_FEATURES) {
			await SiriShortcuts.donate({
				persistentIdentifier: `open-${feature}`,
				title: getFeatureTitle(feature),
				suggestedInvocationPhrase: getInvocationPhrase(feature),
				isEligibleForSearch: true,
				isEligibleForPrediction: true,
				userInfo: { feature }
			});
		}
		console.log('Siri shortcuts donated successfully');
	} catch (err) {
		console.error('Failed to donate Siri shortcuts:', err);
	}
}

/**
 * Initialize shortcut listeners for both platforms.
 * - iOS: Listens for Siri Shortcut activation (appLaunchBySiriShortcuts)
 * - Both: Listens for deep-link URL opens (appUrlOpen) from Android App Actions / iOS
 *
 * Returns a cleanup function to remove listeners.
 */
export function initShortcuts(): () => void {
	const cleanups: (() => void)[] = [];

	if (!Capacitor.isNativePlatform()) {
		return () => {};
	}

	// iOS: Siri Shortcut activation via NSUserActivity
	if (Capacitor.getPlatform() === 'ios') {
		SiriShortcuts.addListener('appLaunchBySiriShortcuts', (shortcut) => {
			const feature = (shortcut as Record<string, string>).feature as ShortcutFeature;
			if (feature && FEATURE_ROUTES[feature]) {
				navigateToFeature(feature);
			}
		})
			.then((handle) => {
				cleanups.push(() => {
					void handle.remove();
				});
			})
			.catch((err) => console.error('Failed to add Siri shortcut listener:', err));
	}

	// Both platforms: Deep-link URL opens (Android App Actions, iOS Universal Links / URL scheme)
	App.addListener('appUrlOpen', (event) => {
		const feature = parseFeatureFromUrl(event.url);
		if (feature) {
			navigateToFeature(feature);
		}
	})
		.then((handle) => {
			cleanups.push(() => {
				void handle.remove();
			});
		})
		.catch((err) => console.error('Failed to add appUrlOpen listener:', err));

	return () => {
		for (const cleanup of cleanups) {
			cleanup();
		}
	};
}
