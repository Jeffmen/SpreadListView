package com.example.spreadlistview.demo;

import java.util.List;

import com.example.spreadlistview.R;
import com.example.spreadlistview.demo.MainActivity.Data;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SpreadListViewAdapter extends BaseAdapter{

	private Context mContext;
    private LayoutInflater inflater;
    private static TypedArray sColors;
    private static int sDefaultColor;
    private int mResource;  
    private List<Data> mDataItem;
    
	public SpreadListViewAdapter(Context context, int resource, List<Data> objects) {
		super();
		mContext = context;
		mResource = resource;
		this.mDataItem = objects;
        sDefaultColor = mContext.getResources().getColor(R.color.list_item_default_color);
        sColors = mContext.getResources().obtainTypedArray(R.array.list_item_colors);
        inflater = LayoutInflater.from(mContext);
	}  
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
        if(convertView == null) {    
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(mResource, null);
    		viewHolder.llyt_show = (LinearLayout) convertView.findViewById(R.id.llyt_show);
    		viewHolder.llyt_hide = (LinearLayout) convertView.findViewById(R.id.llyt_hide);
    		viewHolder.txt_show = (TextView) convertView.findViewById(R.id.txt_show);
    		viewHolder.txt_hide = (TextView) convertView.findViewById(R.id.txt_hide);
    		convertView.setTag(viewHolder);
        } else {   
        	viewHolder = (ViewHolder) convertView.getTag();   
        }
        Data data = (Data)getItem(position);
        //convertView.setBackgroundColor(pickColor(data.showStr));
		viewHolder.llyt_show.setBackgroundColor(pickColor(data.showStr));
		//viewHolder.llyt_show.setAlpha(0.8F);
		viewHolder.llyt_hide.setBackgroundColor(pickColor(data.showStr));
        viewHolder.txt_show.setText(data.showStr);
        viewHolder.txt_hide.setText(data.hideStr);
        
		return convertView;
	}
	
    private int pickColor(final String identifier) {
        if (TextUtils.isEmpty(identifier)) {
            return sDefaultColor;
        }
        final int color = Math.abs(identifier.hashCode()) % sColors.length();
        return sColors.getColor(color, sDefaultColor);
    }
    
	public List<Data> getData() {
		return mDataItem;
	}

	@Override
	public int getCount() {
		if(mDataItem == null){
			return 0;
		}
		else {
			return mDataItem.size();
		}
	}

	@Override
	public Object getItem(int position) {
		if(mDataItem == null){
			return null;
		}
		else {
			return mDataItem.get(position);
		}
	}

	@Override
	public long getItemId(int position) {
		return position;
	} 
	
	final static class ViewHolder {
		LinearLayout llyt_show;
		LinearLayout llyt_hide;
		TextView txt_hide;
		TextView txt_show;
	}
	
}

