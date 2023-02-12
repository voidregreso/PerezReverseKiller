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
package com.perez.arsceditor;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.widget.EditText;
import com.perez.revkiller.R;


@SuppressWarnings("deprecation")
public class SearchString {
    
    private ProgressDialog mdialog;
    
    private Context mContext;
    
    private List<String> listitems;
    
    private List<String> listresult = new ArrayList<String>();
    
    private List<Integer> listposition = new ArrayList<Integer>();

    
    public SearchString(Context context, List<String> listitems) {
        
        mContext = context;
        this.listitems = listitems;
    }

    
    public void search() {
        final EditText name = new EditText(mContext);
        name.setHint(mContext.getString(R.string.search_hint));
        Dialog alertDialog = new AlertDialog.Builder(mContext).setTitle(R.string.search).setView(name)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new AsyncLoader().execute(name.getText().toString()); 
            }
        }).setNegativeButton(R.string.cancel, null).create();
        alertDialog.show();
    }

    
    // AsyncTask
    class AsyncLoader extends AsyncTask<String, Void, Void> {
        @Override
        
        protected void onPreExecute() {
            
            mdialog = new ProgressDialog(mContext);
            
            mdialog.setMessage(mContext.getString(R.string.searching));
            
            mdialog.setCancelable(false);
            
            mdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            
            mdialog.show();
        }

        
        protected Void doInBackground(String... params) {
            for(String result : listitems) { 
                if(result.indexOf(params[0]) != -1) {
                    listresult.add(result);
                    listposition.add(listitems.indexOf(result));
                }
            }
            return null;
        }

        
        protected void onPostExecute(Void result) {
            mdialog.dismiss();
            new AlertDialog.Builder(mContext).setTitle(R.string.search_result)
            .setItems((String[]) listresult.toArray(new String[listresult.size()]),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    
                    ((ArscActivity) mContext).stringListView.setSelection(listposition.get(arg1));
                }
            })
            .create().show();
        }
    }

}
