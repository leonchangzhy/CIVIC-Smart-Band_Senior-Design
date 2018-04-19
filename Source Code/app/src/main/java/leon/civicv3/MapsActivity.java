package leon.civicv3;

import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    DatabaseHelperGPS GPSDb;

    String titlegps;
    Double Latgps;
    Double Longgps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        GPSDb = new DatabaseHelperGPS(this);

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;




        // Add a marker in Sydney and move the camera
        LatLng westlafayette = new LatLng(40.4242, -86.9011);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(westlafayette,10.2f));



        Cursor resgps = GPSDb.getAllGPS();
        if(resgps.getCount() == 0) {

            return;
        }

        while (resgps.moveToNext()) {
            titlegps = resgps.getString(3);
            Latgps = resgps.getDouble(1);
            Longgps = resgps.getDouble(2);
            AddMarker(Latgps,Longgps,titlegps);
        }

    }

    public void AddMarker(Double Lat,Double Long,String title){

        mMap.addMarker(new MarkerOptions().position(new LatLng(Lat,Long)).title(title));
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.commonmenus,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.mnuSync){
            startActivity(new Intent(this,BleActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}
