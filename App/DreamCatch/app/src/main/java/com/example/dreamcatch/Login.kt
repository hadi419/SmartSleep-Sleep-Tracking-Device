package com.example.dreamcatch

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.dreamcatch.network.ApiResponse
import com.example.dreamcatch.network.Client
import com.example.dreamcatch.network.User
import com.example.dreamcatch.network.user_password
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Login : AppCompatActivity() {

    private lateinit var emailText: EditText
    private lateinit var passwordText: EditText
    private lateinit var email: String
    private lateinit var password: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailText = findViewById(R.id.editTextTextEmailAddress)
        passwordText = findViewById(R.id.editTextTextPassword)
        Log.d("connection", emailText.toString())


        Client.init(this)

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            LogIn()
        }
        val textViewCreateAccount = findViewById<TextView>(R.id.textView)
        textViewCreateAccount.setOnClickListener {
            navigateToSignupActivity()

        }
    }

    private fun LogIn() {
        email = emailText.text.toString()
        password = passwordText.text.toString()
        Log.d("connection", email+password)
        val loginRequest = user_password(email, password)
        val apiService = Client.getApiService()
        val call = apiService.login(loginRequest)
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                val apiResponse = response.body()

                if (response.isSuccessful){
                    if(apiResponse?.success==true){
                        val user = apiResponse?.user
                        Log.d("RetrofitDebug", "API call successful")

                        saveToken(user)
                        navigateToAccountActivity()
                    }else{
                        if(apiResponse?.message == "Account does not exist") {
                            showToast("Account does not exist")
                        }
                        else if (apiResponse?.message == "Wrong password"){
                            showToast("Wrong password")
                        }
                        else {
                            showToast("Server error")
                        }
                    }

                }
                else{
                    Log.e("RetrofitDebug", "API call failed: ${response.code()}")

                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                showToast("Network error")
                Log.e("RetrofitDebug", "API call failed: ${t.message}")

            }
        })
    }

    fun saveToken(user: User?) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val userJson = Gson().toJson(user)

        editor.putString("user", userJson)
        editor.apply()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToAccountActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToSignupActivity() {
        val intent = Intent(this, Signup::class.java)
        startActivity(intent)
    }
}