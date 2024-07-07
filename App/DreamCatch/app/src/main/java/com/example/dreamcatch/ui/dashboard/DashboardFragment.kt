package com.example.dreamcatch.ui.dashboard

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.dreamcatch.AlarmReceiver
import com.example.dreamcatch.StopAlarmReceiver
import com.example.dreamcatch.databinding.FragmentDashboardBinding
import com.example.dreamcatch.network.AnalyticsResponse
import com.example.dreamcatch.network.ApiResponse
import com.example.dreamcatch.network.Client
import com.example.dreamcatch.network.User
import com.example.dreamcatch.network.analytics
import com.example.dreamcatch.network.controlsignal
import com.example.dreamcatch.network.email
import com.example.dreamcatch.network.scan
import com.example.dreamcatch.network.user_password
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.zxing.integration.android.IntentIntegrator
import org.w3c.dom.Text
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import okhttp3.ResponseBody

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    private val binding get() = _binding!!

    private lateinit var earliesttime: String
    private lateinit var atesttime: String

    private var asleep = false
    private lateinit var email: String

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)


        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val button = binding.button
        val switch1 = binding.switch1
        val earliestTimeText = binding.editTextText4
        val latestTimeText = binding.editTextText5
        val buttonpair = binding.buttonpair
        buttonpair.setOnClickListener {
            // Start QR code scanning
            startQRCodeScanning()
        }


        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(requireContext(), AlarmReceiver::class.java)
        val requestCode = 123

        val stopIntent = Intent(requireContext(), StopAlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )



        val stopButton = binding.stopbutton

        switch1.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val parts = earliestTimeText.text.toString().split(".")
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()
                Log.d("wake up", "Hour: $hour, Minute: $minute")
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (Calendar.getInstance().after(calendar)) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                switch1.text = "Turn off"
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                stopButton.visibility = View.VISIBLE
            } else {
                switch1.text = "Turn on"
                alarmManager.cancel(pendingIntent)
                stopButton.visibility = View.GONE
            }
        }

        stopButton.setOnClickListener {
            val stopIntent = Intent("STOP_ALARM_AUDIO")
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(stopIntent)
            stopButton.visibility = View.GONE
        }


        button.setOnClickListener {
            control()
            if(asleep){
                button.text = "SLEEP"
                asleep = false
            }
            else{
                button.text = "WAKE UP"
                asleep = true
            }
        }

        return root
    }



    private fun startQRCodeScanning() {
        val integrator = IntentIntegrator.forSupportFragment(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan a QR Code")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val User = getToken()
                email = User!!.email
                val deviceid = result.contents
                val device = scan(email, deviceid)
                val apiService = Client.getApiService()
                val call = apiService.device(device)
                call.enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        val apiResponse = response.body()
                        if (response.isSuccessful){
                            Log.d("RetrofitDebug", "QR code string sent")

                        }
                        else{
                            showToast("Server error")
                        }

                    }
                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        showToast("Network error")
                        Log.e("RetrofitDebug", "API call failed: ${t.message}")

                    }
                })
                Toast.makeText(requireContext(), "Scanned: $deviceid", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Scan failed", Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }


    private fun control() {
        val User = getToken()
        email = User!!.email
        val control = controlsignal(email, asleep)
        val apiService = Client.getApiService()
        val call = apiService.start_stop(control)
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                val apiResponse = response.body()

                if (response.isSuccessful){
                    Log.d("RetrofitDebug", "API call successful")
                    if (asleep){
                        showToast("Started the device")}
                    else{
                        showToast("Stopped the device")
                        val emailsend = email(email)
                        val apiService = Client.getApiService()
                        val call = apiService.analytics(emailsend)
                        call.enqueue(object : Callback<AnalyticsResponse> {
                            override fun onResponse(call: Call<AnalyticsResponse>, response: Response<AnalyticsResponse>) {
                                val apiResponse = response.body()
                                if (response.isSuccessful) {
                                    val body = response.body()
                                    val analytics = body?.analytics
                                    if(analytics != null){
                                        saveAnalytics(analytics)
                                    }
                                    Log.d("response" , "")
                                    Log.d("response", "Analytics: $analytics")

                                } else {
                                    showToast("Server Error")
                                }
                            }
                            override fun onFailure(call: Call<AnalyticsResponse>, t: Throwable) {
                                showToast("Network error")
                            }
                        })


                    }
                }
                else{
                    showToast("Server error")
                }

            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                showToast("Network error")
                Log.e("RetrofitDebug", "API call failed: ${t.message}")

            }
        })

    }



    fun getToken(): User? {
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val userJson = sharedPreferences.getString("user", null)
        return Gson().fromJson(userJson, User::class.java)
    }


    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        Client.init(requireContext())
    }

    private fun saveAnalytics(analytics: analytics?) {
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val analyticsJson = Gson().toJson(analytics)
        editor.putString("analytics", analyticsJson)
        editor.apply()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}