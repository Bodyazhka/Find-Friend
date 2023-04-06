package com.example.findfriend.ui.register

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.findfriend.MainActivity.Companion.database
import com.example.findfriend.MainActivity.Companion.userId
import com.example.findfriend.R
import com.example.findfriend.data.User
import com.example.findfriend.databinding.FragmentRegisterBinding
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.UUID

class RegisterFragment : Fragment() {
    private lateinit var binding: FragmentRegisterBinding
    private val passions =
        listOf<String>(
            "Спорт",
            "Игры",
            "Книги",
            "Фильмы",
            "Искусство",
            "Юмор"
        )
    private lateinit var passionAdapter: ArrayAdapter<String>
    private lateinit var sharedPreferences: SharedPreferences
    private val MYSETT = "mysettings"
    lateinit var user: User
    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = activity?.getSharedPreferences(MYSETT, Context.MODE_PRIVATE)!!
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)

        passionAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, passions)

        (binding.passionField.editText as? AutoCompleteTextView)?.setAdapter(passionAdapter)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        database = Firebase.database.reference
        //database.child("users").child("location").push().setValue("1111111")

        binding.run {
            registerButton.setOnClickListener {
                val email = emailField.editText?.text.toString()
                val name = nameField.editText?.text.toString()
                val password = passwordField.editText?.text.toString()
                val passion = passionField.editText?.text.toString()
                // задать изначальную аватарку
                writeNewUser(userId, email, name, passion, password, "", 0.0, 0.0)
                val editor = sharedPreferences.edit()
                editor.putString("userId", userId)
                editor.apply()
                findNavController().navigate(R.id.action_registerFragment_to_mapFragment)
            }
        }
    }

    private fun writeNewUser(
        userId: String,
        email: String,
        name: String,
        passion: String,
        password: String,
        image: String,
        latitude: Double,
        longitude: Double
    ) {
        user = User(userId, email, name, passion, password, image, latitude, longitude)
        database.child("users").child(userId).setValue(user)
    }
}