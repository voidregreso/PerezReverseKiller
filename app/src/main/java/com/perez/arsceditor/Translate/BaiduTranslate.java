/**
 *  Copyright 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.perez.arsceditor.Translate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

public class BaiduTranslate {

    
    private String str = null, fromString = "auto", toString = "auto";
    
    private String result = "";

    
    public BaiduTranslate(String fromString, String toString) {
        
        this.fromString = fromString;
        
        this.toString = toString;
    }

    /**
     *  str:
     */
    public String getResult(String str) throws IOException, JSONException {
        
        this.str = str;
        
        doTranslate();
        
        return result;
    }

    
    public void doTranslate() throws IOException, JSONException {
        
        String str_utf = URLEncoder.encode(str, "UTF-8");
        
        String str_url = "http://openapi.baidu.com/public/2.0/bmt/translate?client_id=GOr7jiTs5hiQvkHqDNg4KSTV&q="
                         + str_utf + "&from=" + fromString + "&to=" + toString;

        URL url_word = new URL(str_url);
        
        URLConnection connection = (URLConnection) url_word.openConnection();
        
        InputStream is = connection.getInputStream();
        
        InputStreamReader isr = new InputStreamReader(is);
        
        BufferedReader br = new BufferedReader(isr);
        
        String line;
        
        StringBuilder sBuilder = new StringBuilder();
        
        while((line = br.readLine()) != null) {
            
            sBuilder.append(line);
        }
        /**
         * 
         */
        JSONTokener jtk = new JSONTokener(sBuilder.toString());
        JSONObject jObject = (JSONObject) jtk.nextValue();
        JSONArray jArray = jObject.getJSONArray("trans_result");
        Log.i("TAG", url_word.toString());
        Log.i("TAG", jObject.toString());
        JSONObject sub_jObject_1 = jArray.getJSONObject(0);
        
        result = sub_jObject_1.getString("dst");
        br.close();
        isr.close();
        is.close();
    }
}