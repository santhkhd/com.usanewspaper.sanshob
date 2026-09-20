package com.app.webdroid.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.shobmc.san.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherTickerManager {

    private static final String PREF_NAME = "weather_ticker_prefs";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void bind(@NonNull View rootView, @NonNull Context context) {
        View weatherCard = rootView.findViewById(R.id.card_weather_briefing);
        if (weatherCard == null) return;

        TextView tvGreeting = rootView.findViewById(R.id.tv_weather_greeting);
        TextView tvCity = rootView.findViewById(R.id.tv_weather_city);
        TextView tvTemp = rootView.findViewById(R.id.tv_weather_temp);
        TextView tvDesc = rootView.findViewById(R.id.tv_weather_desc);
        TextView tvConditionBadge = rootView.findViewById(R.id.tv_weather_condition_badge);
        ImageView imgIcon = rootView.findViewById(R.id.img_weather_icon);

        TextView tvDowVal = rootView.findViewById(R.id.tv_dow_val);
        TextView tvDowChange = rootView.findViewById(R.id.tv_dow_change);
        TextView tvSpVal = rootView.findViewById(R.id.tv_sp_val);
        TextView tvSpChange = rootView.findViewById(R.id.tv_sp_change);
        TextView tvNasdaqVal = rootView.findViewById(R.id.tv_nasdaq_val);
        TextView tvNasdaqChange = rootView.findViewById(R.id.tv_nasdaq_change);
        TextView tvBtcVal = rootView.findViewById(R.id.tv_btc_val);
        TextView tvBtcChange = rootView.findViewById(R.id.tv_btc_change);
        TextView tvGoldVal = rootView.findViewById(R.id.tv_gold_val);
        TextView tvGoldChange = rootView.findViewById(R.id.tv_gold_change);

        // 1. Set dynamic greeting
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String dayName = new SimpleDateFormat("EEEE", Locale.US).format(new Date()).toUpperCase(Locale.US);
        String greetingPrefix = (hour < 12) ? "GOOD MORNING" : (hour < 17 ? "GOOD AFTERNOON" : "GOOD EVENING");
        if (tvGreeting != null) {
            tvGreeting.setText(greetingPrefix + " • " + dayName + " BRIEFING");
        }

        // 2. Load cached weather
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String cachedCity = sp.getString("weather_city", "Washington, D.C.");
        String cachedTemp = sp.getString("weather_temp", "74°F");
        String cachedDesc = sp.getString("weather_desc", "Sunny • H: 78°  L: 62°");
        String cachedBadge = sp.getString("weather_badge", "CLEAR");

        if (tvCity != null) tvCity.setText(cachedCity);
        if (tvTemp != null) tvTemp.setText(cachedTemp);
        if (tvDesc != null) tvDesc.setText(cachedDesc);
        if (tvConditionBadge != null) tvConditionBadge.setText(cachedBadge);

        // 3. Load cached markets
        if (tvDowVal != null) tvDowVal.setText(sp.getString("dow_val", "38,980.50"));
        if (tvDowChange != null) updateChangeText(tvDowChange, sp.getString("dow_chg", "+0.42%"));
        if (tvSpVal != null) tvSpVal.setText(sp.getString("sp_val", "5,117.09"));
        if (tvSpChange != null) updateChangeText(tvSpChange, sp.getString("sp_chg", "+0.56%"));
        if (tvNasdaqVal != null) tvNasdaqVal.setText(sp.getString("nasdaq_val", "16,177.77"));
        if (tvNasdaqChange != null) updateChangeText(tvNasdaqChange, sp.getString("nasdaq_chg", "+0.82%"));
        if (tvBtcVal != null) tvBtcVal.setText(sp.getString("btc_val", "$64,250"));
        if (tvBtcChange != null) updateChangeText(tvBtcChange, sp.getString("btc_chg", "+1.25%"));
        if (tvGoldVal != null) tvGoldVal.setText(sp.getString("gold_val", "$2,345"));
        if (tvGoldChange != null) updateChangeText(tvGoldChange, sp.getString("gold_chg", "+0.30%"));

        // 4. Fetch live data asynchronously
        refreshLiveMetrics(context, rootView);
    }

    public static void refreshLiveMetrics(@NonNull Context context, @NonNull View rootView) {
        executor.execute(() -> {
            try {
                // Fetch US Weather via Open-Meteo (Washington D.C. coordinates: 38.8951, -77.0364)
                String weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=38.8951&longitude=-77.0364&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min&temperature_unit=fahrenheit&timezone=America%2FNew_York";
                HttpURLConnection conn = (HttpURLConnection) new URL(weatherUrl).openConnection();
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    if (json.has("current")) {
                        JSONObject cur = json.getJSONObject("current");
                        int tempF = (int) Math.round(cur.optDouble("temperature_2m", 72.0));
                        int code = cur.optInt("weather_code", 0);

                        String condition = "Clear";
                        String badge = "CLEAR";
                        if (code == 1 || code == 2) {
                            condition = "Partly Cloudy";
                            badge = "CLOUDY";
                        } else if (code == 3) {
                            condition = "Overcast";
                            badge = "OVERCAST";
                        } else if (code >= 45 && code <= 48) {
                            condition = "Foggy";
                            badge = "FOG";
                        } else if (code >= 51 && code <= 67) {
                            condition = "Rain Showers";
                            badge = "RAIN";
                        } else if (code >= 71 && code <= 77) {
                            condition = "Snow Showers";
                            badge = "SNOW";
                        } else if (code >= 80 && code <= 99) {
                            condition = "Thunderstorms";
                            badge = "STORMS";
                        }

                        int highF = tempF + 5;
                        int lowF = Math.max(tempF - 12, 45);
                        if (json.has("daily")) {
                            JSONObject daily = json.getJSONObject("daily");
                            if (daily.has("temperature_2m_max") && daily.getJSONArray("temperature_2m_max").length() > 0) {
                                highF = (int) Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0));
                            }
                            if (daily.has("temperature_2m_min") && daily.getJSONArray("temperature_2m_min").length() > 0) {
                                lowF = (int) Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0));
                            }
                        }

                        final String fTemp = tempF + "°F";
                        final String fDesc = condition + " • H: " + highF + "°  L: " + lowF + "°";
                        final String fBadge = badge;

                        // Save to cache
                        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                                .putString("weather_temp", fTemp)
                                .putString("weather_desc", fDesc)
                                .putString("weather_badge", fBadge)
                                .apply();

                        mainHandler.post(() -> {
                            TextView tvTemp = rootView.findViewById(R.id.tv_weather_temp);
                            TextView tvDesc = rootView.findViewById(R.id.tv_weather_desc);
                            TextView tvConditionBadge = rootView.findViewById(R.id.tv_weather_condition_badge);
                            if (tvTemp != null) tvTemp.setText(fTemp);
                            if (tvDesc != null) tvDesc.setText(fDesc);
                            if (tvConditionBadge != null) tvConditionBadge.setText(fBadge);
                        });
                    }
                }
            } catch (Exception e) {
                // Keep cached values
            }
        });
    }

    private static void updateChangeText(TextView tv, String change) {
        if (tv == null || change == null) return;
        tv.setText(change);
        if (change.startsWith("+")) {
            tv.setTextColor(Color.parseColor("#10B981")); // Emerald Green
        } else if (change.startsWith("-")) {
            tv.setTextColor(Color.parseColor("#EF4444")); // Red
        } else {
            tv.setTextColor(Color.parseColor("#64748B"));
        }
    }
}
