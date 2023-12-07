package mhha.sample.sharemylocation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.RoundedCorner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
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
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.kakao.sdk.common.util.Utility
import mhha.sample.sharemylocation.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {//class MainActivity : AppCompatActivity()

    private lateinit var binding: ActivityMapBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val markerMap = hashMapOf<String, Marker>()
    private var trackingUserId: String = ""

    private val locationCallback = object: LocationCallback(){
        override fun onLocationResult(p0: LocationResult) {
            //새로 요청된 위치 정보
            for(location in p0.locations){
                Log.i("Location", "latitude : ${location.latitude} / longitude : ${location.longitude}")

                //파이어 베이스의 위치값 업로드
                val uid = Firebase.auth.currentUser?.uid.orEmpty()

                val locationMap = mutableMapOf<String, Any>()
                locationMap["latitude"] = location.latitude
                locationMap["longitude"] = location.longitude
                Firebase.database.reference.child("User").child(uid).updateChildren(locationMap)


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
        setupEmojiAnimationView()
        setupCurrentLocationView()
        setupFirebaseDatabase()

//        startActivity(Intent(this, LoginActivity::class.java))

    }//override fun onCreate(savedInstanceState: Bundle?)

    private fun setupCurrentLocationView(){
        binding.currentLocationButton.setOnClickListener {
            trackingUserId = ""
            moveLastLocation()
        }
    }//private fun setupCurrentLocationView()

    override fun onResume() {
        super.onResume()
        getCurrentLocation()
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun setupEmojiAnimationView(){
        //이모지 리액션 보내기
        binding.emojiLottieAnimationView.setOnClickListener {
            if(trackingUserId != ""){
                val lastEmoji = mutableMapOf<String, Any>()
                lastEmoji["type"] = "star"
                lastEmoji["lastModifier"] = System.currentTimeMillis()
                Firebase.database.reference.child("Emoji").child(trackingUserId).updateChildren(lastEmoji)
            }
            binding.emojiLottieAnimationView.playAnimation()
            binding.dummyLottieAnimationView.animate()
                .scaleX(3f)
                .scaleY(3f)
                .alpha(0f)
                .withStartAction {
                    binding.dummyLottieAnimationView.scaleX = 1f
                    binding.dummyLottieAnimationView.scaleY = 1f
                    binding.dummyLottieAnimationView.alpha = 1f
                }.withEndAction {
                    binding.dummyLottieAnimationView.scaleX = 1f
                    binding.dummyLottieAnimationView.scaleY = 1f
                    binding.dummyLottieAnimationView.alpha = 1f
                }.start()
        }
        binding.centerLottieAnimationView.speed = 3f
        binding.emojiLottieAnimationView.speed = 3f
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0
//        val sydney = LatLng(37.56701355670135, 126.9783740)
//        googleMap.addMarker(
//            MarkerOptions()
//            .position(sydney)
//            .title("Marker in Sydney"))
//        googleMap.moveCamera(CameraUpdateFactory.zoomTo(18.0f))
//        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        googleMap.setMaxZoomPreference(20.0f)
        googleMap.setMinZoomPreference(10.0f)

        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnMapClickListener {
            trackingUserId = ""
        }

    }//override fun onMapReady(p0: GoogleMap)

    private fun createLocationRequest(): LocationRequest {
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
        moveLastLocation()
    }//private fun getCurrentLocation()

    private fun moveLastLocation(){
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
        fusedLocationClient.lastLocation.addOnSuccessListener {
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16.0f)
            )
        }
    }

    private fun getRequestLocationPermission(){
        locationPermission.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )//locationPermission.launch
    }//private fun getRequestLocationPermission()

    private fun setupFirebaseDatabase(){
        Firebase.database.reference.child("User")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val user = snapshot.getValue(MyUser::class.java) ?: return
                    val uid = user.uid ?: return

                    if( markerMap[uid] == null) {
                        markerMap[uid]= makeNewMarker(user,uid) ?: return
                    }else{
                        markerMap[uid]?.position = LatLng(user.latitude ?: 0.0 ,user.longitude ?: 0.0)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val user = snapshot.getValue(MyUser::class.java) ?: return
                    val uid = user.uid ?: return
                    if( markerMap[uid] == null) {
                        markerMap[uid]= makeNewMarker(user,uid) ?: return
                    }else{
                        markerMap[uid]?.position = LatLng(user.latitude ?: 0.0 ,user.longitude ?: 0.0)
                    }
                    if(uid == trackingUserId){
                        googleMap.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(LatLng(user.latitude ?: 0.0 ,user.longitude ?: 0.0))
                                    .zoom(18.0f)
                                    .build()
                            )
                        )
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        //다른 사람이 나를 클릭 시 이모지 반응
        Firebase.database.reference.child("Emoji").child(Firebase.auth.currentUser?.uid ?: "")
            .addChildEventListener(object : ChildEventListener{
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    binding.centerLottieAnimationView.playAnimation()
                    binding.centerLottieAnimationView.animate()
                        .scaleX(7f)
                        .scaleY(7f)
                        .alpha(0.3f)
                        .setDuration(binding.centerLottieAnimationView.duration / 3)
                        .withEndAction {
                            binding.centerLottieAnimationView.scaleX = 0f
                            binding.centerLottieAnimationView.scaleY = 0f
                            binding.centerLottieAnimationView.alpha = 1f
                        }.start()
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }//private fun setupFirebaseDatabase()

    private fun makeNewMarker(user : MyUser, uid : String): Marker? {
        val marker = googleMap.addMarker(
            MarkerOptions().position(LatLng(user.latitude ?: 0.0 ,user.longitude ?: 0.0)).title(user.name.orEmpty())
        ) ?: return null

        marker.tag = uid

        //마커에 이미지 넣기
        Glide.with(this).asBitmap()
            .load(user.profilePhoto)
            .transform(RoundedCorners(60))
            .override(200)
            .listener(object : RequestListener<Bitmap>{
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                    return false
                }

                override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    resource?.let {
                        runOnUiThread {
                            marker.setIcon(
                                BitmapDescriptorFactory.fromBitmap(
                                    resource
                                )
                            )//marker.setIcon
                        }
                    }
                    return true
                }
            }).submit()

        return marker
    }//private fun makeNuwMarker(user : MyUser, uid : String): Marker?

    override fun onMarkerClick(p0: Marker): Boolean {
        Log.d("onMarkerClick", "Click")

        trackingUserId = p0.tag as? String ?: ""

        val bottomSheetbehavior = BottomSheetBehavior.from(binding.emojiBottomSheetLayout)
        bottomSheetbehavior.state = BottomSheetBehavior.STATE_EXPANDED


        return false
    }

}
