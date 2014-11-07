package com.yv.techgee.android;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.yv.techgee.android.listener.ListViewSwipeGestureListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final ListView listView = (ListView) findViewById(R.id.list_view);
        String[] values = new String[]{"Android", "iPhone", "WindowsMobile",
                "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
                "Linux", "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux",
                "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux", "OS/2",
                "Android", "iPhone", "WindowsMobile"};

        final ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < values.length; ++i) {
            list.add(values[i]);
        }
        listView.setOnTouchListener(new ListViewSwipeGestureListener(listView, swipeListener, MainActivity.this));
        final MySimpleArrayAdapter adapter = new MySimpleArrayAdapter(this, list);
        listView.setAdapter(adapter);
    }

    ListViewSwipeGestureListener.TouchCallbacks swipeListener = new ListViewSwipeGestureListener.TouchCallbacks() {


        @Override
        public boolean canAction1(int position) {

            return position % 2 == 0 ? true : false;
        }

        @Override
        public boolean canAction2(int position) {
            return true;
        }

        @Override
        public void onAction1Clicked(int position) {
            Toast.makeText(MainActivity.this, "action1 clicked", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onAction2Clicked(int position) {
            Toast.makeText(MainActivity.this, "action2 clicked", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void OnClickListView(int position) {


        }

    };

    private class MySimpleArrayAdapter extends BaseAdapter {
        private final Context context;
        private final List<String> values;

        public MySimpleArrayAdapter(Context context, List<String> values) {
            this.context = context;
            this.values = values;
        }

        @Override
        public int getCount() {
            return values.size();
        }

        @Override
        public Object getItem(int position) {
            return values.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.list_item_action_item_container, parent, false);
            TextView textView = (TextView) rowView.findViewById(R.id.text_view);
            textView.setText(values.get(position));


            return rowView;
        }
    }
}
