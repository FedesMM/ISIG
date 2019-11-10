package com.isig.lab2;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.security.AuthenticationManager;
import com.esri.arcgisruntime.security.DefaultAuthenticationChallengeHandler;
import com.esri.arcgisruntime.security.OAuthConfiguration;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.TextSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorAttribute;
import com.esri.arcgisruntime.tasks.geocode.LocatorInfo;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;
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
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.view.ViewGroup;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.gson.Gson;
import com.isig.lab2.models.Marker;
import com.isig.lab2.models.Path;
import com.isig.lab2.models.RoutePointRequestModel;

public class MainActivity extends AppCompatActivity {

    //Georeferenciacion
    private SearchView mSearchView = null;
    private GraphicsOverlay mGraphicsOverlay;
    private LocatorTask mLocatorTask = null;
    private GeocodeParameters mGeocodeParameters = new GeocodeParameters();

    private List<Marker> markers = new ArrayList<>();
    private boolean addMarkerFromMap = false;
    private BottomSheetBehavior bottomSheetBehavior;
    private Graphic currentPosition;
    private Handler showCurrentPositionHandler;
    private Runnable runnable;
    private RoutePointRequestModel request;

    //Busqueda por categoria
    private GraphicsOverlay graphicsOverlay;
    private LocatorTask locator = new LocatorTask("http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer");
    //Ruta mas corta
    private List<Point> mPoint = new ArrayList<>();

