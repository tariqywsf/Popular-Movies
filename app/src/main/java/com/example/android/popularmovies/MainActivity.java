/*
 * Copyright (c) 2020. This project was submitted by Tarek Bohdima as part of the Android Developer Nanodegree At Udacity.
 * As part of Udacity Honor code, your submissions must be your own work, hence submitting this project as yours will cause you to break the Udacity Honor Code and the suspension of your account.
 * Me, the author of the project, allow you to check the code as a reference, but if you submit it, it's your own responsibility if you get expelled.
 */

package com.example.android.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.popularmovies.network.JsonUtils;
import com.example.android.popularmovies.network.NetworkUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MovieAdapter.MovieAdapterClickListener {
    public static final String MOVIE_OBJECT = "movie_object";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String POPULAR = "popular";
    private static final String TOP_RATED = "top_rated";
    public ArrayList<Movie> moviesArrayList;
    private ImageView errorImageView;
    private RecyclerView recyclerView;
    private static MovieAdapter adapter;

    // Credits to https://stackoverflow.com/a/61566780/8899344
    private static boolean isNetworkConnected(Context context) {

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
            } else {
                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recycler_view);
        errorImageView = findViewById(R.id.connection_error_imageview);
        Context context = this;

        // Credits to https://stackoverflow.com/a/44187816/8899344
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setGridLayoutManager(context, 2);
        } else {
            setGridLayoutManager(context, 4);
        }

        recyclerView.setHasFixedSize(true);

        adapter = new MovieAdapter(context, moviesArrayList, (MovieAdapter.MovieAdapterClickListener) context);
        recyclerView.setAdapter(adapter);


        checkAndCall(context, POPULAR);
    }

    private void setGridLayoutManager(Context context, int spanCount) {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, spanCount);
        recyclerView.setLayoutManager(gridLayoutManager);
    }

    private void checkAndCall(Context context, String sortByParameter) {
        if (isNetworkConnected(context)) {
            makeMoviesQuery(sortByParameter);
        } else {
            showErrorImageView();
        }
    }

    private void makeMoviesQuery(String sortByParameter) {
        showRecyclerView();
        new MoviesAsyncTasks().execute(sortByParameter);
    }

    private void showRecyclerView() {
        errorImageView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void showErrorImageView() {
        errorImageView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.sort_by_popularity:
                checkAndCall(this, POPULAR);
                setTitle("Popular Movies");
                return true;
            case R.id.sort_by_rating:
                checkAndCall(this, TOP_RATED);
                setTitle("Top Rated Movies");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(Movie currentMovie) {
        Intent MainToDetailIntent = new Intent(MainActivity.this, DetailActivity.class);
        MainToDetailIntent.putExtra(MOVIE_OBJECT, currentMovie);
        startActivity(MainToDetailIntent);
    }


    private static class MoviesAsyncTasks extends AsyncTask<String, Void, ArrayList<Movie>> {
        @Override
        protected ArrayList<Movie> doInBackground(String... strings) {
            if (strings.length == 0) {
                return null;
            }
            String sortByParameter = strings[0];
            URL moviesQueryUrl = NetworkUtils.buildUrl(sortByParameter);

            String JsonResponse;
            try {
                switch (sortByParameter) {
                    case (POPULAR):
                        moviesQueryUrl = NetworkUtils.buildUrl(POPULAR);
                        break;
                    case (TOP_RATED):
                        moviesQueryUrl = NetworkUtils.buildUrl(TOP_RATED);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown URL " + moviesQueryUrl);
                }

                JsonResponse = NetworkUtils.makeHttpRequest(moviesQueryUrl);


            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "IOException in doInBackground: ", e);
                return null;
            }
            return JsonUtils.parseJson(JsonResponse);
        }

        @Override
        protected void onPostExecute(ArrayList<Movie> movies) {
            super.onPostExecute(movies);
            if (movies != null) {
               adapter.setMovieData(movies);
            } else {
                Log.d(TAG, "onPostExecute() called with: movies = [" + movies + "]");
            }
        }
    }
}
