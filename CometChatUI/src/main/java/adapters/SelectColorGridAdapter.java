package adapters;


import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;


import java.util.ArrayList;

import cometchat.inscripts.com.readyui.R;
import pojo.ColorPojo;

public class SelectColorGridAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<ColorPojo> colorList;


    public SelectColorGridAdapter(Context c, ArrayList<ColorPojo> colorList) {
        mContext = c;
        this.colorList = colorList;
    }


    @Override
    public int getCount() {
        return colorList.size();
    }

    @Override
    public Object getItem(int i) {
        return colorList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int i, View convertView, ViewGroup viewGroup) {

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.cc_select_color_grid_item, null);
            final ImageView imageView = (ImageView)convertView.findViewById(R.id.grid_image);
            imageView.getBackground().setColorFilter(Color.parseColor(((ColorPojo)getItem(i)).getColor()), PorterDuff.Mode.SRC_ATOP);

            if(((ColorPojo)getItem(i)).isSelected()){
                imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.cc_ic_check));
            }else{
                imageView.setImageDrawable(null);
            }

        return convertView;
    }
}
