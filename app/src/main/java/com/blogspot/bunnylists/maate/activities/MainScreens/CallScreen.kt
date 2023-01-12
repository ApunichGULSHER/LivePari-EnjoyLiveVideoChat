package com.blogspot.bunnylists.maate.activities.MainScreens

import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import com.blogspot.bunnylists.maate.R
import com.blogspot.bunnylists.maate.Utils.MyApplication
import com.blogspot.bunnylists.maate.Utils.NetworkResult
import com.blogspot.bunnylists.maate.databinding.FragmentCallScreenBinding
import com.blogspot.bunnylists.maate.models.Room
import com.blogspot.bunnylists.maate.viewModels.MainScreenViewModel
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.thecode.aestheticdialogs.*
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import java.util.*


class CallScreen : Fragment() {
    private lateinit var bind: FragmentCallScreenBinding
    private val mViewModel: MainScreenViewModel by activityViewModels {
        MainScreenViewModel.provideFactory(
            (requireActivity().application as MyApplication).repository, this
        )
    }

    private var mInterstitialAd: InterstitialAd? = null
    private var micEnabled = true
    private var webcamEnabled = true

    private var channelName: String? = null
    private var token: String? = null
    private var intUid: Int? = null
    private var isJoined = false
    private var agoraEngine: RtcEngine? = null
    private var remoteSurfaceView: SurfaceView? = null
    private var localSurfaceView: SurfaceView? = null
    private var adTimer: CountDownTimer? = null
    private var serviceTimer: CountDownTimer? = null

