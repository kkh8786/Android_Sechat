/**
 * ****************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * *****************************************************************************
 */
package net.stacksmashing.sechat;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import net.stacksmashing.sechat.db.Message;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import uk.co.senab.photoview.PhotoView;


public class PhotosViewPagerActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor>, ViewPager.OnPageChangeListener {

    private static final String PHOTO_PAGER_INDEX = "photo_index";
    private static final int MIDDLE_PAGE = 1;

    @InjectView(R.id.activity_photos_view_pager)
    ViewPager viewPager;

    private int currentPage = 0;
    private int offset = 0;
    private PhotoView views[] = new PhotoView[3];
    private List<File> files = new ArrayList<>();

    public static Intent intent(Context context, int photoIndex) {
        Intent intent = new Intent(context, PhotosViewPagerActivity.class);
        intent.putExtra(PHOTO_PAGER_INDEX, photoIndex);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos_view_pager);
        ButterKnife.inject(this);

        for (int i = 0; i < views.length; i++) {
            views[i] = new PhotoView(this);
        }

        PhotoPagerAdapter pagerAdapter = new PhotoPagerAdapter(views);

        viewPager.setAdapter(pagerAdapter);
        viewPager.setOnPageChangeListener(this);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return MessageArrayAdapter.createPhotoCursorLoader(this, Preferences.getLastChatId(this));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        data.moveToFirst();
        do {
            files.add(Message.DAO.cursorToObject(data).getFile(this));
        } while (data.moveToNext());
        setOffset(getIntent().getExtras().getInt(PHOTO_PAGER_INDEX, 0) - 1);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        currentPage = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state == ViewPager.SCROLL_STATE_IDLE) {
            if (currentPage < MIDDLE_PAGE) {
                setOffset(offset - 1);
            }
            else if (currentPage > MIDDLE_PAGE) {
                setOffset(offset + 1);
            }
        }
    }

    private void setOffset(int offset) {
        this.offset = offset % files.size();
        if (this.offset < 0) {
            this.offset += files.size();
        }

        viewPager.setCurrentItem(MIDDLE_PAGE, false);

        int fileIndex = this.offset;
        for (PhotoView view : views) {
            RequestCreator creator = Picasso.with(this).load(files.get(fileIndex));
            if (fileIndex - this.offset != 1) {
                creator.resize(200, 0);
            }
            creator.into(view);
            fileIndex = (fileIndex + 1) % files.size();
        }
    }

    public static class PhotoPagerAdapter extends PagerAdapter {

        private PhotoView[] views;

        public PhotoPagerAdapter(PhotoView[] views) {
            this.views = views;
        }

        @Override
        public int getCount() {
            return views.length;
        }

        @Override
        public View instantiateItem(ViewGroup container, int position) {
            container.addView(views[position]);
            return views[position];
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }
}
