package com.nutiteq.advancedmap;

import java.util.Arrays;
import java.util.Vector;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.nutiteq.MapView;
import com.nutiteq.advancedmap.maplisteners.RouteMapEventListener;
import com.nutiteq.components.Components;
import com.nutiteq.components.MapPos;
import com.nutiteq.components.Options;
import com.nutiteq.geometry.Marker;
import com.nutiteq.layers.raster.TMSMapLayer;
import com.nutiteq.log.Log;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.projections.Projection;
import com.nutiteq.services.routing.CloudMadeDirections;
import com.nutiteq.services.routing.CloudMadeToken;
import com.nutiteq.services.routing.Route;
import com.nutiteq.style.MarkerStyle;
import com.nutiteq.style.StyleSet;
import com.nutiteq.ui.DefaultLabel;
import com.nutiteq.utils.UnscaledBitmapLoader;
import com.nutiteq.vectorlayers.GeometryLayer;
import com.nutiteq.vectorlayers.MarkerLayer;

/**
 * Online routing using CloudMade routing http API
 * http://developers.cloudmade.com/projects/show/routing-http-api 
 * 
 * @author jaak
 *
 */
public class CloudMadeRouteActivity extends Activity implements RouteActivity{

	private static final float MARKER_SIZE = 0.3f;
    private MapView mapView;
    protected boolean errorLoading;
    protected boolean graphLoaded;
    protected boolean shortestPathRunning;
    private GeometryLayer routeLayer;
    private Marker startMarker;
    private Marker stopMarker;
    private Bitmap[] routeImages = new Bitmap[5];
    private MarkerLayer markerLayer;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		Log.enableAll();
		Log.setTag("cloudmade");
		
		// 1. Get the MapView from the Layout xml - mandatory
		mapView = (MapView) findViewById(R.id.mapView);
		

		// Optional, but very useful: restore map state during device rotation,
		// it is saved in onRetainNonConfigurationInstance() below
		Components retainObject = (Components) getLastNonConfigurationInstance();
		if (retainObject != null) {
			// just restore configuration, skip other initializations
			mapView.setComponents(retainObject);
	        // add event listener
	        RouteMapEventListener mapListener = new RouteMapEventListener(this);
	        mapView.getOptions().setMapListener(mapListener);

			mapView.startMapping();
			return;
		} else {
			// 2. create and set MapView components - mandatory
		      Components components = new Components();
		      mapView.setComponents(components);
		        // add event listener
		        RouteMapEventListener mapListener = new RouteMapEventListener(this);
		        mapView.getOptions().setMapListener(mapListener);

		      }

		// FIXME: cloudmade here
        TMSMapLayer mapLayer = new TMSMapLayer(new EPSG3857(), 5, 18, 0,
                "http://otile1.mqcdn.com/tiles/1.0.0/osm/", "/", ".png");
        mapView.getLayers().setBaseLayer(mapLayer);
        
        // Location: London
        mapView.setFocusPoint(mapView.getLayers().getBaseLayer().getProjection().fromWgs84(-0.1f, 51.51f));
        mapView.setZoom(14.0f);
        
	      // routing layers
        routeLayer = new GeometryLayer(new EPSG3857());
        mapView.getLayers().addLayer(routeLayer);
          
          
        // create markers for start & end, and a layer for them
        Bitmap olMarker = UnscaledBitmapLoader.decodeResource(getResources(),
                R.drawable.olmarker);
        StyleSet<MarkerStyle> startMarkerStyleSet = new StyleSet<MarkerStyle>(
                MarkerStyle.builder().setBitmap(olMarker).setColor(Color.GREEN)
                        .setSize(MARKER_SIZE).build());
        startMarker = new Marker(new MapPos(0, 0), new DefaultLabel("Start"),
                startMarkerStyleSet, null);

        StyleSet<MarkerStyle> stopMarkerStyleSet = new StyleSet<MarkerStyle>(
                MarkerStyle.builder().setBitmap(olMarker).setColor(Color.RED)
                        .setSize(MARKER_SIZE).build());
        stopMarker = new Marker(new MapPos(0, 0), new DefaultLabel("Stop"),
                stopMarkerStyleSet, null);

        markerLayer = new MarkerLayer(new EPSG3857());
        mapView.getLayers().addLayer(markerLayer);

        // make markers invisible until we need them
//        startMarker.setVisible(false);
//        stopMarker.setVisible(false);
//        
        markerLayer.add(startMarker);
        markerLayer.add(stopMarker);

