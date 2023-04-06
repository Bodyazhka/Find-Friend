package com.example.findfriend.data

import android.media.Image
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    val userId: String? = null,
    val email: String? = null,
    val name: String? = null,
    val passion: String? = null,
    val password: String? = null,
    val image: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
){}