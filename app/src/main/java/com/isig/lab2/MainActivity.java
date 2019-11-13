package com.isig.lab2;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.esri.arcgisruntime.ArcGISRuntimeException;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureEditResult;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.FeatureLayer;
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
import android.widget.SeekBar;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.gson.Gson;
import com.isig.lab2.models.Configuration;
import com.isig.lab2.models.Marker;
import com.isig.lab2.models.Path;
import com.isig.lab2.models.RoutePointRequestModel;

public class MainActivity extends AppCompatActivity {

    private static String CLAVE_BUSQUEDA ="Description";
    private static String VALOR_BUSQUEDA ="Grupo 3";

    private static double SIMULATION_REFRESH_RATE = 1;

    //Georeferenciacion
    private SearchView mSearchView = null;
    private boolean locatorLoaded = false;

    private FeatureLayer featureLayer;
    private ServiceFeatureTable serviceFeatureTable;
    private GraphicsOverlay mGraphicsOverlay;
    private LocatorTask mLocatorTask = null;
    private GeocodeParameters mGeocodeParameters = new GeocodeParameters();

    private List<Marker> markers = new ArrayList<>();
    private boolean addMarkerFromMap = true;
    private BottomSheetBehavior bottomSheetBehavior;

    private Graphic currentPosition;
    private Point currentPositionPoint;
    private int currentPositionColor = Path.getColorBySpeedIndex(Path.getMediumSpeedIndex());
    private Handler showCurrentPositionHandler;
    private Runnable runnable;
    private RoutePointRequestModel request;
    private double distTotal = 0;
    private boolean findBestSequenceForRoute = false;
    private boolean isSimulatingTour = false;

    private Configuration configuration = new Configuration();

    //Busqueda por categoria
    private GraphicsOverlay graphicsOverlay;
    //private LocatorTask locator = new LocatorTask(getString(R.string.url_server_busqueda));

    //Ruta mas corta
    private List<Point> selectedPoints = new ArrayList<>();

