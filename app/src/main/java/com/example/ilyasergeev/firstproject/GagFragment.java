
package com.example.ilyasergeev.firstproject;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.ilyasergeev.firstproject.data.GagContract;
import com.example.ilyasergeev.firstproject.sync.FreshGagSyncAdapter;


public class GagFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String LOG_TAG = GagFragment.class.getSimpleName();
    private GagAdapter mGagAdapter;

    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;
    private boolean mUseTodayLayout;

    private static final String SELECTED_KEY = "selected_position";

    private static final int GAG_LOADER = 0;

    private static final String[] GAG_COLUMNS = {
            GagContract.GagEntry.TABLE_NAME + "." + GagContract.GagEntry._ID,
            GagContract.GagEntry.COLUMN_DATE,
            GagContract.GagEntry.COLUMN_GAG_IMAGE,
            GagContract.TypeEntry.COLUMN_TYPE_SETTING,
            GagContract.GagEntry.COLUMN_GAG_ID,
            GagContract.GagEntry.COLUMN_CAPTION
    };

    static final int COL_GAG_DATE = 1;
    static final int COL_CAPTION = 5;


    public interface Callback {

        public void onItemSelected(Uri dateUri);
    }

    public GagFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        mGagAdapter = new GagAdapter(getActivity(), null, 0);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        mListView.setAdapter(mGagAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    ((Callback) getActivity())
                            .onItemSelected(GagContract.GagEntry.buildGagWithDate(
                                    locationSetting, cursor.getLong(COL_GAG_DATE)
                            ));
                }
                mPosition = position;
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        mGagAdapter.setUseTodayLayout(mUseTodayLayout);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(GAG_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    void onLocationChanged( ) {
        updateWeather();
        getLoaderManager().restartLoader(GAG_LOADER, null, this);
    }

    private void updateWeather() {
        FreshGagSyncAdapter.syncImmediately(getActivity());
    }



    @Override
    public void onSaveInstanceState(Bundle outState) {

        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        String sortOrder = GagContract.GagEntry.COLUMN_DATE + " ASC";

        String locationSetting = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri = GagContract.GagEntry.buildGagTypeWithStartDate(
                locationSetting, System.currentTimeMillis());

        return new CursorLoader(getActivity(),
                weatherForLocationUri,
                GAG_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mGagAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            mListView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mGagAdapter.swapCursor(null);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        if (mGagAdapter != null) {
            mGagAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }
}