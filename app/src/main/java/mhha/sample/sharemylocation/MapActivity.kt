package mhha.sample.sharemylocation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.kakao.sdk.common.util.Utility
import mhha.sample.sharemylocation.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationCallback = object: LocationCallback(){
        override fun onLocationResult(p0: LocationResult) {
            //새로 요청된 위치 정보
            for(location in p0.locations){
                Log.i("Location", "latitude : ${location.latitude} / longitude : ${location.longitude}")
                location.latitude
                location.longitude
            }//for(location in p0.locations)
        }//override fun onLocationResult(p0: LocationResult)
    }//private val locationCallback = object: LocationCallback()

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){ permissions ->
        when{
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION,false) -> {
                //Fine Location 권한이 있음.
                getCurrentLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION,false) -> {
                //Coarse Location 권한이 있음.
                getCurrentLocation()
            }
            else ->{
                Toast.makeText(this,"권한 설정 필수", Toast.LENGTH_SHORT).show()
            }
        }//when
    }//private val locationPermission = registerForActivityResult

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var keyHash = Utility.getKeyHash(this)
        Log.i("kakao_keHash", "${keyHash}")

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getRequestLocationPermission()

//        startActivity(Intent(this, LoginActivity::class.java))

    }//override fun onCreate(savedInstanceState: Bundle?)

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0
        val sydney = LatLng(37.56701355670135, 126.9783740)
        googleMap.addMarker(
            MarkerOptions()
            .position(sydney)
            .title("Marker in Sydney"))
        googleMap.moveCamera(CameraUpdateFactory.zoomTo(18.0f))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

    }//override fun onMapReady(p0: GoogleMap)

    fun createLocationRequest(): LocationRequest {
        return LocationRequest
            .Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .build()
    }
    private fun getCurrentLocation(){
        val locationinfo = createLocationRequest()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            getRequestLocationPermission()
            return
        }
        fusedLocationClient.requestLocationUpdates(locationinfo,locationCallback, Looper.getMainLooper())
    }//private fun getCurrentLocation()

    private fun getRequestLocationPermission(){
        locationPermission.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )//locationPermission.launch
    }//private fun getRequestLocationPermission()

}//class MainActivity : AppCompatActivity()
