package com.metinkale.prayerapp.compass;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import com.metinkale.prayer.R;
import com.metinkale.prayerapp.custom.Geocoder;
import com.metinkale.prayerapp.custom.Geocoder.Response;
import com.metinkale.prayerapp.settings.Prefs;

import java.util.List;

public class LocationPicker extends Activity implements TextWatcher, OnItemClickListener
{
    private EditText mCity;
    private ArrayAdapter<String> mAdapter;
    private Thread mThread;
    private List<Response> mAddresses;
    private Runnable mSearch = new Runnable()
    {

        @Override
        public void run()
        {
            try
            {
                Thread.sleep(500);
            } catch(InterruptedException e)
            {
                //expected to happen sometimes
            }

            String query = mCity.getText().toString();

            mAddresses = Geocoder.from(query, 5, Prefs.getLanguage());

            if(mAddresses != null) LocationPicker.this.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    mAdapter.clear();
                    for(Response a : mAddresses)
                    {

                        mAdapter.add(a.display_name);
                    }
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.compass_location);

        mCity = (EditText) findViewById(R.id.location);

        ListView list = (ListView) findViewById(R.id.listView);
        list.setOnItemClickListener(this);

        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1);

        list.setAdapter(mAdapter);

        mCity.addTextChangedListener(this);
    }

    @Override
    public void afterTextChanged(Editable arg0)
    {
        if(mThread != null && mThread.isAlive())
        {
            mThread.interrupt();
        }
        mThread = new Thread(mSearch);
        mThread.start();
    }

    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
    {

    }

    @Override
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
    {

    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3)
    {
        if(mAddresses == null) return;
        Response a = mAddresses.get(pos);
        Prefs.setCompassPos((float) a.lat, (float) a.lon);

        finish();

    }

}