    @BindView(R.id.mapView) MapView mMapView;
    @BindView(R.id.bottom_sheet_markers) View bottomSheetMarkers;
    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.progress_bar) ProgressBar progressBar;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        progressBar.setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);

        fab.setOnClickListener(view -> showBottomSheet(bottomSheetMarkers));

        /**Autenticacion**/
        setupOAuthManager();

        ArcGISMap map = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, -34.726272, -56.227631, 16);
        mMapView.setMap(map);

        // *** Georeferenciacion ***
        setupLocator();

        //*Desplegar punto, linea y poligono*//
        //createGraphics();
        /**Autenticacion**/
        //ArcGISMapImageLayer traffic = new ArcGISMapImageLayer(getResources().getString(R.string.traffic_service));
        //map.getOperationalLayers().add(traffic);
        /**Ruta mas corta**/
        mMapView.setOnTouchListener (new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                if (addMarkerFromMap) {
                    openAddMarkerFromMapDialog(e);
                } else {
                    android.graphics.Point screenPoint = new android.graphics.Point(
                            Math.round(e.getX()),
                            Math.round(e.getY()));
                    Point mapPoint = mMapView.screenToLocation(screenPoint);
                    mapClicked(mapPoint);
                }
                return super.onSingleTapConfirmed(e);
            }
        });

        createGraphicsOverlay();
        //*Desplegar punto, linea y poligono*//
        //createGraphics();
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
                assert searchManager != null;
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
                progressBar.setVisibility(View.GONE);
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

    //Mostrar puntos, lineas y poligonos
    private void createGraphicsOverlay() {
        mGraphicsOverlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(mGraphicsOverlay);
    }

    private void createPointGraphics() {
        Point point = new Point(-118.69333917997633, 34.032793670122885, SpatialReferences.getWgs84());
        SimpleMarkerSymbol pointSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.rgb(226, 119, 40), 10.0f);
        pointSymbol.setOutline(new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 1.5f));
        Graphic pointGraphic = new Graphic(point, pointSymbol);
        mGraphicsOverlay.getGraphics().add(pointGraphic);
    }

    private void createPolylineGraphics() {
        PointCollection polylinePoints = new PointCollection(SpatialReferences.getWgs84());
        polylinePoints.add(new Point(-118.67999016098526, 34.035828839974684));
        polylinePoints.add(new Point(-118.65702911071331, 34.07649252525452));
        Polyline polyline = new Polyline(polylinePoints);
        SimpleLineSymbol polylineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 3.0f);
        Graphic polylineGraphic = new Graphic(polyline, polylineSymbol);
        mGraphicsOverlay.getGraphics().add(polylineGraphic);
    }

    private void createPolygonGraphics() {
        PointCollection polygonPoints = new PointCollection(SpatialReferences.getWgs84());
        polygonPoints.add(new Point(-118.70372100524446, 34.03519536420519));
        polygonPoints.add(new Point(-118.71766916267414, 34.03505116445459));
        polygonPoints.add(new Point(-118.71923322580597, 34.04919407570509));
        polygonPoints.add(new Point(-118.71631129436038, 34.04915962906471));
        polygonPoints.add(new Point(-118.71526020370266, 34.059921300916244));
        polygonPoints.add(new Point(-118.71153226844807, 34.06035488360282));
        polygonPoints.add(new Point(-118.70803735010169, 34.05014385296186));
        polygonPoints.add(new Point(-118.69877903513455, 34.045182336992816));
        polygonPoints.add(new Point(-118.6979656552508, 34.040267760924316));
        polygonPoints.add(new Point(-118.70259112469694, 34.038800278306674));
        polygonPoints.add(new Point(-118.70372100524446, 34.03519536420519));
        Polygon polygon = new Polygon(polygonPoints);
        SimpleFillSymbol polygonSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.rgb(226, 119, 40),
                new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 2.0f));
        Graphic polygonGraphic = new Graphic(polygon, polygonSymbol);
        mGraphicsOverlay.getGraphics().add(polygonGraphic);
    }

    private void createGraphics() {
        createGraphicsOverlay();
        createPointGraphics();
        createPolylineGraphics();
        createPolygonGraphics();
    }

    /**Autenticacion**/
    private void setupOAuthManager() {
        String clientId = getResources().getString(R.string.client_id);
        String redirectUrl = getResources().getString(R.string.redirect_url);

        try {
            OAuthConfiguration oAuthConfiguration = new OAuthConfiguration("https://www.arcgis.com", clientId, redirectUrl);
            DefaultAuthenticationChallengeHandler authenticationChallengeHandler = new DefaultAuthenticationChallengeHandler(this);
            AuthenticationManager.setAuthenticationChallengeHandler(authenticationChallengeHandler);
            AuthenticationManager.addOAuthConfiguration(oAuthConfiguration);
        } catch (MalformedURLException e) {
            showError(e.getMessage());
        }
    }

    //Ruta mas corta
    private void mapClicked(Point location) {
        mPoint.add(location);

        float markerSize = 8.0f;
        float markerOutlineThickness = 2.0f;
        SimpleMarkerSymbol pointSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.DIAMOND, Color.rgb(226, 119, 40), markerSize);
        pointSymbol.setOutline(new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID,  Color.BLUE, markerOutlineThickness));
        Graphic pointGraphic = new Graphic(location, pointSymbol);
        mGraphicsOverlay.getGraphics().add(pointGraphic);
    }

    private void showError(String message) {
        Log.d("FindRoute", message);
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void findRoute() {
        String routeServiceURI = getResources().getString(R.string.routing_url);
        final RouteTask solveRouteTask = new RouteTask(getApplicationContext(), routeServiceURI);
        solveRouteTask.loadAsync();
        solveRouteTask.addDoneLoadingListener(() -> {
            if (solveRouteTask.getLoadStatus() == LoadStatus.LOADED) {
                final ListenableFuture<RouteParameters> routeParamsFuture = solveRouteTask.createDefaultParametersAsync();
                routeParamsFuture.addDoneListener(() -> {
                    try {
                        RouteParameters routeParameters = routeParamsFuture.get();
                        routeParameters.setFindBestSequence(false); // TODO: Reorder or not for min length path
                        List<Stop> stops = new ArrayList<>();
                        for (int i = 0; i < mPoint.size(); i++) {
                            stops.add(new Stop(mPoint.get(i)));
                        }
                        routeParameters.setStops(stops);
                        // Code from the next step goes here
                        final ListenableFuture<RouteResult> routeResultFuture = solveRouteTask.solveRouteAsync(routeParameters);
                        routeResultFuture.addDoneListener(() -> {
                            progressBar.setVisibility(View.GONE);
                            try {
                                RouteResult routeResult = routeResultFuture.get();
                                Route firstRoute = routeResult.getRoutes().get(0);
                                // Code from the next step goes here
                                Polyline routePolyline = firstRoute.getRouteGeometry();
                                SimpleLineSymbol routeSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 4.0f);
                                Graphic routeGraphic = new Graphic(routePolyline, routeSymbol);
                                mGraphicsOverlay.getGraphics().add(routeGraphic);

                                getPointsFromMap();

                            } catch (InterruptedException | ExecutionException e) {
                                showError("Solve RouteTask failed " + e.getMessage());
                            }
                        });

                    } catch (InterruptedException | ExecutionException e) {
                        showError("Cannot create RouteTask parameters " + e.getMessage());
                    }
                });
            } else {
                showError("Unable to load RouteTask " + solveRouteTask.getLoadStatus().toString());
            }
        });
    }

    private void getPointsFromMap() {
        Log.d("mGraphicsOverlay size", mGraphicsOverlay.getGraphics().size() + " ");

        for (int i = 0; i < mGraphicsOverlay.getGraphics().size(); i++) {
            Graphic g = mGraphicsOverlay.getGraphics().get(i);
            if (g.getGeometry() != null && g.getGeometry().getInternal() != null &&
                    g.getGeometry().getInternal().w() != null && !g.getGeometry().getInternal().w().equals("")) {
                Path path = new Gson().fromJson(g.getGeometry().getInternal().w(), Path.class);
                if (path.getPaths() != null && path.getPaths().length > 0) {
                    Log.d("Graphic " + i, ", " + g.getGeometry().getInternal().w());
                    List<Marker> points = path.getPoints();
                    Log.d("path.getPoints", " size " + points.size());

                    if (!points.isEmpty()) {
                        int adequateSpeed = 50;
                        request = new RoutePointRequestModel(points, 0, adequateSpeed, 1);
                        request.resultMarker = points.get(0);
                        showPointByInterval(request);
                    }
                }
            }
        }
    }

    private void showPointByInterval(RoutePointRequestModel request) {
        if (request.resultMarker != null) {
            showCurrentPositionHandler = new Handler();
            runnable = () -> {
                showPosition(request.resultMarker);
                showPointByInterval(Path.nextPoint(request));
            };
            showCurrentPositionHandler.postDelayed(runnable, request.getRefreshRate());
        }
    }

    private void showPosition(Marker m) {
        Log.d("showPosition: ", m.lat + " " + m.lon);

        SpatialReference sp = m.getSpatialReference();
        Point marker = new Point(m.lon, m.lat, sp);
        SimpleMarkerSymbol sms = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.DIAMOND, m.getColor(), 12);
        Graphic g = new Graphic(marker, sms);
        mGraphicsOverlay.getGraphics().add(g);
        if (mGraphicsOverlay.getGraphics().contains(g)) {
            mGraphicsOverlay.getGraphics().remove(currentPosition);
        }
        currentPosition = g;
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

    @OnClick(R.id.find_route)
    protected void onFindRouteClicked() {
        findRoute();
        hideBottomSheet();
        progressBar.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.clear_route)
    protected void onClearRouteClicked() {
        mPoint.clear();
        currentPosition = null;
        mGraphicsOverlay.getGraphics().clear();
        showCurrentPositionHandler.removeCallbacks(runnable);
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

    private void openAddMarkerFromMapDialog(MotionEvent e) {
        android.graphics.Point p = new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY()));
        Point point = mMapView.screenToLocation(p);
        Marker marker = new Marker(point.getX(), point.getY(), Marker.REPRESENTATION_UTM);
        ViewGroup vg = (ViewGroup) findViewById(android.R.id.content);
        marker.showAddFromMapDialog(MainActivity.this, vg, point, markers, new Marker.Viewer.AddMarkerCallback() {
            @Override
            public void onMarkerAdded() {
                Log.d("onMarkerAdded", "");
                Toast.makeText(MainActivity.this, getString(R.string.marker_added), Toast.LENGTH_LONG).show();
                showMarkers(markers);
                addMarkerFromMap = false;
            }

            @Override
            public void onMarkerAddingCanceled() {
                Log.d("onMarkerAddingCanceled", "");
                addMarkerFromMap = false;
            }
        });
    }

    private void showAddMarkerFromLatLongDialog() {
        Marker marker = new Marker(0, 0, Marker.REPRESENTATION_WGS84);
        ViewGroup vg = (ViewGroup) findViewById(android.R.id.content);
        Point point = new Point(0,0);
        marker.showAddFromLatLongDialog(MainActivity.this, vg, point, markers, new Marker.Viewer.AddMarkerCallback() {
            @Override
            public void onMarkerAdded() {
                Log.d("onMarkerAdded", "");
                Toast.makeText(MainActivity.this, getString(R.string.marker_added), Toast.LENGTH_LONG).show();
                showMarkers(markers);
            }

            @Override
            public void onMarkerAddingCanceled() {
                Log.d("onMarkerAddingCanceled", "");
            }
        });
    }

    private void showMarkers(List<Marker> markers) {
        for (Marker m: markers) {
            SpatialReference sp = m.getSpatialReference();
            Point marker = new Point(m.lon, m.lat, sp);
            SimpleMarkerSymbol sms = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, m.getColor(), 12);
            Graphic g = new Graphic(marker, sms);
            mGraphicsOverlay.getGraphics().add(g);
        }
    }
}

