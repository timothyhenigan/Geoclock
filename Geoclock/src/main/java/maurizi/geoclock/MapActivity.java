package maurizi.geoclock;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;

import java.util.Collection;


public class MapActivity extends ActionBarActivity
		implements NavigationDrawerFragment.NavigationDrawerCallbacks, GeoAlarmFragment.Listener {

	private static final Gson gson = new Gson();
	private static final int DEFAULT_ZOOM_LEVEL = 14;

	private SupportMapFragment mapFragment = null;
	/**
	 * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
	 */
	private NavigationDrawerFragment navigationDrawerFragment;
	private GoogleMap map = null;
	private GoogleApiClient apiClient = null;
	private BiMap<GeoAlarm, Marker> markers = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mapFragment = new SupportMapFragment();
		navigationDrawerFragment = new NavigationDrawerFragment();
		getSupportFragmentManager().beginTransaction()
		                           .replace(R.id.map, mapFragment)
		                           .replace(R.id.navigation_drawer, navigationDrawerFragment)
		                           .commit();

		final GoogleApiClientHandler handler = new GoogleApiClientHandler();
		apiClient = new GoogleApiClient.Builder(this)
				            .addApi(LocationServices.API)
				            .addConnectionCallbacks(handler)
				            .addOnConnectionFailedListener(handler)
				            .build();
		apiClient.connect();
		markers = HashBiMap.create();
	}

	@Override
	public void onResume() {
		super.onResume();
		map = mapFragment.getMap();
		if (map != null) {
			map.setMyLocationEnabled(true);
			map.setOnMapClickListener(this::showPopup);
			map.setOnMarkerClickListener(this::showPopup);
		}

		// Set up the drawer.
		final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		navigationDrawerFragment.setUp(R.id.navigation_drawer, drawerLayout);

		redrawGeoAlarms();
	}

	@Override
	public void onNavigationDrawerItemSelected(GeoAlarm alarm) {
		if (map != null) {
			map.animateCamera(CameraUpdateFactory.newLatLng(alarm.location));
			markers.get(alarm).showInfoWindow();
		}
	}

	void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			actionBar.setDisplayShowTitleEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!navigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			getMenuInflater().inflate(R.menu.map, menu);
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		return id == R.id.action_settings || super.onOptionsItemSelected(item);
	}

	@Override
	public void onAddGeoAlarmFragmentClose(DialogFragment dialog) {
		redrawGeoAlarms();
	}

	private void redrawGeoAlarms() {
		final Collection<GeoAlarm> alarms = GeoAlarm.getGeoAlarms(this);
		navigationDrawerFragment.setGeoAlarms(alarms);
		if (map != null) {
			map.clear();
			for (final GeoAlarm alarm : alarms) {
				markers.put(alarm, map.addMarker(alarm.getMarkerOptions()));
				map.addCircle(alarm.getCircleOptions());
			}
		}
	}

	private class GoogleApiClientHandler extends ToastLocationClientHandler {
		public GoogleApiClientHandler() {
			super(MapActivity.this);
		}

		@Override
		public void onConnected(Bundle bundle) {
			super.onConnected(bundle);
			if (map != null) {
				final Location loc = LocationServices.FusedLocationApi.getLastLocation(apiClient);
				if (loc != null) {
					map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()),
					                                                 DEFAULT_ZOOM_LEVEL));
				}
			}
		}
	}

	boolean showPopup(LatLng latLng) {
		Bundle args = new Bundle();
		args.putParcelable(GeoAlarmFragment.INITIAL_LATLNG, latLng);
		args.putFloat(GeoAlarmFragment.INITIAL_ZOOM, map.getCameraPosition().zoom);

		return showPopup(args);
	}

	boolean showPopup(Marker marker) {
		final GeoAlarm alarm = markers.inverse().get(marker);
		Bundle args = new Bundle();
		args.putFloat(GeoAlarmFragment.INITIAL_ZOOM, map.getCameraPosition().zoom);
		args.putString(GeoAlarmFragment.EXISTING_ALARM, gson.toJson(alarm, GeoAlarm.class));

		return showPopup(args);
	}

	boolean showPopup(Bundle args) {
		GeoAlarmFragment popup = new GeoAlarmFragment();
		popup.setArguments(args);
		popup.show(getSupportFragmentManager(), "AddGeoAlarmFragment");

		return true;
	}
}
