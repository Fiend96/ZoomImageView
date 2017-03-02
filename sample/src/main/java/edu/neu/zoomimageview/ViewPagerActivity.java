package edu.neu.zoomimageview;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import edu.neu.library.ZoomImageView;

public class ViewPagerActivity extends AppCompatActivity {
    //记录上一个位置
    private int currentPosition;
    private ZoomImageView[] views;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pager);
        final ViewPager vp = (ViewPager) findViewById(R.id.vp);
        views = new ZoomImageView[4];
        vp.setAdapter(new MyPagerAdapter(getApplicationContext(), views));
        vp.setPageMargin(10);
        vp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Log.d("fiend", "position:" + position);
                ZoomImageView zoomImageView = views[currentPosition];
                //复原
                zoomImageView.restore();
                currentPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    static class MyPagerAdapter extends PagerAdapter {
        private Context mContext;
        private ZoomImageView[] views;

        public MyPagerAdapter(Context context, ZoomImageView[] views) {
            mContext = context;
            this.views = views;
        }

        @Override
        public int getCount() {
            return views.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if(views[position] == null){
                views[position] = new ZoomImageView(mContext);
            }
            ZoomImageView imageView = views[position];
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
