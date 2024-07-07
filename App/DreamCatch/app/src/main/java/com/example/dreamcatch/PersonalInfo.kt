package com.example.dreamcatch

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.dreamcatch.network.ApiResponse
import com.example.dreamcatch.network.Client
import com.example.dreamcatch.network.User
import com.example.dreamcatch.network.personal_info
import com.example.dreamcatch.network.user_password
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PersonalInfo : AppCompatActivity() {
    private lateinit var email: String
    private lateinit var nameText: EditText
    private lateinit var ageText: EditText
    private lateinit var genderText: EditText
    private lateinit var medicalText: EditText
    private lateinit var name: String
    private lateinit var age: String
    private lateinit var gender: String
    private lateinit var medical: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_info)

        val button = findViewById<Button>(R.id.button)

        nameText = findViewById(R.id.editTextText)
        ageText = findViewById(R.id.editTextText2)
        genderText = findViewById(R.id.editTextText3)
        medicalText = findViewById(R.id.editTextText4)

        button.setOnClickListener {
            personalinfo()

        }
    }

    private fun personalinfo() {
        name = nameText.text.toString()
        age = ageText.text.toString()
        gender = genderText.text.toString()
        medical = medicalText.text.toString()
        val User = getToken()
        email = User!!.email

        val personalinfoRequest = personal_info(email, name, gender, age, medical)
        val apiService = Client.getApiService()
        val call = apiService.personal_info(personalinfoRequest)

        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                val apiResponse = response.body()

                if (response.isSuccessful) {
                    if (apiResponse?.success == true) {
                        val user = apiResponse?.user
                        saveToken(user)
                        navigateToAccountActivity()
                    } else {
                        if (apiResponse?.message == "User not found") {
                            showToast("User not found")
                        }
                    }

                } else {

                    showToast("Server Error")

                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                showToast("Network error")
            }
        })
    }

    fun saveToken(user: User?) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val userJson = Gson().toJson(user)

        editor.putString("User", userJson)
        editor.apply()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToAccountActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    fun getToken(): User? {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val userJson = sharedPreferences.getString("user", null)
        return Gson().fromJson(userJson, User::class.java)
    }

}
