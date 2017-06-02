package ru.jorik.infostends;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * Created by 111 on 06.03.2017.
 */

public class RouteBuilder extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... coords) {
        URL url = null;
        HttpURLConnection connection = null;
        StringBuffer buffer = null;

        //params
        String from = coords[0];
        String to = coords[1];
        String key = "AIzaSyD-IqafbF93Ca-mGZqA0vpXEl0xDx5z07Y";


        try {
            url = new URL("https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin="+from+
                    "&destination="+to+
                    "&key="+key
            );
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("origin", from);
            connection.setRequestProperty("destination", to);
            connection.setRequestProperty("key", key);
            connection.connect();

            String writeBytes = "origin";
            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
            dos.writeBytes("");
            dos.flush();
            dos.close();


            BufferedReader bufferedReader;
            try{
                bufferedReader = new BufferedReader(new InputStreamReader(
                        new GZIPInputStream(new DataInputStream(connection.getInputStream()))));
            } catch (IOException e){
                bufferedReader = new BufferedReader(new InputStreamReader(new DataInputStream(connection.getInputStream())));
            }

//            InputStream inputStream = connection.getInputStream();
//            DataInputStream dis = new DataInputStream(inputStream);
//            GZIPInputStream gson = new GZIPInputStream(dis);
//            InputStreamReader inputStreamReader = new InputStreamReader(gson);
//            bufferedReader = new BufferedReader(inputStreamReader);

            buffer = new StringBuffer();
            String temp;
            while ((temp = bufferedReader.readLine()) != null){
                buffer.append(temp).append('\n');
            }

            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }



        String returnString = new String(buffer);
        return returnString;
    }

}
