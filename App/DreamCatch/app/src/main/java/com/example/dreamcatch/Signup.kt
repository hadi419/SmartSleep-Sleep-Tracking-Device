package com.example.dreamcatch

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.dreamcatch.network.ApiResponse
import com.example.dreamcatch.network.ApiService
import com.example.dreamcatch.network.Client
import com.example.dreamcatch.network.User
import com.example.dreamcatch.network.user_password
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Signup : AppCompatActivity() {

    private lateinit var emailText: EditText
    private lateinit var passwordText: EditText
    private lateinit var email: String
    private lateinit var password: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        Client.init(this)

        emailText = findViewById(R.id.editTextTextEmailAddress)
        passwordText = findViewById(R.id.editTextTextPassword)


        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            email = emailText.text.toString()
            password = passwordText.text.toString()
            if(isValidEmail(email) && isValidPassword(password)){
                SignUp()
            }
            else{
                if(!isValidEmail(email)) {
                    Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show()
                }
                else{
                    Toast.makeText(this, "Invalid password", Toast.LENGTH_SHORT).show()
                }
            }

        }
        val textViewCreateAccount = findViewById<TextView>(R.id.textView)
        textViewCreateAccount.setOnClickListener {
            navigateToLoginActivity()
        }
    }

    private fun SignUp() {
        val signupRequest = user_password(email, password)
        val apiService = Client.getApiService()
        val call = apiService.signup(signupRequest)

        call.enqueue(object : Callback<ApiResponse>{
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                val apiResponse = response.body()

                if (response.isSuccessful){
                    if (apiResponse?.success==true){
                        val user = apiResponse?.user
                        saveToken(user)
                        navigateToPersonalInfoActivity()
                    }
                    else{
                        if(apiResponse?.message == "Account already exists") {
                        showToast("Account already exists")
                        }
                    }

                }
                else{

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

        editor.putString("user", userJson)
        editor.apply()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun isValidPassword(password: String): Boolean {
        val minLength = 5
        if (password.length < minLength) {
            return false
        }

        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }

        if (!hasLetter || !hasDigit) {
            return false
        }

        return true
    }


    fun isValidEmail(email: String): Boolean {
        Log.d("email", email)

        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
    }

    private fun navigateToPersonalInfoActivity() {
        val intent = Intent(this, PersonalInfo::class.java)
        startActivity(intent)
    }
}