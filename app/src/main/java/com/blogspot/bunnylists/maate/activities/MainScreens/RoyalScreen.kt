package com.blogspot.bunnylists.maate.activities.MainScreens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.blogspot.bunnylists.maate.R
import com.blogspot.bunnylists.maate.Utils.MyApplication
import com.blogspot.bunnylists.maate.databinding.FragmentRoyalLobbyBinding
import com.blogspot.bunnylists.maate.viewModels.MainScreenViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.thecode.aestheticdialogs.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class RoyalScreen : Fragment() {

    private val PERMISSION_REQ_ID = 101
    private val REQUESTED_PERMISSIONS =
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)

    private lateinit var bind: FragmentRoyalLobbyBinding
    private val mViewModel: MainScreenViewModel by activityViewModels() {
        MainScreenViewModel.provideFactory(
            (requireActivity().application as MyApplication).repository,
            this
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        bind = FragmentRoyalLobbyBinding.inflate(inflater, container, false)
        val repository = (requireActivity().application as MyApplication).repository

        val balanceAlert =
            AestheticDialog.Builder(requireActivity(), DialogStyle.FLAT, DialogType.WARNING)
                .setTitle("Not enough balance!")
                .setMessage("Add balance to continue")
                .setCancelable(false)
                .setGravity(Gravity.CENTER)
                .setDarkMode(true)
                .setAnimation(DialogAnimation.DEFAULT)


        balanceAlert.setOnClickListener(object : OnDialogClickListener {
            override fun onClick(dialog: AestheticDialog.Builder) {
                balanceAlert.dismiss()
                (activity as MainActivity).bind.bottomBar.itemActiveIndex = 2
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AddBalanceScreen())
                    .commit()
            }
        })

        val verificationAlert =
            AestheticDialog.Builder(requireActivity(), DialogStyle.FLAT, DialogType.WARNING)
                .setTitle("Profile not verified!")
                .setMessage("Please retry some time later")
                .setCancelable(true)
                .setGravity(Gravity.CENTER)
                .setDarkMode(false)
                .setAnimation(DialogAnimation.DEFAULT)


        verificationAlert.setOnClickListener(object : OnDialogClickListener {
            override fun onClick(dialog: AestheticDialog.Builder) {
                verificationAlert.dismiss()
            }
        })

        bind.EnterLobbyButton.setOnClickListener {
            if (!checkSelfPermission()) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    REQUESTED_PERMISSIONS,
                    PERMISSION_REQ_ID
                )
            } else {
                if (mViewModel.user.value!!.model) {
                    if (!mViewModel.user.value!!.verified)
                        verificationAlert.show()
                    else {
                        repository.setLobbyType("Royal")
                        mViewModel.buildQuery()
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.container, CallScreen())
                            .addToBackStack(null)
                            .commit()
                    }
                } else {
                    if (haveEnoughBalance()) {
                        repository.setLobbyType("Royal")
                        mViewModel.buildQuery()
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.container, CallScreen())
                            .addToBackStack(null)
                            .commit()
                    } else {
                        balanceAlert.show()
                    }
                }
            }
        }

        return bind.root
    }

    private fun haveEnoughBalance(): Boolean {
        return mViewModel.user.value!!.balance.toFloat() > 2
    }

    private fun checkSelfPermission(): Boolean {
        return !(ContextCompat.checkSelfPermission(
            requireContext(),
            REQUESTED_PERMISSIONS[0]
        ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    REQUESTED_PERMISSIONS[1]
                ) != PackageManager.PERMISSION_GRANTED)
    }
}