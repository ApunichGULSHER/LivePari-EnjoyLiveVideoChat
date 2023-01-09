package com.blogspot.bunnylists.maate.activities.RegisterScreens

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.blogspot.bunnylists.maate.R
import com.blogspot.bunnylists.maate.databinding.FragmentModelTermsScreenBinding
import com.blogspot.bunnylists.maate.databinding.FragmentTCPolicyScreenBinding

class TC_Policy_Screen : Fragment() {


    private lateinit var bind : FragmentTCPolicyScreenBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bind = FragmentTCPolicyScreenBinding.inflate(inflater, container, false)

        bind.terms.movementMethod = ScrollingMovementMethod()

        bind.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        return bind.root
    }

}