    private val myUid = FirebaseAuth.getInstance().currentUser!!.uid
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        bind = FragmentCallScreenBinding.inflate(inflater, container, false)
        mViewModel.getNextRoom(myUid)
        setupVideoSDKEngine()
        setActionListeners()

        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                val f: Fragment? = parentFragmentManager.findFragmentById(R.id.container)
                val ft: FragmentTransaction = parentFragmentManager.beginTransaction()
                f?.let {
                    ft.remove(it)
                    ft.commit()
                }
                val nextFrag = AddBalanceScreen()
                (activity as MainActivity).bind.bottomBar.itemActiveIndex = 2
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, nextFrag, "CallFragment")
                    .commit()
            }
        })

        mViewModel.findingRoomTask.observe(viewLifecycleOwner) {
            when (it) {
                is NetworkResult.Loading -> {
                    bind.trialProgressBar.isVisible = false
                    bind.trialText.isVisible = false
                    bind.localVideoContainer.isVisible = false
                    bind.remoteVideoContainer.isVisible = false
                    bind.findingRoomProgressBar.isVisible = true
                    bind.toggleButtons.isVisible = false
                    bind.swipeRoomButton.isVisible = false
                }
                is NetworkResult.Success -> {
                    setUpChannel(mViewModel.currentRoom.value!!)
                    mViewModel.setFindingRoomTaskNull()
                }
                is NetworkResult.Error -> {
                    if (!it.massage.isNullOrBlank()) {
                        makeToast(it.massage)
                        mViewModel.setFindingRoomTaskNull()
                    }
                }
            }
        }

        if (mViewModel.currentLobbyType == "Normal") {
            bind.bannerAdView.isVisible = true
            loadBannerAd()
            adTimer?.cancel()
            adTimer = getNewAdTimer().start()
        }

        mViewModel.roomNotExist.observe(viewLifecycleOwner) {
            if (it == true) {
                leaveChannel()
                mViewModel.setRoomExistNull()
                mViewModel.setCurrentRoomNull()
                mViewModel.getNextRoom(myUid)
                serviceTimer?.cancel()
            }
        }

        mViewModel.user.observe(viewLifecycleOwner) {
            if (it.balance.toFloat() < 2 && mViewModel.currentLobbyType == "Royal" && !mViewModel.user.value!!.model) {
                balanceAlert.show()
            }
        }

    }

    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = requireContext()
            config.mAppId = getString(R.string.Agora_App_ID)
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            agoraEngine!!.enableVideo()
        } catch (e: Exception) {
            makeToast(e.localizedMessage)
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        val container: FrameLayout = requireActivity().findViewById(R.id.remote_video_container)
        remoteSurfaceView = SurfaceView(requireContext())
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        container.addView(remoteSurfaceView)
        agoraEngine?.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
        remoteSurfaceView!!.visibility = View.VISIBLE
    }

    private fun setupLocalVideo() {
        val container: FrameLayout = requireActivity().findViewById(R.id.local_video_container)
        localSurfaceView = SurfaceView(requireContext())
        container.addView(localSurfaceView)
        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                0
            )
        )
        localSurfaceView!!.visibility = View.VISIBLE
    }

    private fun setUpChannel(room: Room) {
        if (room.Host == myUid) {
            channelName = randomChannel()
            intUid = randomNumber()
            mViewModel.setChannelName(channelName!!, room)
            mViewModel.getAgoraToken(channelName!!, intUid!!)
            joinChannel()
        } else {
            mViewModel.getChannelName(room)
            mViewModel.channelName.observe(viewLifecycleOwner) {
                if (it != null) {
                    if (it is DatabaseError) {
                        makeToast(it.message)
                        mViewModel.resetChannelName()
                    } else {
                        channelName = it.toString()
                        mViewModel.resetChannelName()
                        intUid = randomNumber()
                        mViewModel.getAgoraToken(channelName!!, intUid!!)
                        joinChannel()
                    }
                }
            }
        }
    }

    private fun joinChannel() {
        mViewModel.agoraToken.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it is Exception) {
                    makeToast(it.localizedMessage)
                } else {
                    token = it.toString()
                    mViewModel.resetAgoraToken()
                    val options = ChannelMediaOptions()
                    options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                    options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                    setupLocalVideo()
                    agoraEngine!!.startPreview()
                    agoraEngine!!.joinChannel(token, channelName, randomNumber(), options)
                    bind.localVideoContainer.isVisible = true
                    bind.remoteVideoContainer.isVisible = true
                    bind.findingRoomProgressBar.isVisible = false
                    bind.toggleButtons.isVisible = true
                    bind.swipeRoomButton.isVisible = true

                    if(mViewModel.currentLobbyType == "Royal"){
                        serviceTimer?.cancel()
                        serviceTimer = getTrial().start()
                    }
                }
            }
        }
    }

    private fun leaveChannel() {
        if (isJoined) {
            agoraEngine!!.leaveChannel()
            if (remoteSurfaceView != null) remoteSurfaceView!!.visibility = View.GONE
            if (localSurfaceView != null) localSurfaceView!!.visibility = View.GONE
            isJoined = false
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            requireActivity().runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoined = true
        }

        override fun onUserOffline(uid: Int, reason: Int) {

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mViewModel.removeCurrentRoom(mViewModel.currentRoom.value?.Host ?: myUid)
        mViewModel.setCurrentRoomNull()
        adTimer?.cancel()
        serviceTimer?.cancel()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()
        token = null
        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }

    private fun setActionListeners() {
        bind.micButton.setOnClickListener {
            if (micEnabled) {
                agoraEngine!!.muteLocalAudioStream(true)
                bind.micButton.setImageDrawable(requireContext().getDrawable(R.drawable.mic_off))
                makeToast("Mic turned off")
            } else {
                agoraEngine!!.muteLocalAudioStream(false)
                bind.micButton.setImageDrawable(requireContext().getDrawable(R.drawable.mic_on))
                makeToast("Mic turned on")
            }
            micEnabled = !micEnabled
        }

        bind.videoButton.setOnClickListener {
            if (webcamEnabled) {
                agoraEngine!!.muteLocalVideoStream(true)
                bind.videoButton.setImageDrawable(requireContext().getDrawable(R.drawable.videocam_off))
                makeToast("Camera turned off")
            } else {
                agoraEngine!!.muteLocalVideoStream(false)
                bind.videoButton.setImageDrawable(requireContext().getDrawable(R.drawable.videocam_on))
                makeToast("Camera turned on")
            }
            webcamEnabled = !webcamEnabled
        }

        bind.swipeRoomButton.setOnClickListener {
            mViewModel.removeCurrentRoom(mViewModel.currentRoom.value!!.Host)
            mViewModel.setCurrentRoomNull()
        }

        bind.backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
            leaveChannel()
        }
    }

    private fun randomChannel(): String {
        return UUID.randomUUID().toString()
    }

    private fun randomNumber(): Int {
        return Math.random().toInt()
    }

    private fun getNewAdTimer(): CountDownTimer {
        return object : CountDownTimer(mViewModel.callAdTimeGap.value!!, 1000) {
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                loadAd()
            }
        }
    }

    private fun getTrial():  CountDownTimer{
        bind.trialProgressBar.isVisible = true
        bind.trialText.isVisible = true
        return object : CountDownTimer(10000, 1000){
            override fun onTick(millisUntilFinished: Long) {
                bind.trialText.text = (millisUntilFinished/1000 + 1).toString()
                bind.trialProgressBar.progress = (millisUntilFinished/100).toInt()
            }
            override fun onFinish() {
                bind.trialProgressBar.isVisible = false
                bind.trialText.isVisible = false
                serviceTimer!!.cancel()
                serviceTimer = getNewServiceTimer().start()
            }
        }
    }

    private fun getNewServiceTimer(): CountDownTimer {
        if(!mViewModel.user.value!!.model)
            mViewModel.chargeUser()
        return object : CountDownTimer(60000, 1000){
            override fun onTick(muf: Long) {

            }
            override fun onFinish() {
                if(mViewModel.user.value!!.model)
                    mViewModel.chargeModel()

                serviceTimer!!.cancel()
                serviceTimer = getNewServiceTimer().start()
            }
        }
    }

    private fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(requireContext(),
            getString(R.string.Live_Interstitial_AD),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                    adTimer?.cancel()
                    adTimer = getNewAdTimer().start()
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    mInterstitialAd!!.show(requireActivity())
                    mInterstitialAd?.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                adTimer?.cancel()
                                adTimer = getNewAdTimer().start()
                                mInterstitialAd = null
                            }

                            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                adTimer?.cancel()
                                adTimer = getNewAdTimer().start()
                                mInterstitialAd = null
                            }
                        }
                }
            })
    }

    private fun loadBannerAd() {
        val adRequest = AdRequest.Builder().build()
        bind.bannerAdView.loadAd(adRequest)
    }

    private fun makeToast(message: String){
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}