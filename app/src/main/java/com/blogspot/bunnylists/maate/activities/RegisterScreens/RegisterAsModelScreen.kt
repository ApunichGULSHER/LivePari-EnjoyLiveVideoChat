package com.blogspot.bunnylists.maate.activities.RegisterScreens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.blogspot.bunnylists.maate.R
import com.blogspot.bunnylists.maate.Utils.MyApplication
import com.blogspot.bunnylists.maate.Utils.NetworkResult
import com.blogspot.bunnylists.maate.activities.MainScreens.MainActivity
import com.blogspot.bunnylists.maate.databinding.FragmentRegisterAsModelScreenBinding
import com.blogspot.bunnylists.maate.models.LoadingDialog
import com.blogspot.bunnylists.maate.viewModels.RegisterScreenViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.thecode.aestheticdialogs.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit


class RegisterAsModelScreen : Fragment() {

    private lateinit var bind : FragmentRegisterAsModelScreenBinding
    private lateinit var loadingDialog: LoadingDialog
    private val mViewModel: RegisterScreenViewModel by activityViewModels() {
        RegisterScreenViewModel.provideFactory((requireActivity().application as MyApplication).repository, this)
    }
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private val googleSignInActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if (result.resultCode == Activity.RESULT_OK){
                loadingDialog.startLoading()
                val task : Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account : GoogleSignInAccount = task.result
                mViewModel.signInWithGoogleModel(account.idToken)
            }
        }
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){ isGranted ->
        if (isGranted) {
            pickImageFromGallery()
        }
        else {
            Toast.makeText(requireContext(), "Permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private val PERMISSION_REQ_ID = 1
    private val REQUESTED_PERMISSIONS =
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private val getImageFromGallery = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode==Activity.RESULT_OK){
            val bmp =if (android.os.Build.VERSION.SDK_INT >= 29){
                val source: ImageDecoder.Source = ImageDecoder.createSource(requireContext().contentResolver, result.data!!.data!!)
                ImageDecoder.decodeBitmap(source)
            } else{
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, result.data!!.data)
            }
            val bas = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 20, bas)
            val data = bas.toByteArray()
            mViewModel.uploadModelImage(data, result.data!!.data!!)
        }
    }

    private val recordSelfieVideo = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode==Activity.RESULT_OK){
            mViewModel.uploadSelfieVideo(result.data!!.data!!)
        }
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        // This method is called when the verification is completed
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            val intent = Intent(activity, MainActivity::class.java)
            activity?.startActivity(intent)
            activity?.finishAffinity()
        }
        // Called when verification is failed add log statement to see the exception
        override fun onVerificationFailed(e: FirebaseException) {
            Toast.makeText(requireContext(), e.localizedMessage, Toast.LENGTH_SHORT).show()
            bind.otpSendProgressBar.isVisible = false
            bind.otpSendButton.isVisible = true
        }
        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            mViewModel.verificationID = verificationId
            bind.otpSendProgressBar.isVisible = false
            bind.otpSendButton.isVisible = true
            bind.otpBox.isVisible = true
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bind = FragmentRegisterAsModelScreenBinding.inflate(inflater, container, false)
        val mFAuth = (requireActivity().application as MyApplication).mFAuth
        loadingDialog = LoadingDialog(requireActivity())
        bind.viewModel = mViewModel
        bind.lifecycleOwner = this

        val termsAlertForGoogle =
            AestheticDialog.Builder(requireActivity(), DialogStyle.FLAT, DialogType.SUCCESS)
                .setTitle("Do you Agree?")
                .setCancelable(true)
                .setMessage("By clicking ok, you are giving consent to our terms and conditions\nWhich are described in the below link!")
                .setGravity(Gravity.CENTER)
                .setDarkMode(true)
                .setAnimation(DialogAnimation.DEFAULT)


        termsAlertForGoogle.setOnClickListener(object : OnDialogClickListener {
            override fun onClick(dialog: AestheticDialog.Builder) {
                termsAlertForGoogle.dismiss()

                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
                val intent = mGoogleSignInClient.signInIntent
                googleSignInActivityResultLauncher.launch(intent)
            }
        })

        val termsAlertForPhone =
            AestheticDialog.Builder(requireActivity(), DialogStyle.FLAT, DialogType.SUCCESS)
                .setTitle("Do you Agree?")
                .setCancelable(true)
                .setMessage("By clicking ok, you are giving consent to our terms and conditions\nWhich are described in the below link!")
                .setGravity(Gravity.CENTER)
                .setDarkMode(true)
                .setAnimation(DialogAnimation.DEFAULT)


        termsAlertForPhone.setOnClickListener(object : OnDialogClickListener {
            override fun onClick(dialog: AestheticDialog.Builder) {
                termsAlertForPhone.dismiss()

                var number: String = mViewModel.phoneNumber.value.toString()
                bind.otpSendButton.isVisible = false
                bind.otpSendProgressBar.isVisible = true
                number = "+91$number"
                val options= PhoneAuthOptions.newBuilder(mFAuth)
                    .setPhoneNumber(number)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(requireActivity())
                    .setCallbacks(callbacks)
                    .build()
                PhoneAuthProvider.verifyPhoneNumber(options)
            }
        })

        bind.backButton.setOnClickListener {
            findNavController().popBackStack()
            mViewModel.deletePreviousImage()
            mViewModel.setUserImageNull()
            mViewModel.deletePreviousVideo()
        }

        bind.termsNote.setOnClickListener {
            findNavController().navigate(R.id.action_registerAsModelScreen_to_TC_Policy_Screen)
        }

        bind.userImage.setOnClickListener {
            if(ActivityCompat.checkSelfPermission(requireContext(),android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_DENIED){
                storagePermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }else
                pickImageFromGallery()
        }

        bind.recordButton.setOnClickListener {
            if(!checkSelfPermission()){
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    REQUESTED_PERMISSIONS,
                    PERMISSION_REQ_ID
                )
            }else{
                recordSelfie()
            }
        }

        bind.googleLoginButton.setOnClickListener {
            if(mViewModel.name.value.isNullOrBlank() || mViewModel.age.value.isNullOrBlank())
                Toast.makeText(requireContext(), "Fill Name and Age!", Toast.LENGTH_SHORT).show()
            else if(mViewModel.userImage.value==null || mViewModel.modelVideoStorageUrl==null)
                Toast.makeText(requireContext(), "Set profile photo and record selfie video!", Toast.LENGTH_SHORT).show()
            else {
                termsAlertForGoogle.show()
            }
        }

        bind.otpSendButton.setOnClickListener {
            val number: String = mViewModel.phoneNumber.value.toString()
            if(mViewModel.name.value.isNullOrBlank() || mViewModel.age.value.isNullOrBlank())
                Toast.makeText(requireContext(), "Fill Name and Age!", Toast.LENGTH_SHORT).show()
            else if(mViewModel.userImage.value==null || mViewModel.modelVideoStorageUrl==null)
                Toast.makeText(requireContext(), "Set profile photo and record selfie video!", Toast.LENGTH_SHORT).show()
            else if(number.isBlank() || number.length < 10) {
                Toast.makeText(requireContext(), "Enter a valid number!", Toast.LENGTH_SHORT).show()
            } else {
                bind.otpBox.isVisible = false
                termsAlertForPhone.show()
            }
        }

        bind.otpVerifyButton.setOnClickListener {
            if(mViewModel.otp.value.isNullOrBlank())
                bind.otpEdtTxt.error = "Enter OTP"
            else
                mViewModel.signInWithPhoneModel()
        }


        return bind.root
    }

    private fun recordSelfie() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10)
        recordSelfieVideo.launch(intent)
    }

    private fun pickImageFromGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        getImageFromGallery.launch(intent)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.loginTask.observe(viewLifecycleOwner){
            when(it){
                is NetworkResult.Loading -> { loadingDialog.startLoading()}
                is NetworkResult.Success -> {
                    loadingDialog.isDismiss()
                    activity?.startActivity(Intent(activity, MainActivity::class.java))
                    activity?.finishAffinity()
                }
                is NetworkResult.Error -> {
                    loadingDialog.isDismiss()
                    if(!it.massage.isNullOrBlank()) {
                        Toast.makeText(requireContext(), it.massage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        mViewModel.imageUploadTask.observe(viewLifecycleOwner){
            when(it){
                is NetworkResult.Loading->{ loadingDialog.startLoading()}
                is NetworkResult.Success->{
                    loadingDialog.isDismiss()
                    mViewModel.modelImageStorageUrl = it.data.toString()
                    Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error->{
                    loadingDialog.isDismiss()
                    Toast.makeText(requireContext(), it.massage, Toast.LENGTH_SHORT).show()}
            }
        }
        mViewModel.videoUploadTask.observe(viewLifecycleOwner){
            when(it){
                is NetworkResult.Loading->{ loadingDialog.startLoading()}
                is NetworkResult.Success->{
                    loadingDialog.isDismiss()
                    mViewModel.modelVideoStorageUrl = it.data.toString()
                    Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error->{
                    loadingDialog.isDismiss()
                    Toast.makeText(requireContext(), it.massage, Toast.LENGTH_SHORT).show()}
            }
        }
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