package com.blogspot.bunnylists.maate.activities.MainScreens

import android.annotation.SuppressLint
import android.content.Context
import android.os.*
import android.view.Gravity
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.blogspot.bunnylists.maate.R
import com.blogspot.bunnylists.maate.Utils.ConnectivityObserver
import com.blogspot.bunnylists.maate.Utils.MyApplication
import com.blogspot.bunnylists.maate.Utils.NetworkConnectivityObserver
import com.blogspot.bunnylists.maate.Utils.NetworkResult
import com.blogspot.bunnylists.maate.databinding.ActivityMainBinding
import com.blogspot.bunnylists.maate.models.LoadingDialog
import com.blogspot.bunnylists.maate.viewModels.MainScreenViewModel
import com.google.android.gms.ads.MobileAds
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.thecode.aestheticdialogs.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart


class MainActivity : AppCompatActivity(), PaymentResultListener {
    internal lateinit var bind: ActivityMainBinding
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var vibrator: Vibrator
    private lateinit var vibrationEffect: VibrationEffect
    private val mViewModel: MainScreenViewModel by viewModels() {
        MainScreenViewModel.provideFactory((application as MyApplication).repository, this)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Checkout.preload(applicationContext)
        bind = DataBindingUtil.setContentView(this, R.layout.activity_main)
        connectivityObserver = NetworkConnectivityObserver(applicationContext)
        loadingDialog = LoadingDialog(this)
        vibrationEffect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        bind.bottomBar.onItemSelected = {
            mViewModel.changeFragment(it)
        }

        val connectionAlert =
            AestheticDialog.Builder(this, DialogStyle.CONNECTIFY, DialogType.ERROR)
                .setTitle("No internet")
                .setMessage("Connect to internet to continue")
                .setCancelable(false)
                .setGravity(Gravity.CENTER)
                .setAnimation(DialogAnimation.DEFAULT)
                .setOnClickListener(object : OnDialogClickListener {
                    override fun onClick(dialog: AestheticDialog.Builder) {
                        dialog.dismiss()
                        finishAffinity()
                    }
                })

        connectivityObserver.observe().onStart { emit(ConnectivityObserver.Status.Unavailable) }
            .onEach {
                when (it) {
                    ConnectivityObserver.Status.Unavailable -> {
                        connectionAlert.show()
                    }
                    ConnectivityObserver.Status.Available -> {
                        connectionAlert.dismiss()
                    }
                    ConnectivityObserver.Status.Lost -> {
                        connectionAlert.show()
                    }
                    else -> {

                    }
                }
            }.launchIn(lifecycleScope)

        mViewModel.user.observe(this) {
            bind.user = it
        }

        mViewModel.gettingUserDataTask.observe(this) {
            when (it) {
                is NetworkResult.Loading -> {
                    loadingDialog.startLoading()
                }
                is NetworkResult.Success -> {
                    loadingDialog.isDismiss()
                }
                is NetworkResult.Error -> {
                    loadingDialog.isDismiss()
                    if(it.massage!=null)
                        Toast.makeText(this, "Please try later! ${it.massage}", Toast.LENGTH_SHORT).show()
                    finishAffinity()
                }
            }
        }

        mViewModel.currentFragment.observe(this) {
            when (it) {
                0 -> loadLobby(NormalScreen())
                1 -> loadLobby(RoyalScreen())
                2 -> loadLobby(ProfileScreen())
            }
        }

    }

    private fun loadLobby(fragment: Fragment) {
        val fm: FragmentManager = supportFragmentManager
        val secondaryScreen: Fragment? =
            supportFragmentManager.findFragmentById(R.id.fragment_container)
        val ft: FragmentTransaction = fm.beginTransaction()

        if (secondaryScreen is NormalScreen)
            ft.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit)
        else if (fragment is NormalScreen && secondaryScreen is RoyalScreen)
            ft.setCustomAnimations(R.anim.pop_enter, R.anim.pop_exit, R.anim.enter, R.anim.exit)
        else if (fragment is ProfileScreen && secondaryScreen is RoyalScreen)
            ft.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit)
        else
            ft.setCustomAnimations(R.anim.pop_enter, R.anim.pop_exit, R.anim.enter, R.anim.exit)

        vibrator.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(vibrationEffect)
        }
        ft.replace(R.id.fragment_container, fragment)
        ft.commit()
    }

    override fun onBackPressed() {
        val mainScreen: Fragment? = supportFragmentManager.findFragmentById(R.id.container)
        val secondaryScreen: Fragment? =
            supportFragmentManager.findFragmentById(R.id.fragment_container)
        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()

//        if (supportFragmentManager.backStackEntryCount > 0)
//            supportFragmentManager.popBackStack()

        when {
            mainScreen is CallScreen -> {
                supportFragmentManager.popBackStack()
            }
            secondaryScreen is NormalScreen -> {
                bind.bottomBar.itemActiveIndex = 1
                ft.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit)
                ft.replace(R.id.fragment_container, RoyalScreen())
                ft.commit()
            }
            secondaryScreen is ProfileScreen -> {
                bind.bottomBar.itemActiveIndex = 1
                ft.setCustomAnimations(R.anim.pop_enter, R.anim.pop_exit, R.anim.enter, R.anim.exit)
                ft.replace(R.id.fragment_container, RoyalScreen())
                ft.commit()
            }
            secondaryScreen is AddBalanceScreen -> {
                ft.setCustomAnimations(R.anim.pop_enter, R.anim.pop_exit, R.anim.enter, R.anim.exit)
                ft.replace(R.id.fragment_container, ProfileScreen())
                ft.commit()
            }
            secondaryScreen is PricingScreen -> {
                ft.setCustomAnimations(R.anim.pop_enter, R.anim.pop_exit, R.anim.enter, R.anim.exit)
                ft.replace(R.id.fragment_container, ProfileScreen())
                ft.commit()
            }
            secondaryScreen is WithdrawBalanceScreen -> {
                ft.setCustomAnimations(R.anim.pop_enter, R.anim.pop_exit, R.anim.enter, R.anim.exit)
                ft.replace(R.id.fragment_container, ProfileScreen())
                ft.commit()
            }
            secondaryScreen is AccountScreen -> {
                ft.setCustomAnimations(R.anim.pop_enter, R.anim.pop_exit, R.anim.enter, R.anim.exit)
                ft.replace(R.id.fragment_container, ProfileScreen())
                ft.commit()
            }
            else -> {
                finishAffinity()
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        try {
            mViewModel.resetInitTask()
            mViewModel.resetBalance()
        }catch (e: Exception){
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPaymentError(errorCode: Int, response: String?) {
        try {
            mViewModel.resetInitTask()
            mViewModel.amount = null
            Toast.makeText(this, "Payment Failed", Toast.LENGTH_SHORT).show()
        }catch (e: Exception){
            mViewModel.resetInitTask()
            mViewModel.amount = null
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }
}
