package com.blogspot.bunnylists.maate.activities.MainScreens

import android.R.attr.bitmap
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.blogspot.bunnylists.maate.R
import com.blogspot.bunnylists.maate.Utils.MyApplication
import com.blogspot.bunnylists.maate.Utils.NetworkResult
import com.blogspot.bunnylists.maate.activities.RegisterScreens.RegisterActivity
import com.blogspot.bunnylists.maate.databinding.FragmentAccountScreenBinding
import com.blogspot.bunnylists.maate.models.LoadingDialog
import com.blogspot.bunnylists.maate.viewModels.MainScreenViewModel
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream


class AccountScreen : Fragment() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){ isGranted ->
        if (isGranted) {
            pickImageFromGallery()
        }
        else {
            Toast.makeText(requireContext(), "Permission required", Toast.LENGTH_SHORT).show()
        }
    }
    private val getImageFromGallery = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode== Activity.RESULT_OK){
            val bmp =if (android.os.Build.VERSION.SDK_INT >= 29){
                val source: ImageDecoder.Source = ImageDecoder.createSource(requireContext().contentResolver, result.data!!.data!!)
                ImageDecoder.decodeBitmap(source)
            } else{
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, result.data!!.data)
            }
            val bas = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 20, bas)
            val data = bas.toByteArray()
            pickedImage = data
            bind.userImage.setImageURI(result.data!!.data!!)
        }
    }
    private val mViewModel: MainScreenViewModel by activityViewModels {
        MainScreenViewModel.provideFactory((requireActivity().application as MyApplication).repository, this)
    }
    private lateinit var bind : FragmentAccountScreenBinding
    private var pickedImage : ByteArray? = null
    private val myUid = FirebaseAuth.getInstance().currentUser!!.uid
    private lateinit var loadingDialog: LoadingDialog
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        bind = FragmentAccountScreenBinding.inflate(inflater, container, false)
        loadingDialog = LoadingDialog(requireActivity())

        bind.userImage.setOnClickListener {
            if(ActivityCompat.checkSelfPermission(requireContext(),android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_DENIED){
                permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }else
                pickImageFromGallery()
        }

        bind.saveButton.setOnClickListener {
            val newName = bind.userName.text.toString()
            val newAge = bind.userAge.text.toString()
            mViewModel.updateProfile(pickedImage, newName, newAge, myUid)
        }

        bind.logoutButton.setOnClickListener {
            mViewModel.logout()
        }

        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.user.observe(viewLifecycleOwner){
            bind.user = it
        }
        mViewModel.loggedIn.observe(viewLifecycleOwner){
            if(!it){
                val intent = Intent(activity, RegisterActivity::class.java)
                activity?.startActivity(intent)
                activity?.finishAffinity()
            }
        }
        mViewModel.updateProfileTask.observe(viewLifecycleOwner){
            when(it){
                is NetworkResult.Loading -> { loadingDialog.startLoading() }
                is NetworkResult.Success -> {
                    loadingDialog.isDismiss()
                    mViewModel.setUpdateTaskNull()
                    requireActivity().supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.pop_enter, R.anim.pop_exit, R.anim.enter, R.anim.exit)
                        .replace(R.id.fragment_container, ProfileScreen())
                        .commit()
                }
                is NetworkResult.Error -> {
                    loadingDialog.isDismiss()
                    if(!it.massage.isNullOrBlank()) {
                        Toast.makeText(requireContext(), it.massage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    }
    private fun pickImageFromGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        getImageFromGallery.launch(intent)
    }


}