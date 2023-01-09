@file:Suppress("UNREACHABLE_CODE")

package com.blogspot.bunnylists.maate.activities.MainScreens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.blogspot.bunnylists.maate.R
import com.blogspot.bunnylists.maate.Utils.MyApplication
import com.blogspot.bunnylists.maate.databinding.FragmentNormalLobbyBinding
import com.blogspot.bunnylists.maate.viewModels.MainScreenViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class NormalScreen : Fragment() {

    private val PERMISSION_REQ_ID = 101
    private val REQUESTED_PERMISSIONS =
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)

    private lateinit var bind: FragmentNormalLobbyBinding
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
        bind = FragmentNormalLobbyBinding.inflate(inflater, container, false)
        val repository = (requireActivity().application as MyApplication).repository

        bind.EnterLobbyButton.setOnClickListener {
            if (!checkSelfPermission()) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    REQUESTED_PERMISSIONS,
                    PERMISSION_REQ_ID
                )
            } else {
                repository.setLobbyType("Normal")
                mViewModel.buildQuery()
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.container, CallScreen())
                    .addToBackStack(null)
                    .commit()
            }
        }

        return bind.root
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