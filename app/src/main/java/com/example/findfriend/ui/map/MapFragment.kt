package com.example.findfriend.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.findfriend.MainActivity
import com.example.findfriend.R
import com.example.findfriend.data.User
import com.example.findfriend.databinding.FragmentMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class MapFragment : Fragment() {
    private lateinit var binding: FragmentMapBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null
    private val DEFAULT_ZOOM = 15
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private val MYSETT = "mysettings"
    private var userList: MutableList<User>? = null
    private var image: BitmapDescriptor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = activity?.getSharedPreferences(MYSETT, Context.MODE_PRIVATE)!!
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())
        database = Firebase.database.reference

        lifecycleScope.launch {
            MainActivity.allUsers.collect {
                userList = it
                println("RRRRRR" + userList)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        lifecycleScope.launch {
            delay(3000)

            with(binding.map) {

                onCreate(null)
                binding.map.onResume()
                getMapAsync {
                    MapsInitializer.initialize(requireContext())
                    mMap = it

                    mapReady(mMap)
                    println("DDDDDDDDDDDDDDDDDDDDD")
                }
            }
        }


    }

    @SuppressLint("CommitPrefEdits")
    fun mapReady(map: GoogleMap) {
        map.clear()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        map.isMyLocationEnabled = true

        updateMarkers(map)

        val locationResult = fusedLocationProviderClient.lastLocation
        locationResult.addOnCompleteListener(requireActivity()) { task ->
            if (task.isSuccessful) {
                // Установливаем положение камеры карты на текущее местоположение устройства.
                lastKnownLocation = task.result
                if (lastKnownLocation != null) {
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                lastKnownLocation!!.latitude,
                                lastKnownLocation!!.longitude
                            ), DEFAULT_ZOOM.toFloat()
                        )
                    )
                }
            }
        }
        map.setOnMapClickListener {
            userList!!.forEach { u ->
                if (u.userId == sharedPreferences.getString("userId", "")) {
                    if (u.latitude == 0.0 && u.longitude == 0.0) {
                        MaterialAlertDialogBuilder(requireContext()).apply {
                            setTitle("Ваш дом")
                            setMessage("Данный маркер будет показывать другим пользователям где вы.")
                            setPositiveButton("Хорошо") { d, v ->

                                map.addMarker(
                                    MarkerOptions().position(it).title("Ваш дом").icon(
                                        BitmapFromVector(
                                            requireContext(),
                                            R.drawable.baseline_home_24
                                        )
                                    )
                                )
                                    .apply {
                                        val childUpdates = hashMapOf<String, Any>(
                                            "users/${
                                                sharedPreferences.getString(
                                                    "userId",
                                                    ""
                                                )
                                            }/latitude" to it.latitude,
                                            "users/${
                                                sharedPreferences.getString(
                                                    "userId",
                                                    ""
                                                )
                                            }/longitude" to it.longitude
                                        )
                                        database.updateChildren(childUpdates)
                                    }
                                updateMarkers(map)
                            }
                            setNegativeButton("Отменить") { d, v -> d.dismiss() }
                        }.show()
                    } else {
                        Snackbar.make(
                            binding.root,
                            "Вы уже добавили свой дом.",
                            Snackbar.LENGTH_LONG
                        ).apply {
                            setBackgroundTint(resources.getColor(android.R.color.holo_red_dark))
                            setTextColor(resources.getColor(android.R.color.white))
                        }.show()
                    }
                }
            }
        }
    }

    @OptIn(InternalAPI::class)
    @Throws(IOException::class)
    suspend fun drawableFromUrl(url: String?): Drawable? {
        val x: Bitmap
        val stream = HttpClient(CIO).use {
            it.get(url!!).content.toInputStream()
        }

        x = BitmapFactory.decodeStream(stream)
        return BitmapDrawable(Resources.getSystem(), x)
    }

    private fun BitmapFromVectorUrl(context: Context, vectorDrawable: Drawable): BitmapDescriptor? {
        // below line is use to generate a drawable.

        // below line is use to set bounds to our vector drawable.
        vectorDrawable!!.setBounds(
            0,
            0,
            100,
            100
        )

        // below line is use to create a bitmap for our drawable which we have added.
        val bitmap = Bitmap.createBitmap(
            100,
            100,
            Bitmap.Config.ARGB_8888
        )

        // below line is use to add bitmap in our canvas.
        val canvas = Canvas(bitmap)

        // below line is use to draw our vector drawable in canvas.
        vectorDrawable.draw(canvas)

        // after generating our bitmap we are returning our bitmap.
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun BitmapFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        // below line is use to generate a drawable.
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)

        // below line is use to set bounds to our vector drawable.
        vectorDrawable!!.setBounds(
            0,
            0,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )

        // below line is use to create a bitmap for our drawable which we have added.
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        // below line is use to add bitmap in our canvas.
        val canvas = Canvas(bitmap)

        // below line is use to draw our vector drawable in canvas.
        vectorDrawable.draw(canvas)

        // after generating our bitmap we are returning our bitmap.
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun updateMarkers(map: GoogleMap) {
        map.clear()
        userList!!.forEach {
            val markerOptions =
                MarkerOptions().apply {
                    if (it.userId == sharedPreferences.getString(
                            "userId",
                            ""
                        )
                    ) {// MarkerOptions - параметры маркеров на карте
                        this.title("Ваш дом")
                        this.icon(BitmapFromVector(requireContext(), R.drawable.baseline_home_24))
                    } else {
                        lifecycleScope.launch() {
                            image =
                                BitmapFromVectorUrl(requireContext(), drawableFromUrl(it.image)!!)!!
                            delay(5000)
                            println(image)
                        }
                        this.title(it.name)
                        this.icon(image)
                    }
                    position(LatLng(it.latitude!!, it.longitude!!))
                }
            map.addMarker(markerOptions)
        }
    }
}