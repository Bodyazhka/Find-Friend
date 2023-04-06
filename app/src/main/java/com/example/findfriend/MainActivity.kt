package com.example.findfriend

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.findfriend.data.User
import com.example.findfriend.databinding.ActivityMainBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val MYSETT = "mysettings"


    companion object {
        private val _allUsers: MutableStateFlow<MutableList<User>> = MutableStateFlow(mutableListOf())
        val allUsers = _allUsers.asStateFlow()
        val userId = UUID.randomUUID().toString()
        lateinit var database: DatabaseReference
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        val sharedPref = this.getSharedPreferences(MYSETT, Context.MODE_PRIVATE)


        database = Firebase.database.reference.child("users")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableListOf<User>()
                for (child in snapshot.children) {
                    val user = child.getValue(User::class.java)
                    user?.let {
                        users.add(it)
                    }
                }

                _allUsers.value = users
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
/*
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                println(task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            println(token)
           // Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        })*/
    }
}