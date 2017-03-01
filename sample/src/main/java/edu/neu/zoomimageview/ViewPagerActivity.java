package edu.neu.zoomimageview;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;

import edu.neu.library.ZoomImageView;

public class ViewPagerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pager);
        ViewPager vp = (ViewPager) findViewById(R.id.vp);
        vp.setAdapter(new MyPagerAdapter(getApplicationContext()));
        vp.setPageMargin(10);
    }

    static class MyPagerAdapter extends PagerAdapter {
        private Context mContext;

        public MyPagerAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            ZoomImageView imageView = new ZoomImageView(mContext);
            imageView.setImageResource(R.mipmap.photo);
            container.addView(imageView);
            if (position == 0) {
                imageView.setInterruptLeft(false);
            }
            if (position == getCount() - 1) {
                imageView.setInterruptRight(false);
            }
            return imageView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (object != null) {
                container.removeView((View) object);
            }
        }
    }
}
