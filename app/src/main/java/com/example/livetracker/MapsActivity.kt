package com.example.livetracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    private lateinit var mMap: GoogleMap
    private var markerPoints = arrayListOf<LatLng>()
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var distance: TextView? = null

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 100
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        distance = findViewById(R.id.btLocation)

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        getMobileLocation()
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        mMap.apply {
            if (ActivityCompat.checkSelfPermission(
                    this@MapsActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this@MapsActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            uiSettings.apply {
                isCompassEnabled = true
                isZoomControlsEnabled = true
            }

            latitude?.let { latitude ->
                longitude?.let { longitude ->
                    addMarker(
                        MarkerOptions().position(LatLng(latitude, longitude)).icon(
                            bitmapDescriptorFromVector(this@MapsActivity)
                        )
                    )
                    markerPoints.add(LatLng(latitude, longitude))
                    animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(latitude, longitude),
                            13f
                        )
                    )
                }
            }



            setOnMapClickListener {
                distance?.visibility = View.GONE
                markerPoints.add(it)
                Log.d("TAG", "Location Data ->$latitude $longitude -> $it")
                Log.d("TAG", "onMapReady ->$it  $markerPoints")
                val options = MarkerOptions()
                options.position(it)


                if (markerPoints.size == 1) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                } else if (markerPoints.size == 2) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                }

                // Add new marker to the Google Map Android API V2
                // Add new marker to the Google Map Android API V2
                mMap.addMarker(options)

                // Checks, whether start and end locations are captured
                var origin: LatLng? = null
                var dest: LatLng? = null
                var url: String? = null
                if (markerPoints.size >= 2) {
                    origin = markerPoints.first()
                    dest = markerPoints.last()
                    distance?.visibility = View.VISIBLE
                    distance?.text =
                        calculationByDistance(markerPoints.first(), markerPoints.last())
                    url = getDirectionsUrl(origin, dest)

                }

                url?.let { urlPath ->
                    downloadUrl(urlPath)
                }
                if (markerPoints.size >= 2) {
                    addPolyline(
                        PolylineOptions().add(markerPoints.first()).add(markerPoints.last())
                    )
                }

                Log.d("TAG", "direction Url : $url")
            }
        }
    }

    private fun calculationByDistance(StartP: LatLng, EndP: LatLng): String {
        val radius = 6371 // radius of earth in Km
        val lat1 = StartP.latitude
        val lat2 = EndP.latitude
        val lon1 = StartP.longitude
        val lon2 = EndP.longitude
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = (sin(dLat / 2) * sin(dLat / 2)
                + (cos(Math.toRadians(lat1))
                * cos(Math.toRadians(lat2)) * sin(dLon / 2)
                * sin(dLon / 2)))
        val c = 2 * asin(sqrt(a))
        val valueResult = radius * c
        val km = valueResult / 1
        val newFormat = DecimalFormat("####")
        val kmInDec: Int = Integer.valueOf(newFormat.format(km))
        val meter = valueResult % 1000
        val meterInDec: Int = Integer.valueOf(newFormat.format(meter))
        Log.i(
            "Radius Value", "" + valueResult + "   KM  " + kmInDec
                    + " Meter   " + meterInDec
        )
        return "   KM :$kmInDec Meter : $meterInDec"
    }


    private fun bitmapDescriptorFromVector(
        context: Context,
    ): BitmapDescriptor? {
        val vectorDrawable = ContextCompat.getDrawable(context, R.drawable.icon_man)

        val bitmap = Bitmap.createBitmap(
            vectorDrawable!!.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun downloadUrl(strUrl: String): Boolean {

        var data = ""
        var iStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null

        return try {
            val url = URL(strUrl)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.connect()
            iStream = urlConnection.inputStream
            val br = BufferedReader(InputStreamReader(iStream))
            val sb = StringBuffer()
            var line: String? = ""
            while (br.readLine().also { line = it } != null) {
                sb.append(line)
            }
            data = sb.toString()
            br.close()
            true
        } catch (e: Exception) {
            false
        } finally {
            iStream?.close()
            urlConnection?.disconnect()
        }

    }


    private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String {

        // Origin of route
        val str_origin = "origin=" + origin.latitude + "," + origin.longitude

        // Destination of route
        val str_dest = "destination=" + dest.latitude + "," + dest.longitude

        // Sensor enabled
        val sensor = "sensor=false"
        val mode = "mode=driving"

        // Building the parameters to the web service
        val parameters = "$str_origin&$str_dest&$sensor&$mode"

        // Output format
        val output = "json"

        // Building the url to the web service
        return "https://maps.googleapis.com/maps/api/directions/$output?$parameters"
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Toast.makeText(
                    this,
                    "Permissions granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun getMobileLocation() {
        Log.v("Location", "getMobileLocation is called")
        try {
            var location: Location? = null
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

            // getting GPS status
            val isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER)
            Log.v("Location", "isGPSEnabled= $isGPSEnabled")

            // getting network status
            val isNetworkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            Log.v("Location", "isNetworkEnabled =$isNetworkEnabled")

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {

                if (isNetworkEnabled) {

                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        0L,
                        0F, this
                    )
                    Log.v("Network", "Network")
                    location = locationManager
                        .getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (location != null) {
                        latitude = location.latitude
                        longitude = location.longitude
                    }
                    Log.v("Location", "Network Enabled : $latitude $longitude")
                }


                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        0L,
                        0f, this
                    )

                    location = locationManager
                        .getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (location != null) {
                        latitude = location.latitude
                        longitude = location.longitude
                    }
                    Log.v("Location", "GPS Enabled : $latitude $longitude")
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }


    }

    override fun onLocationChanged(location: Location) {}


}
