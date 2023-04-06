package com.example.findfriend.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.findfriend.MainActivity
import com.example.findfriend.R
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import kotlinx.coroutines.launch


class MapFragment : Fragment() {
    private lateinit var binding: FragmentMapBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null
    private val DEFAULT_ZOOM = 15
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private val MYSETT = "mysettings"
    private var homeFlag = 0
    private var mImageUri: Uri? = null
    private var mStorageRef: StorageReference? = null
    private var mUploadTask: StorageTask<*>? = null
    private var mDatabaseRef: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = activity?.getSharedPreferences(MYSETT, Context.MODE_PRIVATE)!!
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())
        setHasOptionsMenu(true)
        database = Firebase.database.reference

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

        with(binding.map) {
            onCreate(null)
            binding.map.onResume()
            getMapAsync {
                MapsInitializer.initialize(requireContext())
                mMap = it
                mapReady()
            }
        }

        binding.chatButton.setOnClickListener {
            findNavController().navigate(R.id.action_mapFragment_to_chatFragment)
        }
    }

    @SuppressLint("CommitPrefEdits")
    fun mapReady() {
        mMap.clear()

        with(mMap) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
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

            isMyLocationEnabled = true

            updateMarkers(mMap)

            val locationResult = fusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Установливаем положение камеры карты на текущее местоположение устройства.
                    lastKnownLocation = task.result
                    if (lastKnownLocation != null) {
                        mMap.moveCamera(
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

            setOnMapClickListener {
                var editor = sharedPreferences.edit()
                lifecycleScope.launch {
                    MainActivity.allUsers.collect { list ->
                        list.forEach { u ->
                            if (u.latitude == 0.0 && u.longitude == 0.0) {
                                MaterialAlertDialogBuilder(requireContext()).apply {
                                    setTitle("Ваш дом")
                                    setMessage("Данный маркер будет показывать другим пользователям где вы.")
                                    setPositiveButton("Хорошо") { d, v ->
                                        homeFlag++
                                        /*editor.putString("homeFlag", homeFlag.toString())
                                        editor.apply()*/
                                        mMap.addMarker(
                                            MarkerOptions().position(it).title("Ваш дом").icon(
                                                BitmapFromVector(
                                                    requireContext(),
                                                    R.drawable.baseline_key_24
                                                )
                                            )
                                        ).apply {
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
                                            /*lifecycleScope.launch{
                                                MainActivity.allUsers.collect{list ->
                                                    val user = list.find { u -> u.userId == sharedPreferences.getString("userId", "")}
                                                    if (user != null){
                                                        database.child("users").child(user.userId!!).setValue(it)
                                                    }
                                                }
                                            }*/
                                        }

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
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.simple_menu, menu)
    }

    @SuppressLint("InflateParams")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val rootView =
                    LayoutInflater.from(requireContext())
                        .inflate(
                            R.layout.settings_dialog,
                            null,
                            false
                        )
                MaterialAlertDialogBuilder(requireContext()).apply {
                    setView(rootView)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

    fun updateMarkers(map: GoogleMap) {
        map.clear()
        lifecycleScope.launch {
            MainActivity.allUsers.collect {
                //val user = it.find { u -> u.email == email && u.password == password }
                it.forEach {
                    val markerOptions =
                        MarkerOptions().apply { // MarkerOptions - параметры маркеров на карте
                            title(it.name)
                            position(LatLng(it.latitude!!, it.longitude!!))
                            if (it.userId == sharedPreferences.getString("userId", "")) {
                                icon(
                                    BitmapFromVector(
                                        requireContext(),
                                        R.drawable.baseline_home_24
                                    )
                                )
                            } //else icon(BitmapFromVector(requireContext(), ))
                            //icon(BitmapFromVector(requireContext(), ))
                        }

                    map.addMarker(markerOptions)
                }
            }
        }
    }

    private fun openFileChoose() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, 1)
    }

    /* private fun getFileExtension(uri: Uri): String? {
         val cR = contentRoslver
         val mime = MimeTypeMap.getSingleton()
         return mime.getExtensionFromMimeType(cR.getType(uri))
     }

     private fun uploadFile() {
         if (mImageUri != null) {

             //var id = Intent().getStringExtra("RouteId").toString()

             val fileReference = mStorageRef!!.child(
                 System.currentTimeMillis()
                     .toString() + "." + getFileExtension(mImageUri!!)
             )
             mUploadTask = fileReference.putFile(mImageUri!!)
                 .addOnSuccessListener { taskSnapshot ->
                     val handler = Handler()

                     println("Ваш путь был успешно загружен")
                     *//*val upload = Route(
                        name = name_route!!.text.toString(),
                        imageUrl = mImageUri.toString(),
                        description =  description_route!!.text.toString(),
                        uuid = id
                    )*//*

                    val childUpdates = hashMapOf<String, Any>(
                        "users/${
                            sharedPreferences.getString(
                                "userId",
                                ""
                            )
                        }/image" to mImageUri.toString()
                    )
                    database.updateChildren(childUpdates)
                    *//*val uploadId = mDatabaseRef!!.push().key
                    mDatabaseRef!!.child((uploadId)!!).setValue(upload)*//*
                    val serviceIntent = Intent(requireContext(), MapFragment::class.java)

                    startActivity(serviceIntent)
                }
                .addOnFailureListener { e ->
                }
                .addOnProgressListener { taskSnapshot ->
                }
        } else {
            println("Заполните все параметры")
        }
    }*/
}