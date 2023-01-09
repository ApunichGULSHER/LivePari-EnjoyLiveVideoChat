package com.blogspot.bunnylists.maate.activities.RegisterScreens

import android.graphics.Color
import android.os.Bundle
import android.text.method.MovementMethod
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.blogspot.bunnylists.maate.R
import com.blogspot.bunnylists.maate.databinding.FragmentModelTermsScreenBinding
import com.blogspot.bunnylists.maate.databinding.FragmentProfileScreenBinding
import com.thecode.aestheticdialogs.*

class ModelTermsScreen : Fragment() {

    private lateinit var bind : FragmentModelTermsScreenBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bind = FragmentModelTermsScreenBinding.inflate(inflater, container, false)


        bind.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        bind.agreeButton.setOnClickListener {
            findNavController().navigate(R.id.action_modelTermsScreen_to_registerAsModelScreen)
        }

        return bind.root
    }


}