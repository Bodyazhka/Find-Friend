package com.example.findfriend.ui.login

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.findfriend.MainActivity
import com.example.findfriend.R
import com.example.findfriend.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private lateinit var database: DatabaseReference

    //    SharedPreferences — постоянное хранилище на платформе Android, используемое приложениями для хранения своих настроек
    private lateinit var sharedPreferences: SharedPreferences
    private val MYSETT = "mysettings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = activity?.getSharedPreferences(MYSETT, Context.MODE_PRIVATE)!!
        if (sharedPreferences.contains("userId")) {
            findNavController().navigate(R.id.action_loginFragment_to_mapFragment)
        }
        database = Firebase.database.reference
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("CommitPrefEdits")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            loginButton.setOnClickListener {
                val email = emailField.editText?.text.toString()
                val password = passwordField.editText?.text.toString()

                if (email.isNullOrBlank() || password.isNullOrBlank()) {
                    Snackbar.make(binding.root, "Не все поля заполнены.", Snackbar.LENGTH_LONG)
                        .show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    MainActivity.allUsers.collect { users ->
                        users.forEach {
                            if (it.email == email && it.password == password) {
                                if (!sharedPreferences.contains("userId")) {
                                    val editor = sharedPreferences.edit()
                                    editor.putString("userId", it.userId)
                                    editor.apply()
                                }
                                findNavController().navigate(R.id.action_loginFragment_to_mapFragment)
                                return@collect
                            }
                        }
                    }
                }
            }

            registerButton.setOnClickListener {
                findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
            }
        }
    }
}