        // define images for turns
        // source: http://mapicons.nicolasmollet.com/markers/transportation/directions/directions/
        // TODO: use better structure than plain array for this
        routeImages[Route.IMAGE_ROUTE_START] = UnscaledBitmapLoader.decodeResource(getResources(),
                R.drawable.direction_up);
        routeImages[Route.IMAGE_ROUTE_RIGHT] = UnscaledBitmapLoader.decodeResource(getResources(),
                R.drawable.direction_upthenright);
        routeImages[Route.IMAGE_ROUTE_LEFT] = UnscaledBitmapLoader.decodeResource(getResources(),
                R.drawable.direction_upthenleft);
        routeImages[Route.IMAGE_ROUTE_STRAIGHT] = UnscaledBitmapLoader.decodeResource(getResources(),
                R.drawable.direction_up);
        routeImages[Route.IMAGE_ROUTE_END] = UnscaledBitmapLoader.decodeResource(getResources(),
                R.drawable.direction_down);
        
        // rotation - 0 = north-up
        mapView.setRotation(0f);
        // tilt means perspective view. Default is 90 degrees for "normal" 2D map view, minimum allowed is 30 degrees.
        mapView.setTilt(90.0f);

		// Activate some mapview options to make it smoother - optional
        mapView.getOptions().setPreloading(true);
        mapView.getOptions().setSeamlessHorizontalPan(true);
        mapView.getOptions().setTileFading(true);
        mapView.getOptions().setKineticPanning(true);
        mapView.getOptions().setDoubleClickZoomIn(true);
        mapView.getOptions().setDualClickZoomOut(true);
        
		// set sky bitmap - optional, default - white
		mapView.getOptions().setSkyDrawMode(Options.DRAW_BITMAP);
		mapView.getOptions().setSkyOffset(4.86f);
		mapView.getOptions().setSkyBitmap(
				UnscaledBitmapLoader.decodeResource(getResources(),
						R.drawable.sky_small));

        // Map background, visible if no map tiles loaded - optional, default - white
		mapView.getOptions().setBackgroundPlaneDrawMode(Options.DRAW_BITMAP);
		mapView.getOptions().setBackgroundPlaneBitmap(
				UnscaledBitmapLoader.decodeResource(getResources(),
						R.drawable.background_plane));
		mapView.getOptions().setClearColor(Color.WHITE);

		// configure texture caching - optional, suggested
		mapView.getOptions().setTextureMemoryCacheSize(20 * 1024 * 1024);
		mapView.getOptions().setCompressedMemoryCacheSize(8 * 1024 * 1024);

        // define online map persistent caching - optional, suggested. Default - no caching
        mapView.getOptions().setPersistentCachePath(this.getDatabasePath("mapcache").getPath());
		// set persistent raster cache limit to 100MB
		mapView.getOptions().setPersistentCacheSize(100 * 1024 * 1024);

		// 4. Start the map - mandatory
		mapView.startMapping();

		// 5. zoom buttons using Android widgets - optional
		// get the zoomcontrols that was defined in main.xml
		ZoomControls zoomControls = (ZoomControls) findViewById(R.id.zoomcontrols);
		// set zoomcontrols listeners to enable zooming
		zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				mapView.zoomIn();
			}
		});
		zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				mapView.zoomOut();
			}
		});
	}

	
	@Override
    public void showRoute(final double fromLat, final double fromLon,
            final double toLat, final double toLon) {

        Log.debug("calculating path " + fromLat + "," + fromLon + " to "
                + toLat + "," + toLon);

        Projection proj = mapView.getLayers().getBaseLayer().getProjection();
        stopMarker.setMapPos(proj.fromWgs84(toLon, toLat));

        CloudMadeDirections directionsService = new CloudMadeDirections(this, new MapPos(fromLon, fromLat), new MapPos(toLon, toLat), CloudMadeDirections.ROUTE_TYPE_CAR, CloudMadeDirections.ROUTE_TYPE_MODIFIER_FASTEST, "e12f720d5f2b5499946d2e975088dc89", proj);
        directionsService.route();
    }

    public MapView getMapView() {
        return mapView;
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        mapView.stopMapping();
    }


    @Override
    public void setStartmarker(MapPos startPos) {
        routeLayer.clear();
        markerLayer.clear();

        //stopMarker.setVisible(false);
        markerLayer.add(startMarker);
        startMarker.setMapPos(startPos);
        startMarker.setVisible(true);
    }

    @Override
    public void routeResult(Route route) {
        
        if(route.getRouteResult() != Route.ROUTE_RESULT_OK){
            Toast.makeText(this, "Route error", Toast.LENGTH_LONG).show();
            return;
        }
        
        routeLayer.clear();
        routeLayer.add(route.getRouteLine());
        Log.debug("route line: "+route.getRouteLine().toString());
        markerLayer.addAll(route.getRoutePointMarkers(routeImages, MARKER_SIZE));
        mapView.requestRender();
        Toast.makeText(this, "Route "+route.getRouteSummary(), Toast.LENGTH_LONG).show();
    }
}

