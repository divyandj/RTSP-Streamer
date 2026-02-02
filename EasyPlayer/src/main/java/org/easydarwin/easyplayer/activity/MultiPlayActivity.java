package org.easydarwin.easyplayer.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;

import org.easydarwin.easyplayer.R;
import org.easydarwin.easyplayer.data.VideoSource;
import org.easydarwin.easyplayer.fragments.PlayFragment;

/**
 * Dual screen playback activity (side-by-side in landscape, stacked in portrait)
 */
public class MultiPlayActivity extends AppCompatActivity implements PlayFragment.OnDoubleTapListener {

    public static final String EXTRA_URL = "extra-url";
    public static final int REQUEST_SELECT_ITEM_TO_PLAY = 2001;

    ResultReceiver rr = new ResultReceiver(new Handler());

    private int mNextPlayHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_multi_play);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        ImageButton btn = (ImageButton) findViewById(R.id.toolbar_back);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        String url = getIntent().getStringExtra(EXTRA_URL);
        int transportMode = getIntent().getIntExtra(VideoSource.TRANSPORT_MODE, 0);
        int sendOption = getIntent().getIntExtra(VideoSource.SEND_OPTION, 0);

        if (!TextUtils.isEmpty(url)) {
            addVideoToHolder(url, transportMode, sendOption, R.id.play_fragment_holder1);
        }

        GridLayout grid = findViewById(R.id.fragment_container_grid);
        grid.removeAllViews();

        // Always add exactly 2 windows
        for (int i = 0; i < 2; i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.grid_item, grid, false);
            grid.addView(view);
            view.setId(i + 1);
        }

        setViewLayoutByConfiguration(getResources().getConfiguration());
    }

    public void onAddVideoSource(View view) {
        Intent intent = new Intent(this, PlayListActivity.class);
        intent.putExtra(PlayListActivity.EXTRA_BOOLEAN_SELECT_ITEM_TO_PLAY, true);
        startActivityForResult(intent, REQUEST_SELECT_ITEM_TO_PLAY);

        ViewGroup p = (ViewGroup) view.getParent();
        mNextPlayHolder = p.getId();
    }

    private void addVideoToHolder(String url, int transportMode, int sendOption, int holder) {
        PlayFragment f = PlayFragment.newInstance(url, transportMode, sendOption, rr);

        /**
         * Fill the screen
         */
        f.setScaleType(PlayFragment.ASPECT_RATIO_CENTER_CROPS);
        f.setOnDoubleTapListener(this);
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction().add(holder, f).commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SELECT_ITEM_TO_PLAY) {
            if (resultCode == RESULT_OK) {
                String url = data.getStringExtra("url");
                int transportMode = data.getIntExtra(VideoSource.TRANSPORT_MODE, 0);
                int sendOption = data.getIntExtra(VideoSource.SEND_OPTION, 0);
                addVideoToHolder(url, transportMode, sendOption, mNextPlayHolder);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setViewLayoutByConfiguration(newConfig);
    }

    void setViewLayoutByConfiguration(Configuration newConfig) {
        View container = findViewById(R.id.fragment_container_grid);
        GridLayout grid = (GridLayout) container;

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            params.dimensionRatio = null;

            findViewById(R.id.toolbar).setVisibility(View.GONE);

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); // Hide status bar

            // Side by side: 2 columns, 1 row
            grid.setColumnCount(2);
            grid.setRowCount(1);
            for (int i = 0; i < grid.getChildCount(); i++) {
                View v = grid.getChildAt(i);
                GridLayout.LayoutParams p = (GridLayout.LayoutParams) v.getLayoutParams();
                p.columnSpec = GridLayout.spec(i % 2, 1.0f);
                p.rowSpec = GridLayout.spec(i / 2, 1.0f);
            }
        } else {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            params.dimensionRatio = "1:1";

            findViewById(R.id.toolbar).setVisibility(View.VISIBLE);

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); // Show status bar

            // Stacked: 1 column, 2 rows
            grid.setColumnCount(1);
            grid.setRowCount(2);
            for (int i = 0; i < grid.getChildCount(); i++) {
                View v = grid.getChildAt(i);
                GridLayout.LayoutParams p = (GridLayout.LayoutParams) v.getLayoutParams();
                p.columnSpec = GridLayout.spec(i % 1, 1.0f);
                p.rowSpec = GridLayout.spec(i / 1, 1.0f);
            }
        }

        container.requestLayout();
    }

    @Override
    public void onDoubleTab(PlayFragment f) {
        GridLayout grid = findViewById(R.id.fragment_container_grid);

        for (int i = 0; i < grid.getChildCount(); i++) {
            View view = grid.getChildAt(i);

            if (view.getId() == f.getId()) {
                view.setVisibility(View.VISIBLE);
                continue;
            } else {
                if (view.getVisibility() == View.VISIBLE) {
                    view.setVisibility(View.GONE);
                    f.setScaleType(PlayFragment.FILL_WINDOW);
                } else {
                    view.setVisibility(View.VISIBLE);
                    f.setScaleType(PlayFragment.ASPECT_RATIO_CENTER_CROPS);
                }
            }
        }
    }

    @Override
    public void onSingleTab(PlayFragment f) {
        // No action for single tap
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}