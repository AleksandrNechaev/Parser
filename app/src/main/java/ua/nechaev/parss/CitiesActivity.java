package ua.nechaev.parss;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class CitiesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cities);
        final ArrayList<String> titles = new ArrayList<>();
        final ArrayList<String> intentList = new ArrayList<>();
        final ListView listView = (ListView)findViewById(R.id.list);

        final HashMap<String, ArrayList<String>> mapOfCities = (HashMap<String, ArrayList<String>>)getIntent().getSerializableExtra("mapOfCities");
        final HashMap<String, String> mapOfImages = (HashMap<String, String>)getIntent().getSerializableExtra("mapOfImages");

        if(!mapOfCities.isEmpty()) {
            final TreeMap<String, ArrayList<String>> mapTitles = new TreeMap<>();
            mapTitles.putAll(mapOfCities);
            titles.clear();
            for(String key : mapTitles.keySet()) {
                titles.add(key);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    intentList.clear();
                    intentList.addAll(mapTitles.get(listView.getItemAtPosition(position)));
                    String image = mapOfImages.get(listView.getItemAtPosition(position));
                    Intent intent = new Intent(CitiesActivity.this, InformationActivity.class);
                    intent.putExtra("intentList", intentList);
                    intent.putExtra("image", image);
                    startActivity(intent);
                }
            });
        }
        else {
            TextView textView = (TextView)findViewById(R.id.textView2);
            textView.setText("No information about this city");
        }



    }
}
