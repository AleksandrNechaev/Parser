package ua.nechaev.parss;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import java.util.StringTokenizer;
import java.util.TreeSet;

import android.widget.AdapterView.OnItemSelectedListener;

public class MainActivity extends AppCompatActivity{

    List<String> countriesList = new ArrayList<>();
    HashMap<String, ArrayList<String>> mapOfCities = new HashMap<>();
    HashMap<String, String> mapOfImages = new HashMap<>();

    StringBuilder citiesString = new StringBuilder();
    String selectedCountry = "";
    final String urlJson = "https://raw.githubusercontent.com/David-Haim/CountriesToCitiesJSON/master/countriesToCities.json";
    final String sqlCreate = "CREATE TABLE IF NOT EXISTS listOfCountriesAndCities (country TEXT, city TEXT);";
    final String sqlSelectCountry = "SELECT country FROM listOfCountriesAndCities;";

    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new ParseAsyncTask().execute();
       }

    private class ParseAsyncTask extends AsyncTask<Void, Void, String> {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String resultJson = "";

       @Override
        protected String doInBackground(Void... params) {
            try {
                URL url = new URL(urlJson);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder stringBuilder = new StringBuilder();

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                resultJson = stringBuilder.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return resultJson;
        }

        @Override
        protected void onPostExecute(final String strJson) {
            super.onPostExecute(strJson);

            db = getBaseContext().openOrCreateDatabase("country.db", MODE_PRIVATE, null);
            String sqlInsert = "";
            try {
                JSONObject data = new JSONObject(strJson);
                db.execSQL(sqlCreate);
                for (Iterator<String> countries = data.keys(); countries.hasNext();) {
                    String country = countries.next();
                    countriesList.add(country);
                    String citesToString = data.getJSONArray(country).toString().replace("[", "")
                            .replace("]", "").replaceAll("\"", "").replaceAll("'", "\''");
                    sqlInsert = "INSERT OR REPLACE INTO listOfCountriesAndCities(country, city) VALUES(\'"
                            + country + "\',\'" + citesToString +"\');";
                    db.execSQL(sqlInsert);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            TreeSet<String> countryListFromDb = new TreeSet<>();

            Cursor query = db.rawQuery(sqlSelectCountry, null);
            if(query.moveToFirst()) {
                do{
                    String country = query.getString(query.getColumnIndex("country"));
                    countryListFromDb.add(country);
                }while(query.moveToNext());
            }
            query.close();

            ArrayList<String> listAdapter = new ArrayList<>(countryListFromDb);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, listAdapter);
            final Spinner items = (Spinner) findViewById(R.id.spinner);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            items.setAdapter(adapter);

            items.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedCountry = items.getItemAtPosition(position).toString();
                    citiesString.delete(0,citiesString.length());
                    String sqlSelectCity = "SELECT * FROM listOfCountriesAndCities WHERE country='"+selectedCountry+"\';";
                    Cursor query = db.rawQuery(sqlSelectCity, null);

                    if(query.moveToFirst()) {
                        do{
                            String city = query.getString(query.getColumnIndex("city"));
                            citiesString.append(city);
                        }while(query.moveToNext());
                    }
                    query.close();

                    TreeSet<String> citySet = new TreeSet<>();
                    final ListView listView = (ListView)findViewById(R.id.listView);
                    StringTokenizer stringTokenizer = new StringTokenizer(citiesString.toString(), ",");
                    while(stringTokenizer.hasMoreTokens()) {
                        citySet.add(stringTokenizer.nextToken());
                    }

                    ArrayList<String> adapterCityList = new ArrayList<>(citySet);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, adapterCityList);

                    listView.setAdapter(adapter);

                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            String a = " http://api.geonames.org/wikipediaSearch?q="+listView.getItemAtPosition(position).toString().replaceAll("'","")+"&maxRows=5&username=presis";
                            final String KEY_ENTRY = "entry";
                            final String KEY_TITLE = "title";
                            final String KEY_SUMMARY = "summary";
                            final String KEY_IMAGE = "thumbnailImg";
                            final String KEY_LATITUDE= "lat";
                            final String KEY_LONGITUDE = "lng";
                            final String KEY_WIKI_URL = "wikipediaUrl";

                            try {
                                XMLParser xmlParser = new XMLParser();
                                String xml = xmlParser.getXmlFromUrl(a);
                                Document doc = xmlParser.getDomElement(xml);
                                mapOfCities.clear();
                                NodeList nl = doc.getElementsByTagName(KEY_ENTRY);
                                for(int i = 0; i < nl.getLength(); i++) {
                                    ArrayList<String> findedPlaces = new ArrayList<>();
                                    Element e = (Element) nl.item(i);
                                    String title = xmlParser.getValue(e, KEY_TITLE);
                                    String summary = xmlParser.getValue(e, KEY_SUMMARY);
                                    String image = xmlParser.getValue(e, KEY_IMAGE);
                                    String latitude = xmlParser.getValue(e, KEY_LATITUDE);
                                    String longitude = xmlParser.getValue(e, KEY_LONGITUDE);
                                    String wikiUrl = xmlParser.getValue(e, KEY_WIKI_URL);
                                    findedPlaces.add(summary);
                                    findedPlaces.add(latitude);
                                    findedPlaces.add(longitude);
                                    findedPlaces.add(wikiUrl);
                                    mapOfCities.put(title,findedPlaces);
                                    mapOfImages.put(title,image);
                                }

                                Intent intent = new Intent(MainActivity.this, CitiesActivity.class);
                                intent.putExtra("mapOfCities", mapOfCities);
                                intent.putExtra("mapOfImages", mapOfImages);
                                startActivity(intent);
                            } catch (NullPointerException e) {
                                Intent intent = new Intent(MainActivity.this, CitiesActivity.class);
                                mapOfCities.clear();
                                intent.putExtra("mapOfCities", mapOfCities);
                                startActivity(intent);
                            }
                        }
                    });
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

    }
}
