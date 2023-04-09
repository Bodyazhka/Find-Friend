package com.example.findfriend.ui.register

import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentResolverCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.findfriend.MainActivity
import com.example.findfriend.MainActivity.Companion.database
import com.example.findfriend.MainActivity.Companion.userId
import com.example.findfriend.R
import com.example.findfriend.data.User
import com.example.findfriend.databinding.FragmentRegisterBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*


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
    private lateinit var storageReference: StorageReference
    private lateinit var storage: FirebaseStorage
    private val MYSETT = "mysettings"
    lateinit var user: User
    private val PICK_IMAGE_REQUEST = 22
    private var filePath: Uri? = null
    private lateinit var bitmap: Bitmap
    private var imageUrl: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = activity?.getSharedPreferences(MYSETT, Context.MODE_PRIVATE)!!
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference
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
                println(imageUrl)
                if (email.isNullOrBlank() || name.isNullOrBlank() || password.isNullOrBlank() || passion.isNullOrBlank() || imageUrl == "") {
                    Snackbar.make(binding.root, "Не все поля заполнены.", Snackbar.LENGTH_LONG)
                        .show()
                    return@setOnClickListener
                }
                writeNewUser(userId, email, name, passion, password, imageUrl, 0.0, 0.0)
                val editor = sharedPreferences.edit()
                editor.putString("userId", userId)
                editor.apply()
                findNavController().navigate(R.id.action_registerFragment_to_mapFragment)

            }
        }
        binding.userImage.setOnClickListener {
            openFileChoose()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if ((requestCode == PICK_IMAGE_REQUEST) && (resultCode == RESULT_OK) && (data != null)
            && (data.data != null)
        ) {
            filePath = data.data!!

            try {

                bitmap = MediaStore.Images.Media.getBitmap(activity?.contentResolver, filePath)

                val uploadTask =
                    storageReference.child(UUID.randomUUID().toString())
                lifecycleScope.launch {
                    uploadTask.apply {
                        putBytes(bitmap.toByteArray()).addOnSuccessListener {
                            downloadUrl.addOnSuccessListener {
                                imageUrl = it.toString()
                            }.addOnFailureListener {
                                println("error")
                            }
                        }
                    }
                }
                //database.child("image").push().setValue(b)

                binding.userImage.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }

    fun Bitmap.toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }


    private fun openFileChoose() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(
                intent,
                "Select Image from here..."
            ),
            PICK_IMAGE_REQUEST
        )
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