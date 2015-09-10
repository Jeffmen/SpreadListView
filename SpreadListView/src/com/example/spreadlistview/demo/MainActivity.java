package com.example.spreadlistview.demo;

import java.util.ArrayList;
import java.util.List;

import com.example.spreadlistview.R;
import com.example.spreadlistview.view.SpreadListView;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;


public class MainActivity extends Activity {

	private SpreadListView spreadListView;
	private SpreadListViewAdapter adapter;
	private List<Data> dataList;
	private int count = 5;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dataList = new ArrayList<Data>();
        spreadListView = (SpreadListView) findViewById(R.id.spreadListView);
        spreadListView.setLastView(LayoutInflater.from(this).inflate(R.layout.listview_last_item, null));
        spreadListView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
	    		Data item = (Data)adapter.getItem(position);
				Toast.makeText(MainActivity.this, item.showStr, Toast.LENGTH_SHORT).show();
		    }
		});
        spreadListView.setLastItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
	    		Data item = new Data();
	    		count++;
	    		item.showStr = "Show "+count;
	    		item.hideStr = "Hide "+count;
	    		adapter.getData().add(item);
				adapter.notifyDataSetChanged();
			}
	    });
        adapter = new SpreadListViewAdapter(MainActivity.this, R.layout.listview_item, getData());
        spreadListView.setAdapter(adapter);
    }

    private List<Data> getData(){
    	for(int i=0; i<count; i++){
    		Data item = new Data();
    		item.showStr = "Show "+(i+1);
    		item.hideStr = "Hide "+(i+1);
        	dataList.add(item);
    	}
    	return dataList;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
	public class Data{
		public Data(){
		}
		public String showStr;
		public String hideStr;
	}
}
