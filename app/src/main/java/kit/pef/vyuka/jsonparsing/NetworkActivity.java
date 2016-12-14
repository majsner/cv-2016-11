package kit.pef.vyuka.jsonparsing;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NetworkActivity extends Activity implements IAsyncResponse {

    final String LOG_TAG = "test: ";
    List<String> weekForecast;

    public String post = "Prague";
    public String format = "json";
    public String units = "metric";
    public String apikey = "fd29fbbde4a5259c3ffe7e593e1f9620";
    public int numDays = 10;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new MyTask().execute();
    }

    // kontrola zda je telefon připojený
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    @Override
    public void processFinish(String[] strings) {

        weekForecast = new ArrayList<>(Arrays.asList(strings));
        populateListView();
    }


    // asynchronní třída
    private class MyTask extends AsyncTask<Void, Void, String[]> {
        IAsyncResponse delegate = (IAsyncResponse) NetworkActivity.this;

        @Override
        protected String[] doInBackground(Void... params) {
            try {
                return loadJSON();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] strings) {
            super.onPostExecute(strings);
            delegate.processFinish(strings);
        }
    }

    HttpURLConnection urlConnection;
    BufferedReader reader;

    // stažení JSON, a volání parsovací metody
    private String[] loadJSON() throws IOException {
        InputStream inputStream;

        if (!isOnline()) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "Neni pripojeni - ukonceni!", Toast.LENGTH_SHORT).show();
                }
            });
            return null;
        }

        String forecastJsonStr = null;

        try {

            // Nově chtějí API key a je omezen počet požadavků - ne vždy se dá dostat json
            //URL url = new URL(buildURL().toString());
            Log.v(LOG_TAG, "Built URI " + buildURL().toString());
            // jen pro ukázku, jinak je to k ničemu

            URL url = new URL("http://kit.pef.czu.cz/files/storage/weather.txt");

            inputStream = download(url);

            if (inputStream == null) {
                // inputstream je prazdny - chyba - return null
                return null;
            }

            forecastJsonStr = convertIS(inputStream);

            if (forecastJsonStr == null) {
                return null;
            }

            Log.v(LOG_TAG, "Forecast string: " + forecastJsonStr);

        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error ", e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        try {
            WeatherJsonParsing weatherJsonParsing = new WeatherJsonParsing();
            return weatherJsonParsing.getWeatherDataFromJson(forecastJsonStr, numDays);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        return null;
    }

    private InputStream download(URL url) {
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            return urlConnection.getInputStream();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            return null;
        }

    }

    private String convertIS(InputStream inputStream) {
//        StringBuffer buffer = new StringBuffer();
        StringBuilder buffer = new StringBuilder();
        reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                // Pridanim dalsiho radku, se funkcnost JSONu nezmeni, ale v pripade debuggovani
                // to o mnoho zjednodussi praci
                String string = line + "\n";
                buffer.append(string);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            return null;
        }
        if (buffer.length() == 0) {
            return null;
        }
        return buffer.toString();
    }

    // pro zajimavost, ukazka "staveni" URL, moznost dalsiho vyuziti na zaklade nastaveni/vstupnich parametru
    private Uri buildURL() {
        final String FORECAST_BASE_URL =
                "http://api.openweathermap.org/data/2.5/forecast/daily?";
        final String QUERY_PARAM = "q";
        final String FORMAT_PARAM = "mode";
        final String UNITS_PARAM = "units";
        final String DAYS_PARAM = "cnt";
        final String APIKEY_PARAM = "APPID";

        return Uri.parse(FORECAST_BASE_URL).buildUpon()
                .appendQueryParameter(QUERY_PARAM, post)
                .appendQueryParameter(FORMAT_PARAM, format)
                .appendQueryParameter(UNITS_PARAM, units)
                .appendQueryParameter(APIKEY_PARAM, apikey)
                .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                .build();
    }


    // naplnovani hlavniho ListView pomoci ArrayAdapteru
    // oproti parsovani XML neni potreba pouzivat dalsi tridu, protoze zde mame pouze 1 textview
    private void populateListView() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.row, R.id.forecast, weekForecast);
        ListView list = (ListView) findViewById(R.id.listView);
        list.setAdapter(adapter);
    }
}

