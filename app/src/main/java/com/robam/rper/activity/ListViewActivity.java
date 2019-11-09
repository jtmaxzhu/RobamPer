package com.robam.rper.activity;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


import com.robam.rper.R;
import com.robam.rper.annotation.EntryActivity;

import java.util.ArrayList;
import java.util.List;


//@EntryActivity(icon = R.drawable.xn, name = "列表演示1", index = 3)
public class ListViewActivity extends AppCompatActivity {

    private List<Fruit> fruitList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);
        initFruits();
        FruitAdapter fruitAdapter = new FruitAdapter(this, R.layout.fruit_item, fruitList);
        ListView listView = findViewById(R.id.list_view);
        listView.setAdapter(fruitAdapter);
    }

    private void initFruits(){
        for (int i=0; i<5; i++){
            Fruit apple = new Fruit("Apple", R.drawable.apple_pic);
            Fruit banana = new Fruit("Banana", R.drawable.banana_pic);
            Fruit orange = new Fruit("Orange", R.drawable.orange_pic);
            Fruit watermelon = new Fruit("Watermelon", R.drawable.watermelon_pic);
            Fruit pear = new Fruit("Pear", R.drawable.apple_pic);
            fruitList.add(apple);
            fruitList.add(banana);
            fruitList.add(orange);
            fruitList.add(watermelon);
            fruitList.add(pear);
        }
    }


    public static class Fruit{
        private String name;

        private int imageId;

        public Fruit(String name, int imageId) {
            this.name = name;
            this.imageId = imageId;
        }

        public String getName() {
            return name;
        }

        public int getImageId() {
            return imageId;
        }

    }

    public class FruitAdapter extends ArrayAdapter<Fruit> {

        private int resourceId;

        public FruitAdapter(Context context, int textViewResourceId, List<Fruit> objects) {
            super(context, textViewResourceId, objects);
            resourceId = textViewResourceId;
        }


        @Override
        public View getView(int position, View convertView,  ViewGroup parent) {
            Fruit fruit = getItem(position);
            View view;
            ViewHolder viewHolder;
            if (convertView == null){
                view = LayoutInflater.from(getContext()).inflate(resourceId, parent,false);
                viewHolder = new ViewHolder();
                viewHolder.fruitImage = view.findViewById(R.id.fruit_image);
                viewHolder.fruitName = view.findViewById(R.id.fruit_name);
                view.setTag(viewHolder);
            }else{
                view = convertView;
                viewHolder = (ViewHolder) view.getTag();
            }
            viewHolder.fruitImage.setImageResource(fruit.getImageId());
            viewHolder.fruitName.setText(fruit.getName());
            return view;
        }
    }

    public class ViewHolder{
        ImageView fruitImage;
        TextView fruitName;
    }
}
