package com.isig.lab2;

import android.annotation.SuppressLint;
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
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
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
import com.isig.lab2.utils.APIUtils;
import com.isig.lab2.utils.GeographicUtils;

public class MainActivity extends AppCompatActivity {

    private static String CLAVE_BUSQUEDA ="Description";
    private static String VALOR_BUSQUEDA ="Grupo 3";

    private static double SIMULATION_REFRESH_RATE = 1;

    // Puntos
    private FeatureLayer featureLayerPuntos;
    private ServiceFeatureTable serviceFeatureTablePuntos;
    private List<Feature> selectedFeaturePuntos = new ArrayList<>();

    // Rutas
    private ServiceFeatureTable serviceFeatureTableRutas;
    private FeatureLayer featureLayerRutas;
    private Feature selectedFeatureRuta = null;

    private GraphicsOverlay mGraphicsOverlay;
    private LocatorTask mLocatorTask = null;
    private GeocodeParameters mGeocodeParameters = new GeocodeParameters();
    private boolean locatorLoaded = false;

    private boolean addMarkerFromMap = true;
    private BottomSheetBehavior bottomSheetBehavior;

    // current position
    private Graphic currentPosition;
    private Point currentPositionPoint;
    private int currentPositionColor = Path.getColorBySpeedIndex(Path.getMediumSpeedIndex());
    private SimpleMarkerSymbol.Style currentPositionStyle = SimpleMarkerSymbol.Style.DIAMOND;
    private int currentPositionSize = 15;
    private Handler showCurrentPositionHandler;
    private Runnable runnable;
    private RoutePointRequestModel request;
    private double distTotal = 0;
    private boolean findBestSequenceForRoute = false;

