package com.openyaske.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class TimetableWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // SharedPreferences からデータを読み込む
        SharedPreferences prefs = context.getSharedPreferences("group.com.openyaske.app", Context.MODE_PRIVATE);
        String coursesJson = prefs.getString("courses", "[]");
        String periodsJson = prefs.getString("timetablePeriods", "{}");
        String themeColorStr = prefs.getString("widgetThemeColor", "#17288B");
        boolean useSystemTheme = "true".equals(prefs.getString("widgetUseSystemTheme", "true"));

        // 今日の曜日を取得
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String dayKey;
        String dayText;

        switch (dayOfWeek) {
            case Calendar.MONDAY:    dayKey = "mon"; dayText = "月曜日"; break;
            case Calendar.TUESDAY:   dayKey = "tue"; dayText = "火曜日"; break;
            case Calendar.WEDNESDAY: dayKey = "wed"; dayText = "水曜日"; break;
            case Calendar.THURSDAY:  dayKey = "thu"; dayText = "木曜日"; break;
            case Calendar.FRIDAY:    dayKey = "fri"; dayText = "金曜日"; break;
            case Calendar.SATURDAY:  dayKey = "sat"; dayText = "土曜日"; break;
            case Calendar.SUNDAY:    dayKey = "sun"; dayText = "日曜日"; break;
            default:                 dayKey = "mon"; dayText = "月曜日"; break;
        }

        // コースデータのパース
        JSONArray coursesArray = new JSONArray();
        try {
            coursesArray = new JSONArray(coursesJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 今日の授業を時限（1〜7）ごとにマップ化
        Map<Integer, JSONObject> todayCourses = new HashMap<>();
        for (int i = 0; i < coursesArray.length(); i++) {
            try {
                JSONObject c = coursesArray.getJSONObject(i);
                if (dayKey.equals(c.optString("day"))) {
                    int period = c.optInt("period", 0);
                    if (period >= 1 && period <= 7) {
                        todayCourses.put(period, c);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // 時限の時間帯（開始-終了）のパース
        Map<Integer, String> periodTimes = new HashMap<>();
        try {
            JSONObject periodsObj = new JSONObject(periodsJson);
            for (int p = 1; p <= 7; p++) {
                JSONObject periodObj = periodsObj.optJSONObject(String.valueOf(p));
                if (periodObj != null) {
                    String start = periodObj.optString("start", "");
                    String end = periodObj.optString("end", "");
                    if (!start.isEmpty() && !end.isEmpty()) {
                        periodTimes.put(p, start + "-" + end);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 各ウィジェットの表示を更新
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.timetable_widget);
            views.setTextViewText(R.id.widget_day, dayText);

            // テーマカラーの反映
            if (!useSystemTheme) {
                try {
                    int color = Color.parseColor(themeColorStr);
                    views.setInt(R.id.widget_header, "setBackgroundColor", color);
                    views.setTextColor(R.id.widget_title, Color.WHITE);
                    views.setTextColor(R.id.widget_day, Color.WHITE);
                    views.setViewVisibility(R.id.header_divider, View.GONE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // デフォルト（システムテーマ）は白背景とグレー系のテキスト
                views.setInt(R.id.widget_header, "setBackgroundColor", Color.parseColor("#FFFFFF"));
                views.setTextColor(R.id.widget_title, Color.parseColor("#2D3748"));
                views.setTextColor(R.id.widget_day, Color.parseColor("#718096"));
                views.setViewVisibility(R.id.header_divider, View.VISIBLE);
            }

            // 1〜7限までの項目設定
            for (int p = 1; p <= 7; p++) {
                int nameId = context.getResources().getIdentifier("period" + p + "_name", "id", context.getPackageName());
                int classroomId = context.getResources().getIdentifier("period" + p + "_classroom", "id", context.getPackageName());
                int timeId = context.getResources().getIdentifier("period" + p + "_time", "id", context.getPackageName());

                // カスタム時限時間の反映
                if (periodTimes.containsKey(p)) {
                    views.setTextViewText(timeId, periodTimes.get(p));
                }

                JSONObject course = todayCourses.get(p);
                if (course != null) {
                    views.setTextViewText(nameId, course.optString("name", "-"));
                    views.setTextViewText(classroomId, course.optString("classroom", "-"));
                } else {
                    views.setTextViewText(nameId, "空きコマ");
                    views.setTextViewText(classroomId, "-");
                }
            }

            // ウィジェットタップでアプリ起動（MainActivity）
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT |
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );
            views.setOnClickPendingIntent(R.id.widget_header, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
