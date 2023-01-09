package com.blogspot.bunnylists.maate.activities.RegisterScreens

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.blogspot.bunnylists.maate.R
import com.blogspot.bunnylists.maate.Utils.MyApplication
import com.blogspot.bunnylists.maate.Utils.NetworkResult
import com.blogspot.bunnylists.maate.activities.MainScreens.MainActivity
import com.blogspot.bunnylists.maate.databinding.FragmentLoginScreenBinding
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
import java.util.concurrent.TimeUnit

class LoginScreen : Fragment() {
    private val googleSignInActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadingDialog.startLoading()
                val task: Task<GoogleSignInAccount> =
                    GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account: GoogleSignInAccount = task.result
                mViewModel.signInWithGoogleNormal(account.idToken)
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


    private lateinit var bind: FragmentLoginScreenBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var loadingDialog: LoadingDialog
    private val mViewModel: RegisterScreenViewModel by activityViewModels() {
        RegisterScreenViewModel.provideFactory(
            (requireActivity().application as MyApplication).repository,
            this
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bind = FragmentLoginScreenBinding.inflate(inflater, container, false)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        requireActivity().window.statusBarColor = Color.parseColor("#FFFFFF")
        val mFAuth = (requireActivity().application as MyApplication).mFAuth
        loadingDialog = LoadingDialog(requireActivity())

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
                val options = PhoneAuthOptions.newBuilder(mFAuth)
                    .setPhoneNumber(number)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(requireActivity())
                    .setCallbacks(callbacks)
                    .build()
                PhoneAuthProvider.verifyPhoneNumber(options)
            }
        })

        bind.termsNote.setOnClickListener {
            findNavController().navigate(R.id.action_loginScreen_to_TC_Policy_Screen)
        }

        bind.registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginScreen_to_modelTermsScreen)
        }

        bind.googleLoginButton.setOnClickListener {
            termsAlertForGoogle.show()
        }

        bind.viewModel = mViewModel
        bind.lifecycleOwner = this

        bind.otpSendButton.setOnClickListener {
            val number: String = mViewModel.phoneNumber.value.toString()
            if (number.isBlank() || number.length < 10) {
                Toast.makeText(
                    requireContext(),
                    "Enter a valid number!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                bind.otpBox.isVisible = false
                termsAlertForPhone.show()
            }
        }

        bind.otpVerifyButton.setOnClickListener {
            if (mViewModel.otp.value.isNullOrBlank())
                bind.otpEdtTxt.error = "Enter OTP!"
            else
                mViewModel.signInWithPhoneNormal()
        }

        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.loginTask.observe(viewLifecycleOwner) {
            when (it) {
                is NetworkResult.Loading -> {
                    loadingDialog.startLoading()
                }
                is NetworkResult.Success -> {
                    loadingDialog.isDismiss()
                    val intent = Intent(activity, MainActivity::class.java)
                    activity?.startActivity(intent)
                    activity?.finishAffinity()
                }
                is NetworkResult.Error -> {
                    loadingDialog.isDismiss()
                    if (!it.massage.isNullOrBlank()) {
                        Toast.makeText(requireContext(), it.massage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}