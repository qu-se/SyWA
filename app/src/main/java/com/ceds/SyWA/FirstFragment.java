package com.ceds.SyWA;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.widget.Toast;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.List;

import static com.mapbox.mapboxsdk.style.layers.Property.LINE_CAP_ROUND;
import static com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.MapboxNavigationProvider;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver;

public class FirstFragment extends Fragment implements OnMapReadyCallback, PermissionsListener {
    private static final float NAVIGATION_LINE_WIDTH = 6;
    private static final float NAVIGATION_LINE_OPACITY = .8f;
    private static final String DRIVING_ROUTE_POLYLINE_LINE_LAYER_ID = "DRIVING_ROUTE_POLYLINE_LINE_LAYER_ID";
    private static final String DRIVING_ROUTE_POLYLINE_SOURCE_ID = "DRIVING_ROUTE_POLYLINE_SOURCE_ID";

    private MapView mapView;
    private MapboxMap mapboxMap;
    PermissionsManager permissionsManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        Mapbox.getInstance(getActivity(), getString(R.string.mapbox_access_token));

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_first, container, false);
        if (v != null) {
            mapView = v.findViewById(R.id.mapView);
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }

        return v;
    }

    private final RouteProgressObserver MyRouteProgressObserver = new RouteProgressObserver() {
        public void start() {
            if (MapboxNavigationProvider.isCreated()) {
                MapboxNavigationProvider.retrieve().registerRouteProgressObserver(this);
            }
        }

        public void stop() {
            if (MapboxNavigationProvider.isCreated()) {
                MapboxNavigationProvider.retrieve().unregisterRouteProgressObserver(this);
            }
        }

        @Override
        public void onRouteProgressChanged(RouteProgress routeProgress) {
            // My route progress logic
        }
    };

    /**
     * https://docs.mapbox.com/android/navigation/guides/manuever-instructions/
     * voiceInstructionsObserver. Registered in onMapReady()
     */
    private final VoiceInstructionsObserver voiceInstructionsObserver = new VoiceInstructionsObserver() {
        @Override
        public void onNewVoiceInstructions(VoiceInstructions voiceInstructions) {
            // TODO: how create speechPlayer?
            /*
            SpeechPlayer.play(
                    VoiceInstructions.builder()
                            .announcement(voiceInstructions.announcement())
                    .build()
            );
            */
        }
    };

    @Override public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

        this.mapboxMap.setStyle(new Style.Builder().fromUri(Style.LIGHT)
            .withSource(new GeoJsonSource(DRIVING_ROUTE_POLYLINE_SOURCE_ID))
            .withLayer(new LineLayer(DRIVING_ROUTE_POLYLINE_LINE_LAYER_ID,
                       DRIVING_ROUTE_POLYLINE_SOURCE_ID)
            .withProperties(lineWidth(NAVIGATION_LINE_WIDTH),
                            lineOpacity(NAVIGATION_LINE_OPACITY),
                            lineCap(LINE_CAP_ROUND),
                            lineJoin(LINE_JOIN_ROUND),
                            lineColor(Color.parseColor("#d742f4"))
            )), new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                }
        });

        // https://docs.mapbox.com/android/navigation/guides/install/
        // https://docs.mapbox.com/android/navigation/guides/device-location/
        Context applicationContext;
        if (getContext().getApplicationContext() == null) {
            // In shared processes content providers getApplicationContext() can return null.
            applicationContext = getContext();
        } else {
            applicationContext = getContext().getApplicationContext();
        }

        LocationEngine locationEngine = LocationEngineProvider.getBestLocationEngine(applicationContext);
        NavigationOptions navigationOptions = MapboxNavigation
                .defaultNavigationOptionsBuilder(applicationContext, getString(R.string.mapbox_access_token))
                .locationEngine(locationEngine)
                .build();
        if (!MapboxNavigationProvider.isCreated()) {
            // TODO: Error to fix
            MapboxNavigation mapboxNavigation = MapboxNavigationProvider.create(navigationOptions);
            mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(getActivity(), R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                }
            });
        } else {
            Toast.makeText(getActivity(), R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            getActivity().finish();
        }
    }

    /**
     * Contains Callback of search text and button
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        // TODO: mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}