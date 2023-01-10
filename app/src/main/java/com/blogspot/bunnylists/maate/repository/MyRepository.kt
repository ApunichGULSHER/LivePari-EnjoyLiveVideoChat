package com.blogspot.bunnylists.maate.repository

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blogspot.bunnylists.maate.Utils.NetworkResult
import com.blogspot.bunnylists.maate.models.RazorpayOrder
import com.blogspot.bunnylists.maate.models.Room
import com.blogspot.bunnylists.maate.models.User
import com.blogspot.bunnylists.maate.models.WithdrawRequest
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.*
import com.google.firebase.database.*
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.GsonBuilder
import com.razorpay.Checkout
import org.json.JSONObject
import java.lang.Exception
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt
import kotlin.math.roundToLong


class MyRepository(
    private val mFAuth: FirebaseAuth,
    private val mFDb: FirebaseDatabase,
    private val mFStorage: FirebaseStorage,
    private val mFunctions: FirebaseFunctions
) {
    private val _loginTask = MutableLiveData<NetworkResult<String>>()
    private val _postUserToServerTask = MutableLiveData<NetworkResult<String>>()
    private val _gettingUserDataTask = MutableLiveData<NetworkResult<String>>()
    private val _imageUploadTask = MutableLiveData<NetworkResult<String>>()
    private val _videoUploadTask = MutableLiveData<NetworkResult<String>>()
    private val _transactionTask = MutableLiveData<NetworkResult<String>>()
    private val _findingRoomTask = MutableLiveData<NetworkResult<String>>()
    private val _paymentInitTask = MutableLiveData<NetworkResult<String>>()
    private val _resetBalanceTask = MutableLiveData<NetworkResult<String>>()
    private val _updateProfileTask = MutableLiveData<NetworkResult<String>>()
    private val _user = MutableLiveData<User>()
    private val _loggedIn = MutableLiveData<Boolean>()
    private val _currentRoom = MutableLiveData<Room?>(null)
    private val _roomFound = MutableLiveData<Boolean>()
    private val _roomNotExist = MutableLiveData<Boolean?>(null)
    private val _haveWithdrawRequests = MutableLiveData<WithdrawRequest?>(null)
    private val _agoraToken = MutableLiveData<Any?>(null)
    private val _channelName = MutableLiveData<Any?>()
    private val _royalLobbyCallsPrice= MutableLiveData<String>()
    var currentLobbyType = ""

    val royalLobbyCallsPrice: LiveData<String>
        get() = _royalLobbyCallsPrice

    val haveWithdrawRequests: LiveData<WithdrawRequest?>
        get() = _haveWithdrawRequests

    val channelName: LiveData<Any?>
        get() = _channelName

    val paymentInitTask: LiveData<NetworkResult<String>>
        get() = _paymentInitTask

    val resetBalanceTask: LiveData<NetworkResult<String>>
        get() = _resetBalanceTask

    val agoraToken: LiveData<Any?>
        get() = _agoraToken

    val updateProfileTask: LiveData<NetworkResult<String>>
        get() = _updateProfileTask

    val roomNotExist: LiveData<Boolean?>
        get() = _roomNotExist

    val currentRoom: LiveData<Room?>
        get() = _currentRoom

    val findingRoomTask: LiveData<NetworkResult<String>>
        get() = _findingRoomTask

    val transactionTask: LiveData<NetworkResult<String>>
        get() = _transactionTask

    val imageUploadTask: LiveData<NetworkResult<String>>
        get() = _imageUploadTask

    val videoUploadTask: LiveData<NetworkResult<String>>
        get() = _videoUploadTask

    val loggedIn: LiveData<Boolean>
        get() = _loggedIn

    val user: LiveData<User>
        get() = _user

    val gettingUserDataTask: LiveData<NetworkResult<String>>
        get() = _gettingUserDataTask

    val loginTask: LiveData<NetworkResult<String>>
        get() = _loginTask

    fun getCurrentUserData() {
        _roomFound.postValue(false)
        _gettingUserDataTask.postValue(NetworkResult.Loading())
        mFDb.reference.child("Profiles").child(mFAuth.currentUser!!.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = User(
                        snapshot.child("age").value.toString(),
                        snapshot.child("balance").value.toString(),
                        snapshot.child("model").value.toString().toBoolean(),
                        snapshot.child("verified").value.toString().toBoolean(),
                        snapshot.child("name").value.toString(),
                        snapshot.child("profilePic").value.toString(),
                        null
                    )
                    if(user.model){
                        mFDb.reference.child("Pricing").child("pricingForModel").addValueEventListener(object :ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                _royalLobbyCallsPrice.postValue(snapshot.value.toString())
                            }

                            override fun onCancelled(error: DatabaseError) {

                            }
                        })
                    }else{
                        mFDb.reference.child("Pricing").child("pricingForNormalUser").addValueEventListener(object : ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                _royalLobbyCallsPrice.postValue(snapshot.value.toString())
                            }

                            override fun onCancelled(error: DatabaseError) {

                            }

                        })
                    }
                    _user.postValue(user)
                    _gettingUserDataTask.postValue(NetworkResult.Success())
                }

                override fun onCancelled(error: DatabaseError) {
                    _gettingUserDataTask.postValue(NetworkResult.Error(massage = error.message))
                }
            })
    }

    fun signInWithGoogleNormal(idToken: String) {
        _loginTask.postValue(NetworkResult.Loading())
        val credential: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
        mFAuth.signInWithCredential(credential)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    _loggedIn.postValue(true)
                    postNormalUserToServer()
                } else
                    _loginTask.postValue((NetworkResult.Error(it.exception!!.localizedMessage)))
            }
    }

    fun signInWithGoogleModel(
        idToken: String,
        name: String,
        age: String,
        profileUrl: String,
        modelVideoStorageUrl: String
    ) {
        _loginTask.postValue(NetworkResult.Loading())
        val credential: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
        mFAuth.signInWithCredential(credential)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    _loggedIn.postValue(true)
                    postModelUserToServer(name, age, profileUrl, modelVideoStorageUrl)
                } else
                    _loginTask.postValue(NetworkResult.Error(it.exception!!.localizedMessage))
            }
    }

    fun signInWithPhoneNormal(credential: PhoneAuthCredential) {
        _loginTask.postValue(NetworkResult.Loading())
        mFAuth.signInWithCredential(credential)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    _loggedIn.postValue(true)
                    postNormalUserToServer()
                } else
                    _loginTask.postValue(NetworkResult.Error(it.exception!!.localizedMessage))
            }
    }

    fun signInWithPhoneModel(
        credential: PhoneAuthCredential,
        name: String,
        age: String,
        profileUrl: String,
        modelVideoStorageUrl: String,
    ) {
        _loginTask.postValue(NetworkResult.Loading())
        mFAuth.signInWithCredential(credential)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    _loggedIn.postValue(true)
                    postModelUserToServer(name, age, profileUrl, modelVideoStorageUrl)
                } else
                    _loginTask.postValue(NetworkResult.Error(it.exception!!.localizedMessage))
            }
    }

    private fun postNormalUserToServer() {
        val currentUser = mFAuth.currentUser!!
        mFDb.reference.child("Profiles")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.hasChild(currentUser.uid)) {
                        mFDb.reference.child("Profiles").child(currentUser.uid).setValue(
                            User(
                                "18",
                                "0.0",
                                model = false,
                                verified = true,
                                name = currentUser.displayName ?: "",
                                profilePic = currentUser.photoUrl.toString(),
                                null
                            )
                        ).addOnCompleteListener {
                            if (it.isSuccessful) {
                                _loginTask.postValue(NetworkResult.Success())
                                _postUserToServerTask.postValue(NetworkResult.Success())
                            } else
                                _postUserToServerTask.postValue((NetworkResult.Error(it.exception!!.localizedMessage)))
                        }
                    } else
                        _loginTask.postValue(NetworkResult.Success())
                }

                override fun onCancelled(error: DatabaseError) {
                    _postUserToServerTask.postValue(NetworkResult.Error(error.message))
                }
            })
    }

    private fun postModelUserToServer(
        name: String,
        age: String,
        profileUrl: String,
        videoUrl: String
    ) {
        val currentUser = mFAuth.currentUser!!
        mFDb.reference.child("Profiles")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.hasChild(currentUser.uid)) {
                        mFDb.reference.child("Profiles").child(currentUser.uid).setValue(
                            User(
                                age,
                                "0.0",
                                model = true,
                                verified = false,
                                name = name,
                                profilePic = profileUrl,
                                selfieVideoUrl = videoUrl
                            )
                        )
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    _loginTask.postValue(NetworkResult.Success())
                                    _postUserToServerTask.postValue(NetworkResult.Success())
                                } else
                                    _postUserToServerTask.postValue(NetworkResult.Error(it.exception!!.localizedMessage))
                            }
                    } else {
                        deletePreviousImage(profileUrl)
                        deletePreviousVideo(videoUrl)
                        _loginTask.postValue(NetworkResult.Success())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _postUserToServerTask.postValue(NetworkResult.Error(error.message))
                }

            })
    }

    fun logOut() {
        if (mFAuth.currentUser != null) {
            mFAuth.signOut()
        }
        _loginTask.postValue(NetworkResult.Error())
        _loggedIn.postValue(false)
    }

    private fun getRandom(): String {
        return UUID.randomUUID().toString()
    }

    fun setLobbyType(lobbyType: String) {
        currentLobbyType = lobbyType
        Log.d("getNextRoom", "lobbyType set: $currentLobbyType")
    }

    fun createRoom(myUid: String) {
        val orderFilter = if (user.value!!.model) {
            "notJoined,1"
        } else {
            "notJoined,0"
        }
        val roomRef = mFDb.reference.child("Rooms").child(currentLobbyType)
            .child(myUid)
        roomRef.setValue(
            Room(
                Available = true,
                Joined = false,
                Host = myUid,
                Joiner = "",
                OrderFilter = orderFilter
            )
        )
            .addOnSuccessListener {
                roomRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.child("joined").exists()) {
                            if (snapshot.child("joined").value == true) {
                                roomRef.removeEventListener(this)
                                _roomNotExist.postValue(false)
                                _currentRoom.postValue(
                                    Room(
                                        Available = false,
                                        Joined = true,
                                        Host = myUid,
                                        Joiner = snapshot.child("joiner").value.toString(),
                                        OrderFilter = orderFilter
                                    )
                                )
                                _findingRoomTask.postValue(NetworkResult.Success())
                                roomRef.addValueEventListener(
                                    object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            if (!snapshot.exists()) {
                                                _roomNotExist.postValue(true)
                                                roomRef.removeEventListener(this)
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            _roomNotExist.postValue(true)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        _findingRoomTask.postValue(NetworkResult.Error())
                    }
                })
            }
    }

    fun setChannelName(channel: String, room: Room) {
        mFDb.reference.child("Rooms").child(currentLobbyType)
            .child(room.Host)
            .child("channelName")
            .setValue(channel)
    }

    fun getChannelName(room: Room) {
        val ref = mFDb.reference.child("Rooms").child(currentLobbyType)
            .child(room.Host)
            .child("channelName")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    _channelName.postValue(snapshot.value.toString())
                    ref.removeEventListener(this)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _channelName.postValue(error)
            }

        })
    }

    @SuppressLint("SimpleDateFormat")
    fun withdrawBalance(
        amount: Int,
        accountName: String,
        accountNumber: Long,
        ifscCode: String,
        prevRequestId: String?
    ) {
        _transactionTask.postValue(NetworkResult.Loading())
        val myUid = mFAuth.currentUser!!.uid
        val requestId = prevRequestId ?: getRandom()
        mFDb.reference.child("WithdrawRequests")
            .child(myUid)
            .child(requestId)
            .setValue(
                WithdrawRequest(
                    requestId = requestId,
                    uid = myUid,
                    status = "Processing",
                    isCompleted = false,
                    acName = accountName,
                    acNumber = accountNumber,
                    amount = amount,
                    ifsc = ifscCode,
                    time = SimpleDateFormat("hh:mma dd MMM,yyyy").format(Timestamp.now().toDate())
                )
            ).addOnSuccessListener {
                _transactionTask.postValue(NetworkResult.Success(massage = "Request placed!"))
            }.addOnFailureListener {
                _transactionTask.postValue(NetworkResult.Error(massage = it.localizedMessage))
            }
    }

    fun resetTransactionTask() {
        _transactionTask.postValue(NetworkResult.Error())
    }

    fun resetChannelName() {
        _channelName.postValue(null)
    }

    fun resetInitTask() {
        _paymentInitTask.postValue(NetworkResult.Error())
    }

    fun getNextRoom(query: Query, myUid: String) {
        _findingRoomTask.postValue(NetworkResult.Loading())
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.childrenCount > 0) {
                    for (postSnap in snapshot.children) {
                        mFDb.reference.child("Rooms").child(currentLobbyType)
                            .child(postSnap.key.toString())
                            .child("orderFilter").setValue("joined")
                        mFDb.reference.child("Rooms").child(currentLobbyType)
                            .child(postSnap.key.toString())
                            .child("available").setValue(false)
                        mFDb.reference.child("Rooms").child(currentLobbyType)
                            .child(postSnap.key.toString())
                            .child("joiner").setValue(myUid)
                        mFDb.reference.child("Rooms").child(currentLobbyType)
                            .child(postSnap.key.toString())
                            .child("joined").setValue(true)
                        _currentRoom.postValue(postSnap.getValue(Room::class.java))
                        _roomNotExist.postValue(false)
                        postSnap.ref.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (!snapshot.exists()) {
                                    _roomNotExist.postValue(true)
                                    postSnap.ref.removeEventListener(this)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                _roomNotExist.postValue(true)
                            }
                        })
                    }
                    _findingRoomTask.postValue(NetworkResult.Success())
                } else {
                    createRoom(myUid)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _findingRoomTask.postValue(NetworkResult.Error(error.message))
            }
        })
    }

    fun setFindingRoomTaskNull() {
        _findingRoomTask.postValue(NetworkResult.Error())
    }

    fun setCurrentRoomNull() {
        _currentRoom.postValue(null)
    }

    fun resetAgoraToken() {
        _agoraToken.postValue(null)
    }

    fun removeCurrentRoom(roomID: String) {
        mFDb.reference.child("Rooms").child(currentLobbyType).child(roomID)
            .removeValue()
    }

    fun setRoomExistNull() {
        _roomNotExist.postValue(null)
    }

    fun updateProfile(newImage: ByteArray?, newName: String, newAge: String, myUid: String) {
        _updateProfileTask.postValue(NetworkResult.Loading())
        if (newImage != null) {
            deletePreviousImage(user.value!!.profilePic)
            mFStorage.reference.child("ProfilePictures/${getRandom()}")
                .putBytes(newImage).addOnSuccessListener { task ->
                    task.storage.downloadUrl.addOnSuccessListener {
                        updateNameAndAge(newName, newAge, myUid, it.toString())
                    }
                }.addOnFailureListener {
                    _updateProfileTask.postValue(NetworkResult.Error(it.localizedMessage))
                }
        } else
            updateNameAndAge(newName, newAge, myUid, null)
    }

    private fun updateNameAndAge(newName: String, newAge: String, myUid: String, url: String?) {
        mFDb.reference.child("Profiles").child(myUid).child("name")
            .setValue(newName)
        mFDb.reference.child("Profiles").child(myUid).child("age")
            .setValue(newAge)
        url?.let {
            mFDb.reference.child("Profiles").child(myUid).child("profilePic")
                .setValue(it)
        }
        _updateProfileTask.postValue(NetworkResult.Success())
    }

    fun setUpdateTaskNull() {
        _updateProfileTask.postValue(NetworkResult.Error())
    }

    fun getAgoraToken(channel: String, uid: Int) {
        createAgoraToken(channel, uid).addOnCompleteListener {
            if (it.isSuccessful) {
                _agoraToken.postValue(it.result)
            } else {
                _agoraToken.postValue(it.exception)
            }
        }
    }

    private fun createAgoraToken(channel: String, uid: Int): Task<String> {
        val data = hashMapOf(
            "channel" to channel,
            "uid" to uid,
            "push" to true
        )
        return mFunctions
            .getHttpsCallable("getAgoraToken")
            .call(data)
            .continueWith { task ->
                val result = task.result?.data as String
                result
            }
    }

    fun initiatePayment(co: Checkout, amount: Int) {
        _paymentInitTask.postValue(NetworkResult.Loading())
        createOrderID(amount).addOnCompleteListener {
            if (it.isSuccessful) {
                if (it is Exception) {
                    _paymentInitTask.postValue(NetworkResult.Error(massage = it.exception!!.localizedMessage))
                } else {
                    val result = it.result as Map<*, *>
                    co.setKeyID(result["keyID"] as String)
                    val options = JSONObject()
                    options.put("name", "LivePari")
                    options.put("currency", "INR")
                    options.put("order_id", result["OrderID"] as String)
                    options.put("amount", amount.toString())
                    options.put("description","UID: ${mFAuth.currentUser?.uid}")

                    val retryObj = JSONObject()
                    retryObj.put("enabled", true)
                    retryObj.put("max_count", 4)
                    options.put("retry", retryObj)

                    val prefill = JSONObject()
                    prefill.put("email","${mFAuth.currentUser?.uid}@uid.com")
                    prefill.put("contact","919876543210")
                    options.put("prefill",prefill)

                    _paymentInitTask.postValue(NetworkResult.Success(data = options))
                }
            } else {
                _paymentInitTask.postValue(NetworkResult.Error(massage = it.exception!!.localizedMessage))
            }
        }
    }

    private fun createOrderID(amount: Int): Task<Any> {
        val data = hashMapOf(
            "text" to amount,
            "push" to true
        )

        return mFunctions
            .getHttpsCallable("getRazorpayOrderId")
            .call(data)
            .continueWith { task ->
                try {
                    val gson = GsonBuilder().create()
                    val result = task.result?.data as Map<*, *>
                    val keyId = result["keyId"] as String
                    val jsonOrder = result["order"] as Map<*, *>
                    val jsonString = JSONObject(jsonOrder).toString()
                    val order: RazorpayOrder = gson.fromJson(jsonString, RazorpayOrder::class.java)
                    mapOf("keyID" to keyId, "OrderID" to order.id!!)
                } catch (e: Exception) {
                    e.localizedMessage
                }
            }
    }

    fun resetBalance(amount: Int) {
        _resetBalanceTask.postValue(NetworkResult.Loading())
        mFDb.reference.child("Profiles")
            .child(mFAuth.currentUser!!.uid)
            .child("balance")
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    mFDb.reference.child("Profiles")
                        .child(mFAuth.currentUser!!.uid)
                        .child("balance")
                        .setValue("%.1f".format((task.result.value.toString().toFloat() + amount)))
                        .addOnSuccessListener {
                            _resetBalanceTask.postValue(NetworkResult.Success())
                        }.addOnFailureListener {
                            _resetBalanceTask.postValue(NetworkResult.Error(it.localizedMessage))
                        }
                } else {
                    _resetBalanceTask.postValue(NetworkResult.Error(task.exception!!.localizedMessage))
                }
            }
    }

    fun resetTask() {
        _resetBalanceTask.postValue(NetworkResult.Error())
    }

    fun chargeUser() {
        mFDb.reference.child("Profiles")
            .child(mFAuth.currentUser!!.uid)
            .child("balance")
            .get().addOnSuccessListener {
                mFDb.reference.child("Profiles")
                    .child(mFAuth.currentUser!!.uid)
                    .child("balance")
                    .setValue("%.1f".format((it.value.toString().toFloat() - royalLobbyCallsPrice.value!!.toFloat())))
            }
    }

    fun chargeModel() {
        mFDb.reference.child("Profiles")
            .child(mFAuth.currentUser!!.uid)
            .child("balance")
            .get().addOnSuccessListener {
                mFDb.reference.child("Profiles")
                    .child(mFAuth.currentUser!!.uid)
                    .child("balance")
                    .setValue("%.1f".format((it.value.toString().toFloat() + royalLobbyCallsPrice.value!!.toFloat())))
            }
    }

    fun postVideoToServer(videoUri: Uri) {
        _videoUploadTask.postValue(NetworkResult.Loading())
        mFStorage.reference.child("SelfieVideos/${getRandom()}")
            .putFile(videoUri).addOnSuccessListener { task ->
                task.storage.downloadUrl.addOnSuccessListener {
                    _videoUploadTask.postValue(NetworkResult.Success(data = it.toString()))
                }
            }.addOnFailureListener {
                _videoUploadTask.postValue(NetworkResult.Error(it.localizedMessage))
            }
    }

    fun deletePreviousVideo(videoUrl: String) {
        try {
            mFStorage.getReferenceFromUrl(videoUrl).delete()
        } catch (e: Exception) {

        }
    }


    fun postImageToStorage(data: ByteArray) {
        _imageUploadTask.postValue(NetworkResult.Loading())
        mFStorage.reference.child("ProfilePictures/${getRandom()}")
            .putBytes(data).addOnSuccessListener { task ->
                task.storage.downloadUrl.addOnSuccessListener {
                    _imageUploadTask.postValue(NetworkResult.Success(data = it.toString()))
                }
            }.addOnFailureListener {
                _imageUploadTask.postValue(NetworkResult.Error(it.localizedMessage))
            }
    }

    fun deletePreviousImage(imageUrl: String) {
        try {
            mFStorage.getReferenceFromUrl(imageUrl).delete()
        } catch (e: Exception) {
            
        }
    }

    fun checkWithdrawRequest() {
        mFDb.reference.child("WithdrawRequests").child(mFAuth.currentUser!!.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.childrenCount > 0) {
                        var request: WithdrawRequest? = null
                        snapshot.children.forEach {
                            if (it.child("isCompleted").value != true)
                                request = it.getValue(WithdrawRequest::class.java)!!
                        }
                        _haveWithdrawRequests.postValue(request)
                    } else {
                        _haveWithdrawRequests.postValue(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _haveWithdrawRequests.postValue(null)
                }

            })
    }

    fun removeWithdrawRequest() {
        mFDb.reference.child("WithdrawRequests").child(mFAuth.currentUser!!.uid).removeValue()
    }

}