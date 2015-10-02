/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jikexueyuan.jikecalculator;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Toast;

import com.jikexueyuan.jikecalculator.CalculatorDisplay;
import com.jikexueyuan.jikecalculator.EventListener;
import com.jikexueyuan.jikecalculator.History;
import com.jikexueyuan.jikecalculator.HistoryAdapter;
import com.jikexueyuan.jikecalculator.Logic;
import com.jikexueyuan.jikecalculator.PanelSwitcher;
import com.jikexueyuan.jikecalculator.Persist;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import io.github.codefalling.recyclerviewswipedismiss.SwipeDismissRecyclerViewTouchListener;

public class Calculator extends Activity implements PanelSwitcher.Listener, Logic.Listener,
        OnClickListener, OnMenuItemClickListener {
    EventListener mListener = new EventListener();
    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private Logic mLogic;
    private ViewPager mPager;
    private View mClearButton;
    private View mBackspaceButton;
    private View mOverflowMenuButton;

    private SlidingUpPanelLayout mSlidingUpPanelLayout;
    private ListView mHistoryView;

    private HistoryAdapter mHistoryAdapter;

    private RecyclerView mRecyclerView;
    private RecyclerHistoryAdapter mRecyclerHistoryAdapter;


    static final int BASIC_PANEL    = 0;
    static final int ADVANCED_PANEL = 1;

    private static final String LOG_TAG = "Calculator";
    private static final boolean DEBUG  = false;
    private static final boolean LOG_ENABLED = false;
    private static final String STATE_CURRENT_VIEW = "state-current-view";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Disable IME for this application
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        setContentView(com.jikexueyuan.jikecalculator.R.layout.main);
        mPager = (ViewPager) findViewById(com.jikexueyuan.jikecalculator.R.id.panelswitch);
        if (mPager != null) {
            mPager.setAdapter(new PageAdapter(mPager));
        } else {
            // Single page UI
            final TypedArray buttons = getResources().obtainTypedArray(com.jikexueyuan.jikecalculator.R.array.buttons);
            for (int i = 0; i < buttons.length(); i++) {
                setOnClickListener(null, buttons.getResourceId(i, 0));
            }
            buttons.recycle();
        }

        if (mClearButton == null) {
            mClearButton = findViewById(com.jikexueyuan.jikecalculator.R.id.clear);
            mClearButton.setOnClickListener(mListener);
            mClearButton.setOnLongClickListener(mListener);
        }
        if (mBackspaceButton == null) {
            mBackspaceButton = findViewById(com.jikexueyuan.jikecalculator.R.id.del);
            mBackspaceButton.setOnClickListener(mListener);
            mBackspaceButton.setOnLongClickListener(mListener);
        }

        mPersist = new Persist(this);
        mPersist.load();

        mHistory = mPersist.history;

        mDisplay = (CalculatorDisplay) findViewById(com.jikexueyuan.jikecalculator.R.id.display);

        mLogic = new Logic(this, mHistory, mDisplay);
        mLogic.setListener(this);

        mLogic.setDeleteMode(mPersist.getDeleteMode());
        mLogic.setLineLength(mDisplay.getMaxDigits());

        mHistoryAdapter = new HistoryAdapter(this, mHistory, mLogic);
        mHistory.setObserver(mHistoryAdapter);

        if (mPager != null) {
            mPager.setCurrentItem(state == null ? 0 : state.getInt(STATE_CURRENT_VIEW, 0));
        }

        mListener.setHandler(mLogic, mPager);
        mDisplay.setOnKeyListener(mListener);


        mLogic.resumeWithHistory();
        updateDeleteMode();

        SoundManager.getInstance().initSounds(this);

        mHistoryView = (ListView)findViewById(R.id.historyList);

        mHistoryView.setAdapter(mHistoryAdapter);
        mHistoryView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        mHistoryView.setStackFromBottom(true);
        mHistoryView.setFocusable(false);


        mRecyclerView = (RecyclerView)findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerHistoryAdapter = new RecyclerHistoryAdapter(this,mHistory,mLogic);

        mRecyclerView.setAdapter(mRecyclerHistoryAdapter);
        mHistory.setmRecyclerHistoryAdapter(mRecyclerHistoryAdapter);

        SwipeDismissRecyclerViewTouchListener listener = new SwipeDismissRecyclerViewTouchListener.Builder(mRecyclerView,
                new SwipeDismissRecyclerViewTouchListener.DismissCallbacks() {
                    @Override
                    public boolean canDismiss(int i) {
                        return true;
                    }

                    @Override
                    public void onDismiss(View view) {
                     int pos = mRecyclerView.getChildPosition(view);
                        mRecyclerHistoryAdapter.remove(pos);
                        Toast.makeText(getBaseContext(),String.format("Delete item %d",pos),Toast.LENGTH_SHORT).show();

                    }
                }
        ).setIsVertical(false)
                .create();

        mRecyclerView.setOnTouchListener(listener);

        ImageButton clear = (ImageButton) findViewById(R.id.clear_all_history);
        clear.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mRecyclerHistoryAdapter.removeAll();
                mDisplay.setText("", CalculatorDisplay.Scroll.UP);
            }
        });

        ImageButton add = (ImageButton)findViewById(R.id.add_all_history);
        add.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mRecyclerHistoryAdapter.addAll();
            }
        });
    }

    private void updateDeleteMode() {
        if (mLogic.getDeleteMode() == Logic.DELETE_MODE_BACKSPACE) {
            mClearButton.setVisibility(View.GONE);
            mBackspaceButton.setVisibility(View.VISIBLE);
        } else {
            mClearButton.setVisibility(View.VISIBLE);
            mBackspaceButton.setVisibility(View.GONE);
        }
    }

    void setOnClickListener(View root, int id) {
        final View target = root != null ? root.findViewById(id) : findViewById(id);
        target.setOnClickListener(mListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(com.jikexueyuan.jikecalculator.R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(com.jikexueyuan.jikecalculator.R.id.basic).setVisible(!getBasicVisibility());
        menu.findItem(com.jikexueyuan.jikecalculator.R.id.advanced).setVisible(!getAdvancedVisibility());
        return true;
    }


    private void createFakeMenu() {
        mOverflowMenuButton = findViewById(com.jikexueyuan.jikecalculator.R.id.overflow_menu);
        if (mOverflowMenuButton != null) {
            mOverflowMenuButton.setVisibility(View.VISIBLE);
            mOverflowMenuButton.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case com.jikexueyuan.jikecalculator.R.id.overflow_menu:
                PopupMenu menu = constructPopupMenu();
                if (menu != null) {
                    menu.show();
                }
                break;
        }
    }

    private PopupMenu constructPopupMenu() {
        final PopupMenu popupMenu = new PopupMenu(this, mOverflowMenuButton);
        mOverflowMenuButton.setOnTouchListener(popupMenu.getDragToOpenListener());
        final Menu menu = popupMenu.getMenu();
        popupMenu.inflate(com.jikexueyuan.jikecalculator.R.menu.menu);
        popupMenu.setOnMenuItemClickListener(this);
        onPrepareOptionsMenu(menu);
        return popupMenu;
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    private boolean getBasicVisibility() {
        return mPager != null && mPager.getCurrentItem() == BASIC_PANEL;
    }

    private boolean getAdvancedVisibility() {
        return mPager != null && mPager.getCurrentItem() == ADVANCED_PANEL;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case com.jikexueyuan.jikecalculator.R.id.clear_history:
                mHistory.clear();
                mLogic.onClear();
                break;

            case com.jikexueyuan.jikecalculator.R.id.basic:
                if (!getBasicVisibility() && mPager != null) {
                    mPager.setCurrentItem(BASIC_PANEL, true);
                }
                break;

            case com.jikexueyuan.jikecalculator.R.id.advanced:
                if (!getAdvancedVisibility() && mPager != null) {
                    mPager.setCurrentItem(ADVANCED_PANEL, true);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (mPager != null) {
            state.putInt(STATE_CURRENT_VIEW, mPager.getCurrentItem());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mLogic.updateHistory();
        mPersist.setDeleteMode(mLogic.getDeleteMode());
        mPersist.save();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK && getAdvancedVisibility()
                && mPager != null) {
            mPager.setCurrentItem(BASIC_PANEL);
            return true;
        } else {
            return super.onKeyDown(keyCode, keyEvent);
        }
    }

    static void log(String message) {
        if (LOG_ENABLED) {
            Log.v(LOG_TAG, message);
        }
    }

    @Override
    public void onChange() {
        invalidateOptionsMenu();
    }

    @Override
    public void onDeleteModeChange() {
        updateDeleteMode();
    }

    class PageAdapter extends PagerAdapter {
        private View mSimplePage;
        private View mAdvancedPage;

        public PageAdapter(ViewPager parent) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final View simplePage = inflater.inflate(com.jikexueyuan.jikecalculator.R.layout.simple_pad, parent, false);
            final View advancedPage = inflater.inflate(com.jikexueyuan.jikecalculator.R.layout.advanced_pad, parent, false);
            mSimplePage = simplePage;
            mAdvancedPage = advancedPage;

            final Resources res = getResources();
            final TypedArray simpleButtons = res.obtainTypedArray(com.jikexueyuan.jikecalculator.R.array.simple_buttons);
            for (int i = 0; i < simpleButtons.length(); i++) {
                setOnClickListener(simplePage, simpleButtons.getResourceId(i, 0));
            }
            simpleButtons.recycle();

            final TypedArray advancedButtons = res.obtainTypedArray(com.jikexueyuan.jikecalculator.R.array.advanced_buttons);
            for (int i = 0; i < advancedButtons.length(); i++) {
                setOnClickListener(advancedPage, advancedButtons.getResourceId(i, 0));
            }
            advancedButtons.recycle();

            final View clearButton = simplePage.findViewById(com.jikexueyuan.jikecalculator.R.id.clear);
            if (clearButton != null) {
                mClearButton = clearButton;
            }

            final View backspaceButton = simplePage.findViewById(com.jikexueyuan.jikecalculator.R.id.del);
            if (backspaceButton != null) {
                mBackspaceButton = backspaceButton;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public void startUpdate(View container) {
        }

        @Override
        public Object instantiateItem(View container, int position) {
            final View page = position == 0 ? mSimplePage : mAdvancedPage;
            ((ViewGroup) container).addView(page);
            return page;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            ((ViewGroup) container).removeView((View) object);
        }

        @Override
        public void finishUpdate(View container) {
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }
    }
}
