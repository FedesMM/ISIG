package com.isig.lab2;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.TextSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.esri.arcgisruntime.util.ListenableList;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import android.os.Handler;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.isig.lab2.models.Marker;

public class MainActivity extends AppCompatActivity {

    private MapView mMapView;
    //Georeferenciacion
    private SearchView mSearchView = null;
    private GraphicsOverlay mGraphicsOverlay;
    private LocatorTask mLocatorTask = null;
    private GeocodeParameters mGeocodeParameters = new GeocodeParameters();

    private List<Marker> markers = new ArrayList<>();
    private boolean addMarkerFromMap = false;
    private BottomSheetBehavior bottomSheetBehavior;

    @BindView(R.id.bottom_sheet_markers) View bottomSheetMarkers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> showBottomSheet(bottomSheetMarkers));

        mMapView = findViewById(R.id.mapView);
        ArcGISMap map = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, -34.72, -56.085, 16);
        mMapView.setMap(map);
        // *** Georeferenciacion ***
        setupLocator();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Georeferenciacion
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.search);
        if (searchMenuItem != null) {
            mSearchView = (SearchView) searchMenuItem.getActionView();
            if (mSearchView != null) {
                SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
                mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
                mSearchView.setIconifiedByDefault(false);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause(){
        mMapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mMapView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.dispose();
    }

    @OnClick(R.id.text_cancel_create_sheet)
    protected void onCancelBottomSheetClicked() {
        hideBottomSheet();
    }

    @OnClick(R.id.add_marker_from_map)
    protected void onAddMarkerFromMap() {
        addMarkerFromMap = true;
        Log.d("onAddMarkerFromMap", "addMarkerFromMap: " + addMarkerFromMap);
        hideBottomSheet();
    }

    @OnClick(R.id.add_marker_from_lat_long)
    protected void onAddMarkerFromLatLong() {
        showAddMarkerFromLatLongDialog();
        hideBottomSheet();
    }

    private void setBottomSheet(View bottomSheet) {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void showBottomSheet(View bottomSheet) {
        setBottomSheet(bottomSheet);
        if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            bottomSheet.requestLayout();
        }
    }

    public void hideBottomSheet() {
        if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            bottomSheetBehavior = null;
        }
    }

    //Georeferenciacion
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("onNewIntent", "Ejecuto");
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            queryLocator(intent.getStringExtra(SearchManager.QUERY));
        }
    }

    //Georeferenciacion
    private void queryLocator(final String query) {
        if (query != null && query.length() > 0) {
            mLocatorTask.cancelLoad();
            final ListenableFuture<List<GeocodeResult>> geocodeFuture = mLocatorTask.geocodeAsync(query, mGeocodeParameters);
            geocodeFuture.addDoneListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d("queryLocator","Busca.");
                        List<GeocodeResult> geocodeResults = geocodeFuture.get();
                        if (geocodeResults.size() > 0) {
                            displaySearchResult(geocodeResults.get(0));
                            Log.d("queryLocator","Encuentra");
                        } else {
                            Toast.makeText(getApplicationContext(), getString(R.string.nothing_found) + " " + query, Toast.LENGTH_LONG).show();
                            Log.d("queryLocator","No Encuentra");
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        // ... determine how you want to handle an error
                    }
                    geocodeFuture.removeDoneListener(this); // Done searching, remove the listener.
                }
            });
        }
    }

    // Georeferenciacion
    private void displaySearchResult(GeocodeResult geocodedLocation) {
        Log.d("displaySearchResult", "Desplego el resultado");
        String displayLabel = geocodedLocation.getLabel();
        TextSymbol textLabel = new TextSymbol(18, displayLabel, Color.rgb(192, 32, 32), TextSymbol.HorizontalAlignment.CENTER, TextSymbol.VerticalAlignment.BOTTOM);
        Log.d("displaySearchResult", "Armo el elemento y  su texto");
        Graphic textGraphic = new Graphic(geocodedLocation.getDisplayLocation(), textLabel);
        Graphic mapMarker = new Graphic(geocodedLocation.getDisplayLocation(), geocodedLocation.getAttributes(),
                new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.SQUARE, Color.rgb(255, 0, 0), 12.0f));
        ListenableList allGraphics = mGraphicsOverlay.getGraphics();
        allGraphics.clear();
        Log.d("displaySearchResult", "Desplego el elemento y el texto");
        allGraphics.add(mapMarker);
        allGraphics.add(textGraphic);
        Log.d("displaySearchResult", "Centro la vista en el punto.");
        mMapView.setViewpointCenterAsync(geocodedLocation.getDisplayLocation());
    }

    private void setupLocator() {
        String locatorService = "https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer";
        mLocatorTask = new LocatorTask(locatorService);
        mLocatorTask.addDoneLoadingListener(() -> {
            if (mLocatorTask.getLoadStatus() == LoadStatus.LOADED) {
                mGeocodeParameters.getResultAttributeNames().add("*");
                mGeocodeParameters.setMaxResults(1);
                mGraphicsOverlay = new GraphicsOverlay();
                mMapView.getGraphicsOverlays().add(mGraphicsOverlay);
                Log.d("Init","Cargo el locator");

                //touchListener();
                setAddMarkerListener();

            } else if (mSearchView != null) {
                mSearchView.setEnabled(false);
                Log.d("Init","No cargo el locator");
            }
        });
        mLocatorTask.loadAsync();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void touchListener() {
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView){
            public boolean onSingleTapConfirmed(MotionEvent e) {
                Log.d("setOnTouchListener", "onSingleTapConfirmed");
                final android.graphics.Point screenPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());
                // identify graphics on the graphics overlay
                final ListenableFuture<IdentifyGraphicsOverlayResult>
                        identifyGraphic = mMapView.identifyGraphicsOverlayAsync(mGraphicsOverlay, screenPoint, 10.0, false, 2);
                identifyGraphic.addDoneListener(() -> {
                    try {
                        IdentifyGraphicsOverlayResult grOverlayResult = identifyGraphic.get();
                        // get the list of graphics returned by identify graphic overlay
                        List<Graphic> graphics = grOverlayResult.getGraphics();
                        Callout mCallout = mMapView.getCallout();
                        if (mCallout.isShowing()) {
                            mCallout.dismiss();
                        }
                        if (!graphics.isEmpty()) {
                            // get callout, set content and show
                            String text = "";
                            if (graphics.get(0).getAttributes().get("city") != null) {
                                text = graphics.get(0).getAttributes().get("city").toString();
                            }
                            if (graphics.get(0).getAttributes().get("country") != null) {
                                if (!text.equals("")){
                                    text += ", ";
                                }
                                text += graphics.get(0).getAttributes().get("country").toString();
                            }
                            if (!text.equals("")) {
                                TextView calloutContent = new TextView(getApplicationContext());
                                calloutContent.setText(text);
                                Point mapPoint = mMapView.screenToLocation(screenPoint);
                                mCallout.setLocation(mapPoint);
                                mCallout.setContent(calloutContent);
                                mCallout.show();
                                new Handler().postDelayed(mCallout::dismiss, 3000);
                            }
                        }
                    } catch (InterruptedException | ExecutionException ie) {
                        ie.printStackTrace();
                    }
                });
                return super.onSingleTapConfirmed(e);
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setAddMarkerListener() {
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            public boolean onSingleTapConfirmed(MotionEvent e) {
                openAddMarkerFromMapDialog(e);
                return true;
            }
        });
    }

    private void openAddMarkerFromMapDialog(MotionEvent e) {
        if (addMarkerFromMap) {
            android.graphics.Point p = new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY()));
            Point point = mMapView.screenToLocation(p);
            Marker marker = new Marker("", "", point.getY(), point.getX(), Marker.REPRESENTATION_UTM);
            ViewGroup vg = (ViewGroup) findViewById(android.R.id.content);
            marker.showAddFromMapDialog(MainActivity.this, vg, point, markers, new Marker.Viewer.AddMarkerCallback() {
                @Override
                public void onMarkerAdded() {
                    Log.d("onMarkerAdded", "");
                    Toast.makeText(MainActivity.this, getString(R.string.marker_added), Toast.LENGTH_LONG).show();
                    showMarkers();
                    addMarkerFromMap = false;
                }

                @Override
                public void onMarkerAddingCanceled() {
                    Log.d("onMarkerAddingCanceled", "");
                    addMarkerFromMap = false;
                }
            });
        }
    }

    private void showAddMarkerFromLatLongDialog() {
        Marker marker = new Marker("", "", 0, 0, Marker.REPRESENTATION_WGS84);
        ViewGroup vg = (ViewGroup) findViewById(android.R.id.content);
        Point point = new Point(0,0);
        marker.showAddFromLatLongDialog(MainActivity.this, vg, point, markers, new Marker.Viewer.AddMarkerCallback() {
            @Override
            public void onMarkerAdded() {
                Log.d("onMarkerAdded", "");
                Toast.makeText(MainActivity.this, getString(R.string.marker_added), Toast.LENGTH_LONG).show();
                showMarkers();
            }

            @Override
            public void onMarkerAddingCanceled() {
                Log.d("onMarkerAddingCanceled", "");
            }
        });
    }

    private void showMarkers() {
        for (Marker m: markers) {
            SpatialReference sp = m.getRepresentation() == Marker.REPRESENTATION_UTM ? SpatialReferences.getWebMercator() : SpatialReferences.getWgs84();
            Point marker = new Point(m.getLon(), m.getLat(), sp);
            SimpleMarkerSymbol sms = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, m.getColor(), 12);
            Graphic g = new Graphic(marker, sms);
            mGraphicsOverlay.getGraphics().add(g);
        }
    }

}
