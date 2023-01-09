package com.blogspot.bunnylists.maate.activities.RegisterScreens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.navigation.fragment.findNavController
import com.blogspot.bunnylists.maate.R
import com.blogspot.bunnylists.maate.activities.MainScreens.MainActivity
import com.blogspot.bunnylists.maate.models.Room
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SplashScreen : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        requireActivity().window.statusBarColor = Color.parseColor("#F43C4D")

        MobileAds.initialize(requireContext())
        val mAuth = FirebaseAuth.getInstance()
        Handler(Looper.getMainLooper()).postDelayed({
            if(mAuth.currentUser!=null) {
                activity?.startActivity(Intent(activity, MainActivity::class.java))
                activity?.finish()
            }
            else
                findNavController().navigate(R.id.action_splashScreen_to_loginScreen)
        }, 1000)
        return inflater.inflate(R.layout.fragment_splash_screen, container, false)
    }
}