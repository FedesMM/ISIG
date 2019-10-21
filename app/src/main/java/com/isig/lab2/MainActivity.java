package com.isig.lab2;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.ViewpointChangedEvent;
import com.esri.arcgisruntime.mapping.view.ViewpointChangedListener;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.TextSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorAttribute;
import com.esri.arcgisruntime.tasks.geocode.LocatorInfo;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.esri.arcgisruntime.util.ListenableList;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.Html;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private MapView mMapView;
    //Georeferenciacion
    private SearchView mSearchView = null;
    private GraphicsOverlay mGraphicsOverlay;
    private LocatorTask mLocatorTask = null;
    private GeocodeParameters mGeocodeParameters = null;
    //Busqueda por categoria
    private GraphicsOverlay graphicsOverlay;
    private LocatorTask locator = new LocatorTask("http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer");
    private Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mMapView = findViewById(R.id.mapView);
        ArcGISMap map = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, 34.056295, -117.195800, 16);
        mMapView.setMap(map);
        // *** Georeferenciacion ***
        setupLocator();
        //**Busqueda por categoria**//
        // *** ADD ***
        mMapView.addViewpointChangedListener(new ViewpointChangedListener() {
            @Override
            public void viewpointChanged(ViewpointChangedEvent viewpointChangedEvent) {
                if (graphicsOverlay == null) {
                    graphicsOverlay = new GraphicsOverlay();
                    mMapView.getGraphicsOverlays().add(graphicsOverlay);
                    setupSpinner();
                    setupPlaceTouchListener();
                    setupNavigationChangedListener();
                    mMapView.removeViewpointChangedListener(this);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Georeferenciacion
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.search);
        Log.d("onCreateOptionsMenu","Busca el searchMenuItem");
        if (searchMenuItem != null) {
            Log.d("onCreateOptionsMenu","Encuentra el searchMenuItem");
            mSearchView = (SearchView) searchMenuItem.getActionView();
            Log.d("onCreateOptionsMenu","Procesa el searchMenuItem en un mSearchView");
            if (mSearchView != null) {
                Log.d("onCreateOptionsMenu","Obtengo el mSearchView");
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
        if (id == R.id.search) {

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
                mGeocodeParameters = new GeocodeParameters();
                mGeocodeParameters.getResultAttributeNames().add("*");
                mGeocodeParameters.setMaxResults(1);
                mGraphicsOverlay = new GraphicsOverlay();
                mMapView.getGraphicsOverlays().add(mGraphicsOverlay);
                Log.d("Init","Cargo el locator");
                desplegarInfoLocator(mLocatorTask);



            } else if (mSearchView != null) {
                mSearchView.setEnabled(false);
                Log.d("Init","No cargo el locator");
            }
        });
        mLocatorTask.loadAsync();
    }

    private void desplegarInfoLocator(LocatorTask locatorTask) {
        // Get LocatorInfo from a loaded LocatorTask
        LocatorInfo locatorInfo = locatorTask.getLocatorInfo();
        List<String> resultAttributeNames = new ArrayList<>();
        Log.d("desplegarInfoLocator","desplegarInfoLocator: ");
        //System.out.print(locatorInfo.getProperties());

        // Loop through all the attributes available
        for (LocatorAttribute resultAttribute : locatorInfo.getResultAttributes()) {
            resultAttributeNames.add(resultAttribute.getDisplayName());
            // Use in adapter etc...
            System.out.print(resultAttribute.getName()+": "+resultAttribute.getDisplayName()+" ");
        }
    }

    //Busqueda por categoria
    private void findPlaces(String placeCategory) {
        GeocodeParameters parameters = new GeocodeParameters();
        Point searchPoint;

        if (mMapView.getVisibleArea() != null) {
            searchPoint = mMapView.getVisibleArea().getExtent().getCenter();
            if (searchPoint == null) {
                return;
            }
        } else {
            return;
        }
        parameters.setPreferredSearchLocation(searchPoint);
        parameters.setMaxResults(25);

        List<String> outputAttributes = parameters.getResultAttributeNames();
        outputAttributes.add("Place_addr");
        outputAttributes.add("PlaceName");
        // Execute the search and add the places to the graphics overlay.
        final ListenableFuture<List<GeocodeResult>> results = locator.geocodeAsync(placeCategory, parameters);
        results.addDoneListener(() -> {
            try {
                ListenableList<Graphic> graphics = graphicsOverlay.getGraphics();
                graphics.clear();
                List<GeocodeResult> places = results.get();
                for (GeocodeResult result : places) {

                    // Add a graphic representing each location with a simple marker symbol.
                    SimpleMarkerSymbol placeSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.GREEN, 10);
                    placeSymbol.setOutline(new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.WHITE, 2));
                    Graphic graphic = new Graphic(result.getDisplayLocation(), placeSymbol);
                    java.util.Map<String, Object> attributes = result.getAttributes();

                    // Store the location attributes with the graphic for later recall when this location is identified.
                    for (String key : attributes.keySet()) {
                        String value = attributes.get(key).toString();
                        graphic.getAttributes().put(key, value);
                    }
                    graphics.add(graphic);
                }
            } catch (InterruptedException | ExecutionException exception) {
                exception.printStackTrace();
            }
        });
    }

    private void showCalloutAtLocation(Graphic graphic, Point mapPoint) {
        Callout callout = mMapView.getCallout();
        TextView calloutContent = new TextView(getApplicationContext());

        callout.setLocation(graphic.computeCalloutLocation(mapPoint, mMapView));
        calloutContent.setTextColor(Color.BLACK);
        calloutContent.setText(Html.fromHtml("<b>" + graphic.getAttributes().get("PlaceName").toString() + "</b><br>" + graphic.getAttributes().get("Place_addr").toString()));
        callout.setContent(calloutContent);
        callout.show();
    }

    private void setupSpinner() {
        spinner = findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                findPlaces(adapterView.getItemAtPosition(i).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        findPlaces(spinner.getSelectedItem().toString());
    }

    private void setupNavigationChangedListener() {
        mMapView.addNavigationChangedListener(navigationChangedEvent -> {
            if (!navigationChangedEvent.isNavigating()) {
                mMapView.getCallout().dismiss();
                findPlaces(spinner.getSelectedItem().toString());
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPlaceTouchListener() {
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent motionEvent) {

                // Dismiss a prior callout.
                mMapView.getCallout().dismiss();

                // get the screen point where user tapped
                final android.graphics.Point screenPoint = new android.graphics.Point(Math.round(motionEvent.getX()), Math.round(motionEvent.getY()));

                // identify graphics on the graphics overlay
                final ListenableFuture<IdentifyGraphicsOverlayResult> identifyGraphic = mMapView.identifyGraphicsOverlayAsync(graphicsOverlay, screenPoint, 10.0, false, 2);

                identifyGraphic.addDoneListener(() -> {
                    try {
                        IdentifyGraphicsOverlayResult graphicsResult = identifyGraphic.get();
                        // get the list of graphics returned by identify graphic overlay
                        List<Graphic> graphicList = graphicsResult.getGraphics();

                        // get the first graphic selected and show its attributes with a callout
                        if (!graphicList.isEmpty()){
                            showCalloutAtLocation(graphicList.get(0), mMapView.screenToLocation(screenPoint));
                        }
                    } catch (InterruptedException | ExecutionException exception) {
                        exception.printStackTrace();
                    }
                });
                return super.onSingleTapConfirmed(motionEvent);
            }
        });
    }
}
