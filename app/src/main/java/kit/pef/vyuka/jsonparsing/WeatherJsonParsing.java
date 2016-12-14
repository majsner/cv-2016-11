package kit.pef.vyuka.jsonparsing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WeatherJsonParsing {

    // formatovani teplotniho minima a maxima do pozadovaneho formatu
    private String formatHighLows(double high, double low) {

        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        return roundedHigh + "/" + roundedLow;
    }

    String[] getWeatherDataFromJson(String forecastJsonStr, int numDays) throws JSONException {
        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray("list");

        String[] resultStrs = new String[numDays];
        for (int i = 0; i < weatherArray.length(); i++) {
            // promenne pro finalni vystup
            String day;
            String highAndLow;

            // ziskani JSON objektu reprezentujici den
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // Cas se vraci jako timestamp; "1400356800"
            // je potreba tedy cas prevest do citelneho formatu
            day = this.getDate(dayForecast.getLong("dt"));

            // Teplota je v child objektu nazyvaneho "temp"
            JSONObject temperatureObject = dayForecast.getJSONObject("temp");
            double high = temperatureObject.getDouble("max");
            double low = temperatureObject.getDouble("min");

            highAndLow = formatHighLows(high, low);

            resultStrs[i] = day + " - " + highAndLow;

        }

        return resultStrs;

    }

    private String getDate(long timeStamp) {
        SimpleDateFormat format = new SimpleDateFormat("dd. MM. yyyy");
        // timestamp je v sekundách, potřebujeme milisekundy
        String date = format.format(new Date(timeStamp * 1000)).toString();
        return date;

    }

}




