package binarybricks.com.yelpql.details;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import binarybricks.com.yelpql.R;
import binarybricks.com.yelpql.network.BusinessDetailsAPI;
import binarybricks.com.yelpql.network.model.Business;
import binarybricks.com.yelpql.utils.YelpDataUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import yelp.BusinessDetails;

public class BusinessDetailsActivity extends AppCompatActivity {

    private static final String RESTAURANT_ID = "Restaurant_ID";
    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";

    ScrollView svBusinessDetails;
    ImageView ivRestaurant;
    TextView tvRestaurantName;
    ProgressBar progressbar;
    ImageView ivRating;
    TextView tvReviewsCount;
    TextView tvCost;
    TextView tvHours;
    TextView tvOpenToday;
    TextView tvAddress;
    MapView mvRestaurantLocation;
    ViewGroup reviewsLayout;

    private String restaurantID;
    private double latitude;
    private double longitude;
    private Business business;
    private CompositeDisposable compositeDisposable;

    public static Intent getBusinessDetailsIntent(@NonNull Context context, @NonNull String restaurantID, double latitude, double longitude) {
        Intent intent = new Intent(context, BusinessDetailsActivity.class);
        intent.putExtra(RESTAURANT_ID, restaurantID);
        intent.putExtra(LATITUDE, latitude);
        intent.putExtra(LONGITUDE, longitude);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_details);

        svBusinessDetails = findViewById(R.id.svBusinessDetails);
        ivRestaurant = findViewById(R.id.ivRestaurant);
        tvRestaurantName = findViewById(R.id.tvRestaurantName);
        progressbar = findViewById(R.id.progressbar);
        ivRating = findViewById(R.id.ivRating);
        tvReviewsCount = findViewById(R.id.tvReviewsCount);
        tvCost = findViewById(R.id.tvCost);
        tvHours = findViewById(R.id.tvHours);
        tvOpenToday = findViewById(R.id.tvOpenToday);
        tvAddress = findViewById(R.id.tvAddress);
        mvRestaurantLocation = findViewById(R.id.mvRestaurantLocation);
        reviewsLayout = findViewById(R.id.reviewsLayout);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        compositeDisposable = new CompositeDisposable();
        restaurantID = getIntent().getStringExtra(RESTAURANT_ID);
        latitude = getIntent().getDoubleExtra(LATITUDE, 0);
        longitude = getIntent().getDoubleExtra(LONGITUDE, 0);

        mvRestaurantLocation.onCreate(savedInstanceState);

        loadRestaurantData(restaurantID, latitude, longitude);

        findViewById(R.id.tvPhoneNumber).setOnClickListener(v -> {
            if (business != null) {
                Intent i = new Intent(Intent.ACTION_DIAL);
                i.setData(Uri.fromParts("tel", business.getPhone(), null));
                startActivity(i);
            }
        });

        findViewById(R.id.tvDirection).setOnClickListener(v -> {
            if (business != null) {
                // Create a Uri from an intent string. Use the result to create an Intent.
                Uri gmmIntentUri = Uri.parse(String.format("google.streetview:cbll=%1$f,%2%f", business.getLatitude(), business.getLongitude()));

                // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                // Make the Intent explicit by setting the Google Maps package
                mapIntent.setPackage("com.google.android.apps.maps");

                // Attempt to start an activity that can handle the Intent
                startActivity(mapIntent);
            }
        });
    }

    private void loadRestaurantData(final String businessID, final double latitude, final double longitude) {

        progressbar.setVisibility(View.VISIBLE);

        compositeDisposable.add(BusinessDetailsAPI.getBusinessDetails(getString(R.string.apiKey), businessID, latitude, longitude)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(business -> {
                            progressbar.setVisibility(View.GONE);
                            bindData(business);
                        }
                        , throwable -> {
                            progressbar.setVisibility(View.GONE);
                            Toast.makeText(BusinessDetailsActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
                        }));
    }

    private void bindData(Business business) {
        this.business = business;

        getSupportActionBar().setTitle(business.getName());

        Picasso.get().load(business.getPhotos().get(0)).into(ivRestaurant);
        tvRestaurantName.setText(business.getName());
        YelpDataUtil.showRatingLogo(ivRating, business.getRating());
        tvReviewsCount.setText(business.getReviewCount() + " Reviews");
        tvCost.setText(business.getPrice());
        tvHours.setText(YelpDataUtil.getTodaysHours(business.getHourList()));
        if (business.isOpenNow()) {
            tvOpenToday.setText(R.string.open);
        } else {
            tvOpenToday.setText(R.string.closed);
            tvOpenToday.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
        tvAddress.setText(business.getFormattedAddress());
        showPointerOnMap(business.getLatitude(), business.getLongitude());
        showTopReviews(business.getReviewList());
    }

    private void showPointerOnMap(final double latitude, final double longitude) {
        mvRestaurantLocation.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                LatLng latLng = new LatLng(latitude, longitude);
                googleMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_flag))
                        .anchor(0.0f, 1.0f)
                        .position(latLng));
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                googleMap.getUiSettings().setZoomControlsEnabled(true);

                // Updates the location and zoom of the MapView
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
                googleMap.moveCamera(cameraUpdate);
            }
        });
    }

    private void showTopReviews(List<BusinessDetails.Review> reviewList) {
        LayoutInflater layoutInflater = getLayoutInflater();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = 32;
        for (BusinessDetails.Review review : reviewList) {
            reviewsLayout.addView(getUserReviewSection(review, layoutInflater, dateFormat), params);
        }
    }

    private View getUserReviewSection(BusinessDetails.Review review, LayoutInflater layoutInflater, DateFormat dateFormat) {
        View reviewView = layoutInflater.inflate(R.layout.business_reviews, null);

        Picasso.get().load(review.user().image_url()).into((ImageView) reviewView.findViewById(R.id.ivUserImage));
        ((TextView) reviewView.findViewById(R.id.tvUserName)).setText(review.user().name());
        YelpDataUtil.showRatingLogo((ImageView) reviewView.findViewById(R.id.ivRating), String.valueOf(review.rating()));
        ((TextView) reviewView.findViewById(R.id.tvUserReview)).setText(review.text());

        try {
            ((TextView) reviewView.findViewById(R.id.tvUserRatingTime)).setText(YelpDataUtil.getDuration(this, dateFormat.parse(review.time_created()).getTime()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return reviewView;
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
    protected void onStop() {
        super.onStop();
        compositeDisposable.dispose();
    }

    @Override
    public void onResume() {
        mvRestaurantLocation.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mvRestaurantLocation.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mvRestaurantLocation.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mvRestaurantLocation.onLowMemory();
    }
}