    private Configuration configuration = new Configuration();

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.mapView) MapView mMapView;
    @BindView(R.id.progress_bar) ProgressBar progressBar;
    @BindView(R.id.add_points_toggle_text) TextView addPointToggleText;
    @BindView(R.id.create_point) TextView modeText;
    @BindView(R.id.bottom_sheet_markers) View bottomSheetMarkers;
    @BindView(R.id.speedSeekBar) SeekBar speedSeekBar;
    @BindView(R.id.fab) FloatingActionButton fab;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            int actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) addPointToggleText.getLayoutParams();
            p.setMargins(5,actionBarHeight + 5, 5, 5);
            addPointToggleText.requestLayout();
        }

        progressBar.setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);

        fab.setOnClickListener(view -> showBottomSheet(bottomSheetMarkers));

        // *** Autenticacion ***
        setupOAuthManager();

        // set up map
        ArcGISMap map = new ArcGISMap(Basemap.Type.STREETS_VECTOR, 39.222678, -105.998207, 16);

        serviceFeatureTablePuntos = new ServiceFeatureTable(getString(R.string.url_server_puntos));
        featureLayerPuntos = new FeatureLayer(serviceFeatureTablePuntos);
        map.getOperationalLayers().add(featureLayerPuntos);

        serviceFeatureTableRutas = new ServiceFeatureTable(getString(R.string.url_server_rutas));
        featureLayerRutas = new FeatureLayer(serviceFeatureTableRutas);
        map.getOperationalLayers().add(featureLayerRutas);

        mMapView.setMap(map);

        // *** Georeferenciacion ***
        setupLocator();

        /**Ruta mas corta**/
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (locatorLoaded) {
                    android.graphics.Point screenPoint = new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY()));

                    final ListenableFuture<IdentifyLayerResult> identifyFuturePunto = mMapView.identifyLayerAsync(featureLayerPuntos, screenPoint, 12, false, 25);
                    identifyFuturePunto.addDoneListener(() -> {
                        try {
                            IdentifyLayerResult identifyLayerResult = identifyFuturePunto.get();

                            if (identifyLayerResult.getLayerContent() instanceof FeatureLayer) {
                                featureLayerPuntos = (FeatureLayer) identifyLayerResult.getLayerContent();
                            }

                            if (!identifyLayerResult.getElements().isEmpty() && featureLayerPuntos != null) {
                                for (GeoElement identifiedElement : identifyLayerResult.getElements()) {
                                    if (identifiedElement instanceof Feature) {
                                        Feature identifiedFeature = (Feature) identifiedElement;

                                        if (identifiedElement.getGeometry() instanceof Point) {
                                            if (!GeographicUtils.isSelected(selectedFeaturePuntos, identifiedFeature)) {
                                                featureLayerPuntos.selectFeature(identifiedFeature);
                                                selectedFeaturePuntos.add(identifiedFeature);
                                            } else {
                                                featureLayerPuntos.unselectFeature(identifiedFeature);
                                                GeographicUtils.removeFeatureFromList(selectedFeaturePuntos, identifiedFeature);
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (addMarkerFromMap) {
                                    openAddMarkerFromMapDialog(e);
                                }
                            }

                        } catch (InterruptedException | ExecutionException ex) {
                            ex.printStackTrace();
                        }
                    });

                    final ListenableFuture<IdentifyLayerResult> identifyFutureRuta = mMapView.identifyLayerAsync(featureLayerRutas, screenPoint, 15, false, 25);
                    identifyFutureRuta.addDoneListener(() -> {
                        if (!addMarkerFromMap) {
                            try {
                                IdentifyLayerResult identifyLayerResult = identifyFutureRuta.get();

                                if (identifyLayerResult.getLayerContent() instanceof FeatureLayer) {
                                    featureLayerRutas = (FeatureLayer) identifyLayerResult.getLayerContent();
                                }

                                if (!identifyLayerResult.getElements().isEmpty() && featureLayerRutas != null) {
                                    for (GeoElement identifiedElement : identifyLayerResult.getElements()) {
                                        if (identifiedElement instanceof Feature) {
                                            Feature identifiedFeature = (Feature) identifiedElement;

                                            if (identifiedElement.getGeometry() instanceof Polyline) {
                                                if (selectedFeatureRuta != null) {
                                                    featureLayerRutas.unselectFeature(selectedFeatureRuta);
                                                }
                                                selectedFeatureRuta = identifiedFeature;
                                                featureLayerRutas.selectFeature(selectedFeatureRuta);
                                            }
                                        }
                                    }
                                }

                            } catch (InterruptedException | ExecutionException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
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
        getMenuInflater().inflate(R.menu.options_menu, menu);
        final MenuItem miSearch = showMenuItem(menu.findItem(R.id.search), true);
        if(miSearch != null) {
            final SearchView searchView = (SearchView) miSearch.getActionView();
            searchView.setMaxWidth(Integer.MAX_VALUE);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    queryLocator(query);
                    return false;
                }
                @Override
                public boolean onQueryTextChange(String s) {
                    return false;
                }
            });
        }
        super.onCreateOptionsMenu(menu);
        return true;
    }

    public MenuItem showMenuItem(MenuItem item, boolean show) {
        if (item != null) {
            item.setVisible(show);
        }
        return item;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.search) {
            if (!item.isActionViewExpanded()) {
                item.expandActionView();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                        selectPlaceFromList(geocodeResults);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    geocodeFuture.removeDoneListener(this); // Done searching, remove the listener.
                }
            });
        }
    }

    public void selectPlaceFromList(List<GeocodeResult> list) {
        if (list != null && list.size() > 0) {
            Log.d("selectPlaceFromList", "list size: " + list.size());
            final CharSequence[] items = new CharSequence[list.size()];
            final GeocodeResult[] geos = new GeocodeResult[list.size()];
            int insertIndex = 0;
            for (int i = 0; i < list.size(); i++) {
                GeocodeResult elem = list.get(i);
                Map<String, Object> map = elem.getAttributes();
                if (map != null) {
                    if (map.containsKey("Country") && map.get("Country").equals("USA")) {
                        if (elem.getLabel() != null) {
                            items[insertIndex] = elem.getLabel();
                            geos[insertIndex] = elem;
                            insertIndex++;
                        }
                    }
                }
            }

            if (items.length == 1 && geos[0] != null) {
                displaySearchResult(geos[0]);
            } else if (items.length > 1) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Selecciona una ubicación");
                builder.setItems(items, (dialog, item) -> {
                    displaySearchResult(geos[item]);
                    dialog.dismiss();
                }).show();
            } else {
                showNoPlaceFoundDialog();
            }
        } else {
            showNoPlaceFoundDialog();
        }
    }

    private void showNoPlaceFoundDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("404 - Ubicación no encontrada");
        builder.setMessage("No se encontraron ubicaciones asociadas a la busqueda realizada");
        builder.setPositiveButton(getString(android.R.string.ok), null);
        builder.show();
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

    // Georeferenciacion
    private void displaySearchResult(GeocodeResult geocodedLocation) {
        String displayLabel = geocodedLocation.getLabel();
        TextSymbol textLabel = new TextSymbol(18, displayLabel, Color.rgb(192, 32, 32), TextSymbol.HorizontalAlignment.CENTER, TextSymbol.VerticalAlignment.BOTTOM);
        Graphic textGraphic = new Graphic(geocodedLocation.getDisplayLocation(), textLabel);
        Graphic mapMarker = new Graphic(geocodedLocation.getDisplayLocation(), geocodedLocation.getAttributes(),
                new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.SQUARE, Color.rgb(255, 0, 0), 12.0f));
        ListenableList allGraphics = mGraphicsOverlay.getGraphics();
        allGraphics.clear();
        allGraphics.add(mapMarker);
        allGraphics.add(textGraphic);
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

            } else {
                Log.d("Init", "No cargo el locator");
            }
        });
        mLocatorTask.loadAsync();
    }

    private void desplegarInfoLocator(LocatorTask locatorTask) {
        LocatorInfo locatorInfo = locatorTask.getLocatorInfo();
        List<String> resultAttributeNames = new ArrayList<>();

        for (LocatorAttribute resultAttribute : locatorInfo.getResultAttributes()) {
            resultAttributeNames.add(resultAttribute.getDisplayName());
            System.out.print(resultAttribute.getName() + ": " + resultAttribute.getDisplayName() + " ");
        }
    }

    // Mostrar puntos, lineas y poligonos
    private void createGraphicsOverlay() {
        mGraphicsOverlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(mGraphicsOverlay);
    }

    // Autenticacion
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

    public void addFeaturePoint(Point mapPoint, boolean selectPoint) {

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("Description", VALOR_BUSQUEDA);
        attributes.put("Event Website",VALOR_BUSQUEDA);
        attributes.put("Recommend Attending", "Yes");
        attributes.put("Event_Type", 1);

        Feature addedFeature = serviceFeatureTablePuntos.createFeature(attributes, mapPoint);
        final ListenableFuture<Void> addFeatureFuture = serviceFeatureTablePuntos.addFeatureAsync(addedFeature);
        addFeatureFuture.addDoneListener(() -> {
            try {
                addFeatureFuture.get();

                if (selectPoint) {
                    featureLayerPuntos.selectFeature(addedFeature);
                    selectedFeaturePuntos.add(addedFeature);
                }

                // apply the edits
                final ListenableFuture<List<FeatureEditResult>> applyEditsFuture = serviceFeatureTablePuntos.applyEditsAsync();
                applyEditsFuture.addDoneListener(() -> {
                    try {
                        final List<FeatureEditResult> featureEditResults = applyEditsFuture.get();
                        Log.d("addFeaturePoint", "number of saved feature points: " + featureEditResults.size());

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
        featureLayerPuntos.clearSelection();

        QueryParameters query = new QueryParameters();
        query.setWhereClause("upper("+key+") LIKE '%" + value+ "%'");
        final ListenableFuture<FeatureQueryResult> future = serviceFeatureTablePuntos.queryFeaturesAsync(query);
        future.addDoneListener(() -> {
            try {
                FeatureQueryResult result = future.get();
                Iterator<Feature> resultIteratorPrueba = result.iterator();
                if (resultIteratorPrueba.hasNext()) {
                    for (Feature feature : result) {
                        Envelope envelope = feature.getGeometry().getExtent();
                        mMapView.setViewpointGeometryAsync(envelope, 10);
                        featureLayerPuntos.selectFeature(feature);
                        selectedFeaturePuntos.add(feature);
                    }
                } else {
                    Toast.makeText(this, "No encuentra puntos con Descripcion: " + VALOR_BUSQUEDA, Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                String error = "Feature search failed for: " + VALOR_BUSQUEDA + ". Error: " + e.getMessage();
                showError(error);
                Log.e("buscarNuestrosPuntos", error);
            }
        });
    }

    private void eliminarNuestrosPuntos(String key, String value) {
        featureLayerPuntos.clearSelection();

        QueryParameters query = new QueryParameters();
        query.setWhereClause("upper("+key+") LIKE '%" + value+ "%'");
        final ListenableFuture<FeatureQueryResult> future = serviceFeatureTablePuntos.queryFeaturesAsync(query);
        future.addDoneListener(() -> {
            try {
                FeatureQueryResult result = future.get();
                Iterator<Feature> resultIteratorPrueba = result.iterator();
                if (resultIteratorPrueba.hasNext()) {
                    serviceFeatureTablePuntos.deleteFeaturesAsync(result).get();
                    final List<FeatureEditResult> featureEditResults = serviceFeatureTablePuntos.applyEditsAsync().get();
                } else {
                    Toast.makeText(this, "No encuentra puntos con Descripcion: " + VALOR_BUSQUEDA, Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                String error = "Feature search failed for: " + VALOR_BUSQUEDA + ". Error: " + e.getMessage();
                showError(error);
                Log.e("eliminarNuestrosPuntos", error);
            }
        });
    }

    public void addFeatureRoute(Polyline route, final boolean selectRoute){

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("Trailtype", 2);
        attributes.put("Condition", 2);
        attributes.put("Notes", VALOR_BUSQUEDA);
        attributes.put("Recordedon", null);
        attributes.put("Difficulty", 2);
        attributes.put("Segregation", 0);
        Polyline ruta = (Polyline) GeometryEngine.removeM(route);

        serviceFeatureTableRutas.loadAsync();
        serviceFeatureTableRutas.addDoneLoadingListener(() -> {

            Feature addedFeature = serviceFeatureTableRutas.createFeature(attributes, ruta);
            final ListenableFuture<Void> addFeatureFuture = serviceFeatureTableRutas.addFeatureAsync(addedFeature);
            addFeatureFuture.addDoneListener(() -> {
                try {
                    addFeatureFuture.get();

                    if (selectRoute) {
                        featureLayerRutas.clearSelection();
                        featureLayerRutas.selectFeature(addedFeature);
                        selectedFeatureRuta = addedFeature;
                    }

                    final ListenableFuture<List<FeatureEditResult>> applyEditsFuture = serviceFeatureTableRutas.applyEditsAsync();
                    applyEditsFuture.addDoneListener(() -> {
                        try {
                            final List<FeatureEditResult> featureEditResults = applyEditsFuture.get();
                            Log.d("addFeatureRoute", "number of saved feature routes: " + featureEditResults.size());
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    });

                } catch (InterruptedException | ExecutionException e) {
                    if (e.getCause() instanceof ArcGISRuntimeException) {
                        ArcGISRuntimeException agsEx = (ArcGISRuntimeException)e.getCause();
                        Log.d("addFeatureRoute", "Add Feature Error :"+ agsEx.getErrorCode()+"\n\t"+ agsEx.getMessage());
                    } else {
                        e.printStackTrace();
                    }
                }
            });
        });

    }

    private void buscarNuestrasRutas() {
        featureLayerRutas.clearSelection();
        
        QueryParameters query = new QueryParameters();
        query.setWhereClause("upper(Notes) LIKE '%" + VALOR_BUSQUEDA + "%'");
        
        final ListenableFuture<FeatureQueryResult> future = serviceFeatureTableRutas.queryFeaturesAsync(query);
        future.addDoneListener(() -> {
            try {
                FeatureQueryResult result = future.get();

                Iterator<Feature> resultIterator = result.iterator();
                if (resultIterator.hasNext()) {
                    Feature feature = resultIterator.next();
                    Envelope envelope = feature.getGeometry().getExtent();
                    mMapView.setViewpointGeometryAsync(envelope, 10);
                    
                    featureLayerRutas.selectFeature(feature);
                    featureLayerRutas.setFeatureVisible(feature, true);
                } else {
                    Toast.makeText(this, "No encuentra la ruta con Notes: " + VALOR_BUSQUEDA, Toast.LENGTH_LONG).show();
                }
                
            } catch (Exception e) {
                String error = "Feature search failed for: " + VALOR_BUSQUEDA + ". Error: " + e.getMessage();
                showError(error);
                Log.e("buscarNuestrasRutas", error);
            }
        });
    }

    //Ruta mas corta
    private void mapClicked(Point location) {

        float markerSize = 8.0f;
        float markerOutlineThickness = 2.0f;
        SimpleMarkerSymbol pointSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.DIAMOND, Color.rgb(226, 119, 40), markerSize);
        pointSymbol.setOutline(new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, markerOutlineThickness));
        Graphic pointGraphic = new Graphic(location, pointSymbol);
        mGraphicsOverlay.getGraphics().add(pointGraphic);
    }

    private void showError(String message) {
        Log.d("showError", message);
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void findRoute() {
        if (selectedFeaturePuntos.size() > 1) {
            final RouteTask solveRouteTask = new RouteTask(getApplicationContext(), getString(R.string.routing_url));
            solveRouteTask.loadAsync();
            solveRouteTask.addDoneLoadingListener(() -> {
                if (solveRouteTask.getLoadStatus() == LoadStatus.LOADED) {
                    final ListenableFuture<RouteParameters> routeParamsFuture = solveRouteTask.createDefaultParametersAsync();
                    routeParamsFuture.addDoneListener(() -> {
                        try {
                            RouteParameters routeParameters = routeParamsFuture.get();
                            routeParameters.setFindBestSequence(findBestSequenceForRoute);
                            List<Stop> stops = new ArrayList<>();
                            for (int i = 0; i < selectedFeaturePuntos.size(); i++) {
                                if (selectedFeaturePuntos.get(i).getGeometry() instanceof Point) {
                                    stops.add(new Stop((Point) selectedFeaturePuntos.get(i).getGeometry()));
                                }
                            }
                            routeParameters.setStops(stops);

                            final ListenableFuture<RouteResult> routeResultFuture = solveRouteTask.solveRouteAsync(routeParameters);
                            routeResultFuture.addDoneListener(() -> {
                                progressBar.setVisibility(View.GONE);
                                try {
                                    RouteResult routeResult = routeResultFuture.get();
                                    Route firstRoute = routeResult.getRoutes().get(0);

                                    Polyline routePolyline = firstRoute.getRouteGeometry();
                                    SimpleLineSymbol routeSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 4.0f);
                                    Graphic routeGraphic = new Graphic(routePolyline, routeSymbol);
                                    mGraphicsOverlay.getGraphics().add(routeGraphic);

                                    addFeatureRoute(routePolyline, true);

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
        } else {
            showError("Selecciona al menos 2 puntos para crear una ruta");
        }
    }

    private void simulateTour() {
        speedSeekBar.setVisibility(View.VISIBLE);
        Log.d("mGraphicsOverlay size", mGraphicsOverlay.getGraphics().size() + " ");

        Geometry g = null;
        if (selectedFeatureRuta != null) {
            g = selectedFeatureRuta.getGeometry();
            parsePath(g, 0, Marker.REPRESENTATION_UTM);
        } else {
            for (int i = 0; i < mGraphicsOverlay.getGraphics().size(); i++) {
                g = mGraphicsOverlay.getGraphics().get(i).getGeometry();
                parsePath(g, i, Marker.REPRESENTATION_WGS84);
            }
        }
    }

    private void parsePath(Geometry g, int i, int representation) {
        if (g instanceof Polyline && g.getInternal() != null && g.getInternal().w() != null && !g.getInternal().w().equals("")) {
            Path path = new Gson().fromJson(g.getInternal().w(), Path.class);
            if (path.getPaths() != null && path.getPaths().length > 0) {
                Log.d("Graphic " + i, ", " + g.getInternal().w());
                List<Marker> markerPoints = path.getPoints(representation);
                Log.d("path.getPoints", " size: " + markerPoints.size());

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

    private void showPointByInterval(RoutePointRequestModel request) {
        if (request.resultMarker != null) {
            showCurrentPositionHandler = new Handler();
            runnable = () -> {
                showPosition(request.resultMarker, currentPositionStyle, currentPositionSize);
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
        mGraphicsOverlay.getGraphics().remove(currentPosition);
        currentPosition = g;
    }

    @OnClick(R.id.create_point)
    protected void onCreatePointsClicked() {
        addMarkerFromMap = !addMarkerFromMap;
        addPointToggleText.setText(addMarkerFromMap ? getString(R.string.add_marker_mode) : getString(R.string.select_mode));
        modeText.setText(addMarkerFromMap ? getString(R.string.select_mode) : getString(R.string.add_marker_mode));
        hideBottomSheet();
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

    @OnClick(R.id.deselect_all)
    protected void onLoadRoutesClicked() {
        selectedFeaturePuntos.clear();
        featureLayerPuntos.clearSelection();
        selectedFeatureRuta = null;
        featureLayerRutas.clearSelection();
        currentPosition = null;
        speedSeekBar.setVisibility(View.INVISIBLE);
        hideBottomSheet();
    }

    @OnClick(R.id.clear_routes)
    protected void onClearRouteClicked() {
        selectedFeaturePuntos.clear();
        featureLayerPuntos.clearSelection();
        currentPosition = null;
        speedSeekBar.setVisibility(View.INVISIBLE);
        mGraphicsOverlay.getGraphics().clear(); // TODO: limpiar tabla de rutas
        if (showCurrentPositionHandler != null) {
            showCurrentPositionHandler.removeCallbacks(runnable);
        }
        hideBottomSheet();
    }

    @OnClick(R.id.simulate_tour)
    protected void onSimulateTourClicked() {
        simulateTour();
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
        String url = "https://services.arcgisonline.com/arcgis/rest/services/Demographics/USA_1990-2000_Population_Change/MapServer/3/query";
        String params = "f=json&spatialRel=esriSpatialRelIntersects&returnIdsOnly=true&geometry=%7B%22rings%22%3A%5B%5B%5B-93.266744044856253%2C44.9847001553116%5D%2C%5B-93.266596845520226%2C44.984694070854012%5D%2C%5B-93.266451636895056%2C44.98467589976687%5D%2C%5B-93.266310382765724%2C44.98464588779413%5D%2C%5B-93.266174993429686%2C44.984604440814181%5D%2C%5B-93.266047299859295%2C44.984552119350106%5D%2C%5B-93.265929028937506%2C44.984489630988236%5D%2C%5B-93.265821780102343%2C44.984417820807749%5D%2C%5B-93.26572700371571%2C44.984337659950775%5D%2C%5B-93.265645981449495%2C44.984250232487547%5D%2C%5B-93.265579808954016%2C44.984156720754534%5D%2C%5B-93.265529381043223%2C44.984058389363696%5D%2C%5B-93.265495379596842%2C44.983956568099423%5D%2C%5B-93.265478264343017%2C44.983852633934283%5D%2C%5B-93.265478266645687%2C44.983747992407096%5D%2C%5B-93.265495386380721%2C44.983644058615013%5D%2C%5B-93.265529391942579%2C44.983542238076737%5D%2C%5B-93.265579823381287%2C44.983443907725714%5D%2C%5B-93.26564599862688%2C44.983350397290266%5D%2C%5B-93.265727022717172%2C44.983262971312399%5D%2C%5B-93.26582179990352%2C44.983182812048504%5D%2C%5B-93.265929048470909%2C44.983111003482946%5D%2C%5B-93.266047318071855%2C44.98304851667077%5D%2C%5B-93.266175009339591%2C44.982996196607616%5D%2C%5B-93.266310395515248%2C44.982954750804318%5D%2C%5B-93.266451645796863%2C44.982924739720495%5D%2C%5B-93.266596850094416%2C44.982906569186611%5D%2C%5B-93.266744044856253%2C44.982900484916811%5D%2C%5B-93.26689123961809%2C44.982906569186611%5D%2C%5B-93.267036443915643%2C44.982924739720495%5D%2C%5B-93.267177694197258%2C44.982954750804318%5D%2C%5B-93.267313080372915%2C44.982996196607616%5D%2C%5B-93.267440771640651%2C44.98304851667077%5D%2C%5B-93.267559041241597%2C44.983111003482946%5D%2C%5B-93.267666289808986%2C44.983182812048504%5D%2C%5B-93.267761066995334%2C44.983262971312399%5D%2C%5B-93.267842091085626%2C44.983350397290259%5D%2C%5B-93.267908266331219%2C44.983443907725714%5D%2C%5B-93.267958697769927%2C44.983542238076744%5D%2C%5B-93.267992703331785%2C44.983644058615013%5D%2C%5B-93.268009823066819%2C44.983747992407096%5D%2C%5B-93.268009825369489%2C44.983852633934283%5D%2C%5B-93.267992710115664%2C44.983956568099423%5D%2C%5B-93.267958708669298%2C44.984058389363703%5D%2C%5B-93.26790828075849%2C44.984156720754534%5D%2C%5B-93.267842108263011%2C44.984250232487547%5D%2C%5B-93.267761085996796%2C44.984337659950768%5D%2C%5B-93.267666309610163%2C44.984417820807749%5D%2C%5B-93.267559060775%2C44.984489630988229%5D%2C%5B-93.267440789853211%2C44.984552119350106%5D%2C%5B-93.26731309628282%2C44.984604440814181%5D%2C%5B-93.267177706946782%2C44.984645887794123%5D%2C%5B-93.26703645281745%2C44.98467589976687%5D%2C%5B-93.26689124419228%2C44.984694070854012%5D%2C%5B-93.266744044856253%2C44.9847001553116%5D%5D%5D%2C%22spatialReference%22%3A%7B%22wkid%22%3A4326%7D%7D&geometryType=esriGeometryPolygon&";
        APIUtils apiUtils = new APIUtils();
        apiUtils.callAPI(url, params, new APIUtils.View.APICallback() {
            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(String error) {

            }
        });
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
        marker.showAddFromMapDialog(MainActivity.this, vg, point, new Marker.Viewer.AddMarkerCallback() {
            @Override
            public void onMarkerAdded(Marker marker) {
                Log.d("onMarkerAdded", "");
                Toast.makeText(MainActivity.this, getString(R.string.marker_added), Toast.LENGTH_LONG).show();
                addFeaturePoint(point, true);
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
        marker.showAddFromLatLongDialog(MainActivity.this, vg, point, new Marker.Viewer.AddMarkerCallback() {
            @Override
            public void onMarkerAdded(Marker marker) {
                Log.d("onMarkerAdded", "");
                Toast.makeText(MainActivity.this, getString(R.string.marker_added), Toast.LENGTH_LONG).show();
                addFeaturePoint(new Point(marker.lon, marker.lat), true);
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

