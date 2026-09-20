package com.app.webdroid.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StateWeatherManager {

    private static final String PREF_NAME = "state_weather_cache";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static class DayForecast {
        public String dayName;
        public int maxTempF;
        public int minTempF;
        public int weatherCode;
        public String condition;

        public DayForecast(String dayName, int maxTempF, int minTempF, int weatherCode, String condition) {
            this.dayName = dayName;
            this.maxTempF = maxTempF;
            this.minTempF = minTempF;
            this.weatherCode = weatherCode;
            this.condition = condition;
        }
    }

    public static class StateLocation {
        public String stateName;
        public String majorCity;
        public double lat;
        public double lon;

        public StateLocation(String stateName, String majorCity, double lat, double lon) {
            this.stateName = stateName;
            this.majorCity = majorCity;
            this.lat = lat;
            this.lon = lon;
        }
    }

    private static final Map<String, StateLocation> STATE_LOCATIONS = new HashMap<>();

    static {
        add("alabama", "Alabama", "Birmingham", 33.5186, -86.8104);
        add("alaska", "Alaska", "Anchorage", 61.2181, -149.9003);
        add("arizona", "Arizona", "Phoenix", 33.4484, -112.0740);
        add("arkansas", "Arkansas", "Little Rock", 34.7465, -92.2896);
        add("california", "California", "Los Angeles", 34.0522, -118.2437);
        add("colorado", "Colorado", "Denver", 39.7392, -104.9903);
        add("connecticut", "Connecticut", "Hartford", 41.7658, -72.6734);
        add("delaware", "Delaware", "Wilmington", 39.7447, -75.5484);
        add("district_of_columbia", "District of Columbia", "Washington, D.C.", 38.8951, -77.0364);
        add("florida", "Florida", "Miami", 25.7617, -80.1918);
        add("georgia", "Georgia", "Atlanta", 33.7490, -84.3880);
        add("hawaii", "Hawaii", "Honolulu", 21.3069, -157.8583);
        add("idaho", "Idaho", "Boise", 43.6150, -116.2023);
        add("illinois", "Illinois", "Chicago", 41.8781, -87.6298);
        add("indiana", "Indiana", "Indianapolis", 39.7684, -86.1581);
        add("iowa", "Iowa", "Des Moines", 41.5868, -93.6250);
        add("kansas", "Kansas", "Wichita", 37.6872, -97.3301);
        add("kentucky", "Kentucky", "Louisville", 38.2527, -85.7585);
        add("louisiana", "Louisiana", "New Orleans", 29.9511, -90.0715);
        add("maine", "Maine", "Portland", 43.6591, -70.2568);
        add("maryland", "Maryland", "Baltimore", 39.2904, -76.6122);
        add("massachusetts", "Massachusetts", "Boston", 42.3601, -71.0589);
        add("michigan", "Michigan", "Detroit", 42.3314, -83.0458);
        add("minnesota", "Minnesota", "Minneapolis", 44.9778, -93.2650);
        add("mississippi", "Mississippi", "Jackson", 32.2988, -90.1848);
        add("missouri", "Missouri", "St. Louis", 38.6270, -90.1994);
        add("montana", "Montana", "Billings", 45.7833, -108.5007);
        add("nebraska", "Nebraska", "Omaha", 41.2565, -95.9345);
        add("nevada", "Nevada", "Las Vegas", 36.1699, -115.1398);
        add("new_hampshire", "New Hampshire", "Manchester", 42.9956, -71.4548);
        add("new_jersey", "New Jersey", "Newark", 40.7357, -74.1724);
        add("new_mexico", "New Mexico", "Albuquerque", 35.0844, -106.6504);
        add("new_york", "New York", "New York City", 40.7128, -74.0060);
        add("north_carolina", "North Carolina", "Charlotte", 35.2271, -80.8431);
        add("north_dakota", "North Dakota", "Fargo", 46.8772, -96.7898);
        add("ohio", "Ohio", "Columbus", 39.9612, -82.9988);
        add("oklahoma", "Oklahoma", "Oklahoma City", 35.4676, -97.5164);
        add("oregon", "Oregon", "Portland", 45.5152, -122.6784);
        add("pennsylvania", "Pennsylvania", "Philadelphia", 39.9526, -75.1652);
        add("rhode_island", "Rhode Island", "Providence", 41.8240, -71.4128);
        add("south_carolina", "South Carolina", "Charleston", 32.7765, -79.9311);
        add("south_dakota", "South Dakota", "Sioux Falls", 43.5460, -96.7313);
        add("tennessee", "Tennessee", "Nashville", 36.1627, -86.7816);
        add("texas", "Texas", "Houston", 29.7604, -95.3698);
        add("utah", "Utah", "Salt Lake City", 40.7608, -111.8910);
        add("vermont", "Vermont", "Burlington", 44.4759, -73.2121);
        add("virginia", "Virginia", "Virginia Beach", 36.8529, -75.9780);
        add("washington", "Washington", "Seattle", 47.6062, -122.3321);
        add("west_virginia", "West Virginia", "Charleston", 38.3498, -81.6326);
        add("wisconsin", "Wisconsin", "Milwaukee", 43.0389, -87.9065);
        add("wyoming", "Wyoming", "Cheyenne", 41.1400, -104.8202);
    }

    private static void add(String key, String stateName, String city, double lat, double lon) {
        STATE_LOCATIONS.put(key.toLowerCase(), new StateLocation(stateName, city, lat, lon));
    }

    public static StateLocation resolveLocation(String stateInput) {
        if (stateInput == null) return STATE_LOCATIONS.get("district_of_columbia");
        String clean = stateInput.toLowerCase().trim()
                .replace(".json", "")
                .replace("states/", "")
                .replace(" ", "_")
                .replace("🏛️", "")
                .trim();

        for (Map.Entry<String, StateLocation> entry : STATE_LOCATIONS.entrySet()) {
            if (clean.contains(entry.getKey()) || entry.getKey().contains(clean)
                    || entry.getValue().stateName.equalsIgnoreCase(stateInput.trim())) {
                return entry.getValue();
            }
        }
        return new StateLocation("United States", "Washington, D.C.", 38.8951, -77.0364);
    }

    public interface WeatherCallback {
        void onWeatherLoaded(int currentTempF, String condition, int highF, int lowF, List<DayForecast> weeklyForecast);
    }

    public static void fetchStateWeather(@NonNull Context context, String stateKey, @NonNull WeatherCallback callback) {
        final StateLocation loc = resolveLocation(stateKey);
        final SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // 1. Deliver cached forecast immediately if available
        String cacheKey = "cache_" + loc.stateName.toLowerCase().replace(" ", "_");
        String cachedJson = sp.getString(cacheKey, null);
        if (cachedJson != null) {
            try {
                JSONObject obj = new JSONObject(cachedJson);
                int curTemp = obj.optInt("current_temp", 72);
                String cond = obj.optString("condition", "Sunny");
                int high = obj.optInt("high", 78);
                int low = obj.optInt("low", 62);
                JSONArray arr = obj.optJSONArray("days");
                List<DayForecast> days = new ArrayList<>();
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject d = arr.getJSONObject(i);
                        days.add(new DayForecast(d.getString("day"), d.getInt("max"), d.getInt("min"), d.getInt("code"), d.getString("cond")));
                    }
                }
                callback.onWeatherLoaded(curTemp, cond, high, low, days);
            } catch (Exception ignored) {}
        }

        // 2. Fetch fresh weather asynchronously
        executor.execute(() -> {
            try {
                String urlStr = String.format(Locale.US,
                        "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min&temperature_unit=fahrenheit&timezone=auto",
                        loc.lat, loc.lon);

                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
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

                    JSONObject root = new JSONObject(sb.toString());
                    int curTemp = 72;
                    String mainCond = "Sunny";
                    if (root.has("current")) {
                        JSONObject cur = root.getJSONObject("current");
                        curTemp = (int) Math.round(cur.optDouble("temperature_2m", 72.0));
                        mainCond = mapCondition(cur.optInt("weather_code", 0));
                    }

                    int highF = curTemp + 5;
                    int lowF = Math.max(curTemp - 12, 45);
                    List<DayForecast> weeklyForecast = new ArrayList<>();

                    if (root.has("daily")) {
                        JSONObject daily = root.getJSONObject("daily");
                        JSONArray maxArr = daily.optJSONArray("temperature_2m_max");
                        JSONArray minArr = daily.optJSONArray("temperature_2m_min");
                        JSONArray codeArr = daily.optJSONArray("weather_code");

                        if (maxArr != null && maxArr.length() > 0) {
                            highF = (int) Math.round(maxArr.getDouble(0));
                        }
                        if (minArr != null && minArr.length() > 0) {
                            lowF = (int) Math.round(minArr.getDouble(0));
                        }

                        int count = maxArr != null ? Math.min(maxArr.length(), 7) : 0;
                        Calendar cal = Calendar.getInstance();

                        for (int i = 0; i < count; i++) {
                            int dMax = (int) Math.round(maxArr.getDouble(i));
                            int dMin = minArr != null ? (int) Math.round(minArr.getDouble(i)) : dMax - 12;
                            int dCode = codeArr != null ? codeArr.optInt(i, 0) : 0;
                            String dCond = mapCondition(dCode);

                            String dayLabel;
                            if (i == 0) {
                                dayLabel = "Today";
                            } else {
                                Calendar dayCal = (Calendar) cal.clone();
                                dayCal.add(Calendar.DAY_OF_YEAR, i);
                                dayLabel = new SimpleDateFormat("EEE", Locale.US).format(dayCal.getTime());
                            }

                            weeklyForecast.add(new DayForecast(dayLabel, dMax, dMin, dCode, dCond));
                        }
                    }

                    // Save to persistent cache
                    JSONObject cacheObj = new JSONObject();
                    cacheObj.put("current_temp", curTemp);
                    cacheObj.put("condition", mainCond);
                    cacheObj.put("high", highF);
                    cacheObj.put("low", lowF);
                    JSONArray daysArr = new JSONArray();
                    for (DayForecast df : weeklyForecast) {
                        JSONObject d = new JSONObject();
                        d.put("day", df.dayName);
                        d.put("max", df.maxTempF);
                        d.put("min", df.minTempF);
                        d.put("code", df.weatherCode);
                        d.put("cond", df.condition);
                        daysArr.put(d);
                    }
                    cacheObj.put("days", daysArr);
                    sp.edit().putString(cacheKey, cacheObj.toString()).apply();

                    final int finalCurTemp = curTemp;
                    final String finalCond = mainCond;
                    final int finalHigh = highF;
                    final int finalLow = lowF;
                    final List<DayForecast> finalDays = weeklyForecast;

                    mainHandler.post(() -> callback.onWeatherLoaded(finalCurTemp, finalCond, finalHigh, finalLow, finalDays));
                }
            } catch (Exception e) {
                // Ignore network failure and keep cache
            }
        });
    }

    public static String mapCondition(int code) {
        if (code == 0) return "Sunny";
        if (code == 1 || code == 2) return "Partly Cloudy";
        if (code == 3) return "Overcast";
        if (code >= 45 && code <= 48) return "Foggy";
        if (code >= 51 && code <= 67) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Showers";
        if (code >= 95 && code <= 99) return "Thunderstorms";
        return "Clear";
    }

    public static String getStateNewsRssUrl(String stateName) {
        String clean = stateName.replace("🏛️", "").trim();
        return "https://news.google.com/rss/search?q=" + clean.replace(" ", "+") + "+news&hl=en-US&gl=US&ceid=US:en";
    }

    public static String getStateWeatherRssUrl(String stateName) {
        String clean = stateName.replace("🏛️", "").trim();
        return "https://news.google.com/rss/search?q=" + clean.replace(" ", "+") + "+weather+forecast+storm&hl=en-US&gl=US&ceid=US:en";
    }

    public interface StateRssCallback {
        void onLoaded(List<com.app.webdroid.model.NewsItem> items);
    }

    public static void fetchStateRss(String rssUrl, String sourceTitle, @NonNull StateRssCallback callback) {
        executor.execute(() -> {
            List<com.app.webdroid.model.NewsItem> results = new ArrayList<>();
            try {
                java.net.URL url = new java.net.URL(rssUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                if (conn.getResponseCode() == 200) {
                    com.app.webdroid.util.RssParser parser = new com.app.webdroid.util.RssParser();
                    List<com.app.webdroid.model.NewsItem> parsed = parser.parseNews(conn.getInputStream(), sourceTitle);
                    if (parsed != null) {
                        results.addAll(parsed);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mainHandler.post(() -> callback.onLoaded(results));
        });
    }
}
