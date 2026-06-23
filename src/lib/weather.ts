/**
 * Open-Meteo API クライアント
 * https://api.open-meteo.com/v1/forecast
 * 認証不要、緯度経度で現在の天気と時間別予報を取得
 */

export type WeatherCode =
	| 0 // Clear sky
	| 1
	| 2
	| 3 // Overcast
	| 45
	| 48 // Fog
	| 51
	| 53
	| 55 // Drizzle
	| 56
	| 57
	| 61
	| 63
	| 65 // Rain
	| 66
	| 67
	| 71
	| 73
	| 75 // Snow
	| 77
	| 80
	| 81
	| 82 // Showers
	| 85
	| 86
	| 95 // Thunderstorm
	| 96
	| 99;

export type CurrentWeather = {
	temperature: number;
	apparentTemperature: number;
	humidity: number;
	windSpeed: number;
	weatherCode: WeatherCode;
	isDay: boolean;
	time: string;
};

export type HourlyForecast = {
	hours: {
		time: string;
		temperature: number;
		weatherCode: WeatherCode;
		precipitationProbability: number;
	}[];
};

const BASE_URL = 'https://api.open-meteo.com/v1/forecast';

function mapWeatherCode(code: number): WeatherCode {
	return code as WeatherCode;
}

export async function getCurrentWeather(lat: number, lng: number): Promise<CurrentWeather> {
	const url = new URL(BASE_URL);
	url.searchParams.set('latitude', String(lat));
	url.searchParams.set('longitude', String(lng));
	url.searchParams.set(
		'current',
		'temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code,is_day'
	);
	url.searchParams.set('timezone', 'auto');

	const res = await fetch(url);
	if (!res.ok) throw new Error(`HTTP ${res.status}`);
	const data = await res.json();
	const c = data.current;
	return {
		temperature: c.temperature_2m,
		apparentTemperature: c.apparent_temperature,
		humidity: c.relative_humidity_2m,
		windSpeed: c.wind_speed_10m,
		weatherCode: mapWeatherCode(c.weather_code),
		isDay: c.is_day === 1,
		time: c.time
	};
}

export async function getHourlyForecast(lat: number, lng: number): Promise<HourlyForecast> {
	const url = new URL(BASE_URL);
	url.searchParams.set('latitude', String(lat));
	url.searchParams.set('longitude', String(lng));
	url.searchParams.set('hourly', 'temperature_2m,weather_code,precipitation_probability');
	url.searchParams.set('forecast_days', '2');
	url.searchParams.set('timezone', 'auto');

	const res = await fetch(url);
	if (!res.ok) throw new Error(`HTTP ${res.status}`);
	const data = await res.json();
	const h = data.hourly;
	const times: string[] = h.time;
	const temps: number[] = h.temperature_2m;
	const codes: number[] = h.weather_code;
	const probs: number[] = h.precipitation_probability;

	const now = Date.now();
	const hours = times
		.map((t, i) => ({
			time: t,
			temperature: temps[i],
			weatherCode: mapWeatherCode(codes[i]),
			precipitationProbability: probs[i] ?? 0
		}))
		.filter((h) => new Date(h.time).getTime() >= now - 60 * 60 * 1000)
		.slice(0, 24);

	return { hours };
}

/** 天気コード → ラベル (i18n key) */
export function weatherCodeToLabel(code: WeatherCode): string {
	if (code === 0) return 'weather_clear';
	if (code <= 3) return 'weather_partly_cloudy';
	if (code === 45 || code === 48) return 'weather_fog';
	if (code >= 51 && code <= 57) return 'weather_drizzle';
	if (code >= 61 && code <= 67) return 'weather_rain';
	if (code >= 71 && code <= 77) return 'weather_snow';
	if (code >= 80 && code <= 82) return 'weather_showers';
	if (code >= 85 && code <= 86) return 'weather_snow_showers';
	if (code >= 95) return 'weather_thunderstorm';
	return 'weather_unknown';
}

/** 天気コード → 絵文字 (簡易アイコン) */
export function weatherCodeToEmoji(code: WeatherCode, isDay = true): string {
	if (code === 0) return isDay ? '☀️' : '🌙';
	if (code <= 3) return isDay ? '⛅' : '☁️';
	if (code === 45 || code === 48) return '🌫️';
	if (code >= 51 && code <= 57) return '🌦️';
	if (code >= 61 && code <= 67) return '🌧️';
	if (code >= 71 && code <= 77) return '❄️';
	if (code >= 80 && code <= 82) return '🌧️';
	if (code >= 85 && code <= 86) return '🌨️';
	if (code >= 95) return '⛈️';
	return '🌡️';
}
