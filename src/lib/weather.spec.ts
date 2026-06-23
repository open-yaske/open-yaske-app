import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockFetch = vi.fn();
vi.stubGlobal('fetch', mockFetch);

describe('weather', () => {
	beforeEach(() => {
		vi.resetModules();
		mockFetch.mockReset();
	});

	describe('getCurrentWeather', () => {
		it('parses Open-Meteo response correctly', async () => {
			mockFetch.mockResolvedValue({
				ok: true,
				json: async () => ({
					current: {
						temperature_2m: 25.5,
						apparent_temperature: 27.0,
						relative_humidity_2m: 60,
						wind_speed_10m: 5.2,
						weather_code: 1,
						is_day: 1,
						time: '2026-06-22T12:00'
					}
				})
			});

			const { getCurrentWeather } = await import('./weather');
			const weather = await getCurrentWeather(35.6586, 139.7454);
			expect(weather.temperature).toBe(25.5);
			expect(weather.apparentTemperature).toBe(27.0);
			expect(weather.humidity).toBe(60);
			expect(weather.windSpeed).toBe(5.2);
			expect(weather.weatherCode).toBe(1);
			expect(weather.isDay).toBe(true);
			expect(weather.time).toBe('2026-06-22T12:00');
		});

		it('throws on HTTP error', async () => {
			mockFetch.mockResolvedValue({ ok: false, status: 429 });
			const { getCurrentWeather } = await import('./weather');
			await expect(getCurrentWeather(0, 0)).rejects.toThrow('HTTP 429');
		});

		it('constructs correct URL with query params', async () => {
			mockFetch.mockResolvedValue({
				ok: true,
				json: async () => ({
					current: {
						temperature_2m: 20,
						apparent_temperature: 20,
						relative_humidity_2m: 50,
						wind_speed_10m: 3,
						weather_code: 0,
						is_day: 0,
						time: '2026-06-22T00:00'
					}
				})
			});

			const { getCurrentWeather } = await import('./weather');
			await getCurrentWeather(35.0, 139.0);
			const callUrl = new URL(mockFetch.mock.calls[0][0]);
			expect(callUrl.searchParams.get('latitude')).toBe('35');
			expect(callUrl.searchParams.get('longitude')).toBe('139');
			expect(callUrl.searchParams.get('timezone')).toBe('auto');
			expect(callUrl.searchParams.get('current')).toContain('temperature_2m');
			expect(callUrl.searchParams.get('current')).toContain('weather_code');
		});
	});

	describe('getHourlyForecast', () => {
		it('filters past hours and limits to 24', async () => {
			const now = new Date('2026-06-22T12:00:00Z');
			vi.setSystemTime(now);

			const times: string[] = [];
			const temps: number[] = [];
			const codes: number[] = [];
			const probs: number[] = [];
			for (let i = 0; i < 48; i++) {
				const d = new Date(now);
				d.setHours(d.getHours() - 12 + i);
				times.push(d.toISOString());
				temps.push(20 + i);
				codes.push(0);
				probs.push(i % 10);
			}

			mockFetch.mockResolvedValue({
				ok: true,
				json: async () => ({
					hourly: {
						time: times,
						temperature_2m: temps,
						weather_code: codes,
						precipitation_probability: probs
					}
				})
			});

			const { getHourlyForecast } = await import('./weather');
			const forecast = await getHourlyForecast(35.0, 139.0);
			expect(forecast.hours.length).toBe(24);
			// First hour should be within 1h before now or after
			const firstHourTime = new Date(forecast.hours[0].time).getTime();
			expect(firstHourTime).toBeGreaterThanOrEqual(now.getTime() - 60 * 60 * 1000);
			vi.useRealTimers();
		});

		it('throws on HTTP error', async () => {
			mockFetch.mockResolvedValue({ ok: false, status: 500 });
			const { getHourlyForecast } = await import('./weather');
			await expect(getHourlyForecast(0, 0)).rejects.toThrow('HTTP 500');
		});
	});

	describe('weatherCodeToLabel', () => {
		it('maps clear sky', async () => {
			const { weatherCodeToLabel } = await import('./weather');
			expect(weatherCodeToLabel(0)).toBe('weather_clear');
		});

		it('maps partly cloudy', async () => {
			const { weatherCodeToLabel } = await import('./weather');
			expect(weatherCodeToLabel(1)).toBe('weather_partly_cloudy');
			expect(weatherCodeToLabel(2)).toBe('weather_partly_cloudy');
			expect(weatherCodeToLabel(3)).toBe('weather_partly_cloudy');
		});

		it('maps fog', async () => {
			const { weatherCodeToLabel } = await import('./weather');
			expect(weatherCodeToLabel(45)).toBe('weather_fog');
			expect(weatherCodeToLabel(48)).toBe('weather_fog');
		});

		it('maps drizzle', async () => {
			const { weatherCodeToLabel } = await import('./weather');
			expect(weatherCodeToLabel(51)).toBe('weather_drizzle');
			expect(weatherCodeToLabel(55)).toBe('weather_drizzle');
			expect(weatherCodeToLabel(57)).toBe('weather_drizzle');
		});

		it('maps rain', async () => {
			const { weatherCodeToLabel } = await import('./weather');
			expect(weatherCodeToLabel(61)).toBe('weather_rain');
			expect(weatherCodeToLabel(65)).toBe('weather_rain');
			expect(weatherCodeToLabel(67)).toBe('weather_rain');
		});

		it('maps snow', async () => {
			const { weatherCodeToLabel } = await import('./weather');
			expect(weatherCodeToLabel(71)).toBe('weather_snow');
			expect(weatherCodeToLabel(77)).toBe('weather_snow');
		});

		it('maps showers', async () => {
			const { weatherCodeToLabel } = await import('./weather');
			expect(weatherCodeToLabel(80)).toBe('weather_showers');
			expect(weatherCodeToLabel(82)).toBe('weather_showers');
		});

		it('maps snow showers', async () => {
			const { weatherCodeToLabel } = await import('./weather');
			expect(weatherCodeToLabel(85)).toBe('weather_snow_showers');
			expect(weatherCodeToLabel(86)).toBe('weather_snow_showers');
		});

		it('maps thunderstorm', async () => {
			const { weatherCodeToLabel } = await import('./weather');
			expect(weatherCodeToLabel(95)).toBe('weather_thunderstorm');
			expect(weatherCodeToLabel(99)).toBe('weather_thunderstorm');
		});

		it('returns unknown for out-of-range codes', async () => {
			const { weatherCodeToLabel } = await import('./weather');
			// Test invalid codes at runtime; cast through unknown so the test
			// can verify the fall-through branch without weakening the WeatherCode type.
			expect(weatherCodeToLabel(9 as unknown as Parameters<typeof weatherCodeToLabel>[0])).toBe(
				'weather_unknown'
			);
			expect(weatherCodeToLabel(90 as unknown as Parameters<typeof weatherCodeToLabel>[0])).toBe(
				'weather_unknown'
			);
		});
	});

	describe('weatherCodeToEmoji', () => {
		it('returns sun during day and moon at night for clear', async () => {
			const { weatherCodeToEmoji } = await import('./weather');
			expect(weatherCodeToEmoji(0, true)).toBe('☀️');
			expect(weatherCodeToEmoji(0, false)).toBe('🌙');
		});

		it('returns partly-cloudy emoji', async () => {
			const { weatherCodeToEmoji } = await import('./weather');
			expect(weatherCodeToEmoji(1, true)).toBe('⛅');
			expect(weatherCodeToEmoji(3, false)).toBe('☁️');
		});

		it('returns fog emoji', async () => {
			const { weatherCodeToEmoji } = await import('./weather');
			expect(weatherCodeToEmoji(45)).toBe('🌫️');
		});

		it('returns drizzle emoji', async () => {
			const { weatherCodeToEmoji } = await import('./weather');
			expect(weatherCodeToEmoji(53)).toBe('🌦️');
		});

		it('returns rain emoji', async () => {
			const { weatherCodeToEmoji } = await import('./weather');
			expect(weatherCodeToEmoji(63)).toBe('🌧️');
		});

		it('returns snow emoji', async () => {
			const { weatherCodeToEmoji } = await import('./weather');
			expect(weatherCodeToEmoji(73)).toBe('❄️');
		});

		it('returns showers emoji', async () => {
			const { weatherCodeToEmoji } = await import('./weather');
			expect(weatherCodeToEmoji(81)).toBe('🌧️');
		});

		it('returns snow showers emoji', async () => {
			const { weatherCodeToEmoji } = await import('./weather');
			expect(weatherCodeToEmoji(86)).toBe('🌨️');
		});

		it('returns thunderstorm emoji', async () => {
			const { weatherCodeToEmoji } = await import('./weather');
			expect(weatherCodeToEmoji(96)).toBe('⛈️');
		});

		it('returns thermometer for unknown codes', async () => {
			const { weatherCodeToEmoji } = await import('./weather');
			expect(weatherCodeToEmoji(9 as unknown as Parameters<typeof weatherCodeToEmoji>[0])).toBe(
				'🌡️'
			);
			expect(weatherCodeToEmoji(90 as unknown as Parameters<typeof weatherCodeToEmoji>[0])).toBe(
				'🌡️'
			);
		});
	});
});
