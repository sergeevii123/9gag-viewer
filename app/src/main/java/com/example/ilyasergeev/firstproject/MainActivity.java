package com.example.ilyasergeev.firstproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.List;


public class MainActivity extends ActionBarActivity {
    ArrayList<String> theList;
    ArrayList<Bitmap> images;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ArrayList<String> arrayList = new ArrayList<>();
        /*for(int i =0; i < 20; i++){
            arrayList.add("a"+i);
        }*/
        final Context s = this;

        AsyncTask <ArrayList<String>, Void, ArrayList<String>> asyncTask = new AsyncTask<ArrayList<String>, Void, ArrayList<String>>() {
            private HttpClient httpClient;
            private HttpGet httpGet;


            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                httpClient = new DefaultHttpClient();
                httpGet = new HttpGet("http://infinigag.eu01.aws.af.cm/hot/0");
            }

            @Override
            protected ArrayList<String> doInBackground(ArrayList<String>... params) {
                theList = params[0];
                images = new ArrayList<>();
                try {
                    HttpResponse response = httpClient.execute(httpGet);

                    HttpEntity entity = response.getEntity();
                    BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(entity.getContent()));
                    String line = "";
                    String result = "";
                    while((line = bufferedReader.readLine()) != null)
                        result += line;
                    JSONObject jsonResponse = new JSONObject(result);
                    JSONArray forecastList = jsonResponse.optJSONArray("data");
                    for(int i = theList.size(); i < forecastList.length(); i++){
                        JSONObject forecast = forecastList.getJSONObject(i);
                        //long date = forecast.optLong("dt");
                        String url = forecast.optJSONObject("images").optString("large");
                        theList.add(url);
                        System.out.println(theList.size());
                        HttpURLConnection urlConnection = (HttpURLConnection)new URL(url).openConnection();
                        InputStream inputStream = new URL(url).openConnection().getInputStream();
                        if (inputStream != null) {
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            if(bitmap != null )images.add(bitmap);
                        }

                        //new ImageDownloaderTask((ImageView) findViewById(R.id.rowImageView)).doInBackground(theList.toArray(new String[theList.size()]));
                        //JSONObject artistName = forecast.optJSONObject("main");
                        //Double temp = artistName.optDouble("temp");
                        //temp-= 273;
                        //NumberFormat formatter = new DecimalFormat("#0.00");
                        //arrayList.add(new SimpleDateFormat("d/m/y").format(new Date(date)) + " "+temp);
                        //theList.add(new SimpleDateFormat("EEE, MMM d").format(new Date(date*1000)) + " t: "+ formatter.format(temp));
                    }
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return  null;
            }


            protected void onPostExecute(ArrayList<String> result) {
                //ArrayAdapter adapter = new ArrayAdapter(s, R.layout.simplerow, R.id.rowTextView,theList);
                //CustomListAdapter customListAdapter = new CustomListAdapter(s,theList);
                final ListView listView = (ListView) findViewById(R.id.listView);
                listView.setAdapter(new CustomListAdapter(s, images));
                System.out.println("here111");
                //((ListView) findViewById(R.id.listView)).setAdapter(customListAdapter);

            }
        };

        asyncTask.execute(arrayList);
        //CustomListAdapter customListAdapter = new CustomListAdapter(this,theList);
        System.out.println("here111");
        //((ListView) findViewById(R.id.listView)).setAdapter(customListAdapter);

        //ArrayAdapter adapter = new ArrayAdapter(this, R.layout.simplerow, R.id.rowTextView,arrayList);
        //((ListView)findViewById(R.id.listView)).setAdapter(adapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*class MyTask extends AsyncTask<ArrayList<String>, Void, ArrayList<String>> {
        private HttpClient httpClient;
        private HttpGet httpGet;
        private ArrayList<String> theList;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            httpClient = new DefaultHttpClient();
            httpGet = new HttpGet("http://api.openweathermap.org/data/2.5/forecast?q=Moscow,ru");
        }

        @Override
        protected ArrayList<String> doInBackground(ArrayList<String>... params) {
            theList = params[0];
            ArrayList<String> arrayList = null;
            try {
                HttpResponse response = httpClient.execute(httpGet);
                HttpEntity entity = response.getEntity();
                JSONObject jsonResponse = new JSONObject(entity.getContent().toString());
                JSONArray forecastList = jsonResponse.optJSONArray("list");
                arrayList = new ArrayList<>();
                for(int i = 0; i < forecastList.length(); i++){
                    JSONObject forecast = forecastList.getJSONObject(i);
                    long date = forecast.optLong("dt");
                    String dateString = forecast.getString("dt_tx");
                    JSONObject artistName = forecast.optJSONObject("main");
                    int temp = forecast.optInt("temp");
                    arrayList.add(dateString+ " "+temp);
                    theList.add(dateString+ " "+temp);
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return  null;
        }


        protected void onPostExecute(ArrayList<String> result) {
            httpClient.getConnectionManager().closeExpiredConnections();

        }
    }*/

}