    @BindView(R.id.mapView) MapView mMapView;
    @BindView(R.id.bottom_sheet_markers) View bottomSheetMarkers;
    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.progress_bar) ProgressBar progressBar;
    @BindView(R.id.speedSeekBar) SeekBar speedSeekBar;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        progressBar.setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);

        fab.setOnClickListener(view -> showBottomSheet(bottomSheetMarkers));

        // *** Autenticacion ***
        setupOAuthManager();

        // set up map
        ArcGISMap map = new ArcGISMap(Basemap.Type.STREETS_VECTOR, -34.726272, -56.227631, 16);
        serviceFeatureTable = new ServiceFeatureTable(getString(R.string.url_server_puntos));
        featureLayer = new FeatureLayer(serviceFeatureTable);
        map.getOperationalLayers().add(featureLayer);
        mMapView.setMap(map);

        // *** Georeferenciacion ***
        setupLocator();

        /**Ruta mas corta**/
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (locatorLoaded) {
                    if (addMarkerFromMap) {
                        openAddMarkerFromMapDialog(e);
                    } else {
                        android.graphics.Point screenPoint = new android.graphics.Point(
                                Math.round(e.getX()),
                                Math.round(e.getY()));
                        Point mapPoint = mMapView.screenToLocation(screenPoint);
                        //mapClicked(mapPoint);
                        addFeature(mapPoint);
                    }
                }
                return super.onSingleTapConfirmed(e);
            }
        });

        setSpeedSeekBar();

        createGraphicsOverlay();
    }

    private void setSpeedSeekBar() {
        speedSeekBar.setMax(Path.TOUR_TRAVEL_INTERVALS-1);
        speedSeekBar.setProgress(Path.getMediumSpeedIndex());
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (request != null && distTotal > 0) {
                    Log.d("onProgressChanged", "progress: " + progress + ", speed: " + Path.getSpeeds(distTotal)[Path.TOUR_TRAVEL_INTERVALS-progress-1]);
                    request.speed = Path.getSpeedByIndex(distTotal, progress);
                    currentPositionColor = Path.getColorBySpeedIndex(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
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
    protected void onPause() {
        mMapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
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
                        List<GeocodeResult> geocodeResults = geocodeFuture.get();
                        if (geocodeResults.size() > 0) {
                            displaySearchResult(geocodeResults.get(0));
                        } else {
                            Toast.makeText(getApplicationContext(), getString(R.string.nothing_found) + " " + query, Toast.LENGTH_LONG).show();
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
                Log.d("Init", "Cargo el locator");
                progressBar.setVisibility(View.GONE);
                locatorLoaded = true;
                desplegarInfoLocator(mLocatorTask);

            } else if (mSearchView != null) {
                mSearchView.setEnabled(false);
                Log.d("Init", "No cargo el locator");
            }
        });
        mLocatorTask.loadAsync();
    }

    private void desplegarInfoLocator(LocatorTask locatorTask) {
        // Get LocatorInfo from a loaded LocatorTask
        LocatorInfo locatorInfo = locatorTask.getLocatorInfo();
        List<String> resultAttributeNames = new ArrayList<>();

        // Loop through all the attributes available
        for (LocatorAttribute resultAttribute : locatorInfo.getResultAttributes()) {
            resultAttributeNames.add(resultAttribute.getDisplayName());
            // Use in adapter etc...
            System.out.print(resultAttribute.getName() + ": " + resultAttribute.getDisplayName() + " ");
        }
    }

    //Mostrar puntos, lineas y poligonos
    private void createGraphicsOverlay() {
        mGraphicsOverlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(mGraphicsOverlay);
    }

    /**
     * Autenticacion
     **/
    private void setupOAuthManager() {
        try {
            OAuthConfiguration oAuthConfiguration =
                    new OAuthConfiguration(getString(R.string.page_url), getString(R.string.client_id), getString(R.string.redirect_url));
            DefaultAuthenticationChallengeHandler authenticationChallengeHandler = new DefaultAuthenticationChallengeHandler(this);
            AuthenticationManager.setAuthenticationChallengeHandler(authenticationChallengeHandler);
            AuthenticationManager.addOAuthConfiguration(oAuthConfiguration);
        } catch (MalformedURLException e) {
            showError(e.getMessage());
        }
    }

    public void addFeature(Point mapPoint) {
        selectedPoints.add(mapPoint);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("Description", VALOR_BUSQUEDA); // Coded Values: [1: Manatee] etc...
        attributes.put("Event Website",VALOR_BUSQUEDA); // Coded Values: [0: No] , [1: Yes]
        attributes.put("Recommend Attending", "Yes");
        attributes.put("Event_Type", 1);

        // Create a new feature from the attributes and an existing point geometry, and then add the feature
        Feature addedFeature = serviceFeatureTable.createFeature(attributes, mapPoint);
        final ListenableFuture<Void> addFeatureFuture = serviceFeatureTable.addFeatureAsync(addedFeature);
        addFeatureFuture.addDoneListener(() -> {
            try {
                addFeatureFuture.get();

                // apply the edits
                final ListenableFuture<List<FeatureEditResult>> applyEditsFuture = serviceFeatureTable.applyEditsAsync();
                applyEditsFuture.addDoneListener(() -> {
                    try {
                        final List<FeatureEditResult> featureEditResults = applyEditsFuture.get();
                        // if required, can check the edits applied in this operation
                        Log.d("Number of edits", "" + featureEditResults.size());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                });

            } catch (InterruptedException | ExecutionException e) {
                if (e.getCause() instanceof ArcGISRuntimeException) {
                    ArcGISRuntimeException agsEx = (ArcGISRuntimeException)e.getCause();
                    Log.d("Add Feature Error", agsEx.getErrorCode() + "\n=" + agsEx.getMessage());
                } else {
                    e.printStackTrace();
                }
            }
        });
    }

    private void buscarNuestrosPuntos(String key, String value) {
        featureLayer.clearSelection();

        QueryParameters query = new QueryParameters();
        query.setWhereClause("upper("+key+") LIKE '%" + value+ "%'");
        final ListenableFuture<FeatureQueryResult> future = serviceFeatureTable.queryFeaturesAsync(query);
        future.addDoneListener(() -> {
            try {
                FeatureQueryResult result = future.get();
                Iterator<Feature> resultIteratorPrueba = result.iterator();
                if (resultIteratorPrueba.hasNext()) {
                    for (Feature feature : result) {
                        Envelope envelope = feature.getGeometry().getExtent();
                        mMapView.setViewpointGeometryAsync(envelope, 10);
                        featureLayer.selectFeature(feature);
                    }
                } else {
                    Toast.makeText(this, "No encuentra puntos con Descripcion: " + "Grupo 3", Toast.LENGTH_LONG).show();
                    Log.e("buscarNuestrosPuntos", "No encuentra puntos con "+key+": " + value);
                }
            } catch (Exception e) {
                String error = "Feature search failed for: " + "Grupo 3" + ". Error: " + e.getMessage();
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                Log.e("buscarNuestrosPuntos", error);
            }
        });
    }

    private void eliminarNuestrosPuntos(String key, String value) {
        featureLayer.clearSelection();

        QueryParameters query = new QueryParameters();
        query.setWhereClause("upper("+key+") LIKE '%" + value+ "%'");
        final ListenableFuture<FeatureQueryResult> future = serviceFeatureTable.queryFeaturesAsync(query);
        future.addDoneListener(() -> {
            try {
                FeatureQueryResult result = future.get();
                Iterator<Feature> resultIteratorPrueba = result.iterator();
                if (resultIteratorPrueba.hasNext()) {
                    serviceFeatureTable.deleteFeaturesAsync(result).get();
                    final List<FeatureEditResult> featureEditResults = serviceFeatureTable.applyEditsAsync().get();
                } else {
                    Toast.makeText(this, "No encuentra puntos con Descripcion: " + "Grupo 3", Toast.LENGTH_LONG).show();
                    Log.e("eliminarNuestrosPuntos", "No encuentra puntos con "+key+": " + value);
                }
            } catch (Exception e) {
                String error = "Feature search failed for: " + "Grupo 3" + ". Error: " + e.getMessage();
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                Log.e("eliminarNuestrosPuntos", error);
            }
        });
    }

    //Ruta mas corta
    private void mapClicked(Point location) {
        selectedPoints.add(location);

        float markerSize = 8.0f;
        float markerOutlineThickness = 2.0f;
        SimpleMarkerSymbol pointSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.DIAMOND, Color.rgb(226, 119, 40), markerSize);
        pointSymbol.setOutline(new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, markerOutlineThickness));
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
                        routeParameters.setFindBestSequence(findBestSequenceForRoute);
                        List<Stop> stops = new ArrayList<>();
                        for (int i = 0; i < selectedPoints.size(); i++) {
                            stops.add(new Stop(selectedPoints.get(i)));
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
        speedSeekBar.setVisibility(View.VISIBLE);
        Log.d("mGraphicsOverlay size", mGraphicsOverlay.getGraphics().size() + " ");

        for (int i = 0; i < mGraphicsOverlay.getGraphics().size(); i++) {
            Graphic g = mGraphicsOverlay.getGraphics().get(i);
            if (g.getGeometry() != null && g.getGeometry().getInternal() != null &&
                    g.getGeometry().getInternal().w() != null && !g.getGeometry().getInternal().w().equals("")) {
                Path path = new Gson().fromJson(g.getGeometry().getInternal().w(), Path.class);
                if (path.getPaths() != null && path.getPaths().length > 0) {
                    Log.d("Graphic " + i, ", " + g.getGeometry().getInternal().w());
                    List<Marker> markerPoints = path.getPoints();
                    Log.d("path.getPoints", " size " + markerPoints.size());

                    if (!markerPoints.isEmpty()) {
                        distTotal = Path.largoCaminoEnKm(markerPoints);
                        int[] speeds = Path.getSpeeds(distTotal);
                        int adequateSpeed = speeds[Path.getMediumSpeedIndex()];
                        if (speedSeekBar != null) {
                            adequateSpeed = Path.getSpeedByIndex(distTotal, speedSeekBar.getProgress());
                        }
                        request = new RoutePointRequestModel(markerPoints, 0, adequateSpeed, SIMULATION_REFRESH_RATE);
                        request.resultMarker = markerPoints.get(0);
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
                showPosition(request.resultMarker, SimpleMarkerSymbol.Style.DIAMOND, 12);
                showPointByInterval(Path.nextPoint(request));
            };
            showCurrentPositionHandler.postDelayed(runnable, request.getRefreshRate());
        } else {
            speedSeekBar.setVisibility(View.INVISIBLE);
        }
    }

    private void showPosition(Marker m, SimpleMarkerSymbol.Style style, int size) {
        if (m != null) {
            Log.d("showPosition: ", m.lat + " " + m.lon);
            currentPositionPoint = new Point(m.lon, m.lat, m.getSpatialReference());
        }
        SimpleMarkerSymbol sms = new SimpleMarkerSymbol(style, currentPositionColor, size);
        Graphic g = new Graphic(currentPositionPoint, sms);
        mGraphicsOverlay.getGraphics().add(g);
        if (mGraphicsOverlay.getGraphics().contains(g)) {
            mGraphicsOverlay.getGraphics().remove(currentPosition);
        }
        currentPosition = g;
    }

    @OnClick(R.id.clear_points)
    protected void onClearPointsClicked() {
        eliminarNuestrosPuntos(CLAVE_BUSQUEDA, VALOR_BUSQUEDA);
        hideBottomSheet();
    }

    @OnClick(R.id.create_route)
    protected void onCreateRouteClicked() {
        findRoute();
        progressBar.setVisibility(View.VISIBLE);
        hideBottomSheet();
    }

    @OnClick(R.id.load_routes)
    protected void onLoadRoutesClicked() {
        // TODO: cargar rutas
        hideBottomSheet();
    }

    @OnClick(R.id.clear_routes)
    protected void onClearRouteClicked() {
        selectedPoints.clear();
        currentPosition = null;
        mGraphicsOverlay.getGraphics().clear(); // TODO: limpiar tabla de rutas
        if (showCurrentPositionHandler != null) {
            showCurrentPositionHandler.removeCallbacks(runnable);
        }
        hideBottomSheet();
    }

    @OnClick(R.id.simulate_tour)
    protected void onSimulateTourClicked() {
        getPointsFromMap();
        hideBottomSheet();
    }

    @OnClick(R.id.text_config)
    protected void onConfigClicked() {
        ViewGroup vg = (ViewGroup) findViewById(android.R.id.content);
        configuration.showConfigurationDialog(this, configuration, vg, new Configuration.Viewer.ChangeConfigCallback() {
            @Override
            public void onConfigChanged(Configuration configuration) {
                Toast.makeText(MainActivity.this, getString(R.string.config_saved), Toast.LENGTH_LONG).show();
                findBestSequenceForRoute = configuration.shortestRoute;
            }

            @Override
            public void onConfigCanceled() {
                Log.d("onConfigCanceled", "configuration not saved");
            }
        });
        hideBottomSheet();
    }

    @OnClick(R.id.text_close_sheet)
    protected void onCancelBottomSheetClicked() {
        hideBottomSheet();
    }

    public void hideBottomSheet() {
        if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            bottomSheetBehavior = null;
        }
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

    private void openAddMarkerFromMapDialog(MotionEvent e) {
        android.graphics.Point p = new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY()));
        Point point = mMapView.screenToLocation(p);
        Marker marker = new Marker(point.getX(), point.getY(), Marker.REPRESENTATION_UTM);
        ViewGroup vg = (ViewGroup) findViewById(android.R.id.content);
        marker.showAddFromMapDialog(MainActivity.this, vg, point, markers, new Marker.Viewer.AddMarkerCallback() {
            @Override
            public void onMarkerAdded(Marker marker) {
                Log.d("onMarkerAdded", "");
                Toast.makeText(MainActivity.this, getString(R.string.marker_added), Toast.LENGTH_LONG).show();
                addFeature(point);
                //showMarkers(markers);
            }

            @Override
            public void onMarkerAddingCanceled() {
                Log.d("onMarkerAddingCanceled", "");
            }
        });
    }

    private void showAddMarkerFromLatLongDialog() {
        Marker marker = new Marker(0, 0, Marker.REPRESENTATION_WGS84);
        ViewGroup vg = (ViewGroup) findViewById(android.R.id.content);
        Point point = new Point(0, 0);
        marker.showAddFromLatLongDialog(MainActivity.this, vg, point, markers, new Marker.Viewer.AddMarkerCallback() {
            @Override
            public void onMarkerAdded(Marker marker) {
                Log.d("onMarkerAdded", "");
                Toast.makeText(MainActivity.this, getString(R.string.marker_added), Toast.LENGTH_LONG).show();
                addFeature(new Point(marker.lon, marker.lat));
                //showMarkers(markers);
            }

            @Override
            public void onMarkerAddingCanceled() {
                Log.d("onMarkerAddingCanceled", "");
            }
        });
    }

    private void showMarkers(List<Marker> markers) {
        for (Marker m : markers) {
            SpatialReference sp = m.getSpatialReference();
            Point marker = new Point(m.lon, m.lat, sp);
            SimpleMarkerSymbol sms = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, m.getColor(), 12);
            Graphic g = new Graphic(marker, sms);
            mGraphicsOverlay.getGraphics().add(g);
        }
    }
}

