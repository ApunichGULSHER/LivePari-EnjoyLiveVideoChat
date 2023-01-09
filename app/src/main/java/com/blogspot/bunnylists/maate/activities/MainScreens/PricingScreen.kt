package com.blogspot.bunnylists.maate.activities.MainScreens

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.blogspot.bunnylists.maate.R
import com.blogspot.bunnylists.maate.Utils.MyApplication
import com.blogspot.bunnylists.maate.databinding.FragmentAddBalanceScreenBinding
import com.blogspot.bunnylists.maate.databinding.FragmentPricingScreenBinding
import com.blogspot.bunnylists.maate.viewModels.MainScreenViewModel

class PricingScreen : Fragment(){
    private val mViewModel: MainScreenViewModel by activityViewModels() {
        MainScreenViewModel.provideFactory(
            (requireActivity().application as MyApplication).repository,
            this
        )
    }
    private lateinit var bind: FragmentPricingScreenBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bind = FragmentPricingScreenBinding.inflate(inflater, container, false)
        bind.lifecycleOwner = this

        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.royalCallsPrice.observe(viewLifecycleOwner){
            bind.price = it
        }

        mViewModel.user.observe(viewLifecycleOwner){
            if (it.model){
                bind.billingNote.text = getString(R.string.model_billing_note)
            }else{
                bind.billingNote.text = getString(R.string.normal_user_billing_note)
            }
        }
    }

}