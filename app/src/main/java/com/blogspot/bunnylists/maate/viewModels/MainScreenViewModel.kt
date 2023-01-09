package com.blogspot.bunnylists.maate.viewModels

import android.os.Bundle
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import com.blogspot.bunnylists.maate.Utils.NetworkResult
import com.blogspot.bunnylists.maate.models.Room
import com.blogspot.bunnylists.maate.models.User
import com.blogspot.bunnylists.maate.models.WithdrawRequest
import com.blogspot.bunnylists.maate.repository.MyRepository
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.razorpay.Checkout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainScreenViewModel(
    private val repository: MyRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    companion object {
        fun provideFactory(
            myRepository: MyRepository,
            owner: SavedStateRegistryOwner,
            defaultArgs: Bundle? = null,
        ): AbstractSavedStateViewModelFactory =
            object : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle,
                ): T {
                    return MainScreenViewModel(myRepository, handle) as T
                }
            }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getCurrentUserData()
        }
    }

    private lateinit var query: Query
    private val _currentFragment = MutableLiveData(1)
    val name = MutableLiveData<String>()
    val age = MutableLiveData<String>()
    val errTxt = MutableLiveData<String>()
    var amount: Int? = null
    var accountName: String? = null
    var accountNumber: Long? = null
    var ifscCode: String? = null

    val transactionTask: LiveData<NetworkResult<String>>
        get() = repository.transactionTask

    val currentLobbyType: String
        get() = repository.currentLobbyType

    val currentFragment: LiveData<Int>
        get() = _currentFragment

    val gettingUserDataTask: LiveData<NetworkResult<String>>
        get() = repository.gettingUserDataTask

    val user: LiveData<User>
        get() = repository.user

    val loggedIn: LiveData<Boolean>
        get() = repository.loggedIn

    val haveWithdrawRequest: LiveData<WithdrawRequest?>
        get() = repository.haveWithdrawRequests

    val channelName: LiveData<Any?>
        get() = repository.channelName

    val royalCallsPrice: LiveData<String>
        get() = repository.royalLobbyCallsPrice

    val paymentInitTask: LiveData<NetworkResult<String>>
        get() = repository.paymentInitTask

    val resetBalanceTask: LiveData<NetworkResult<String>>
        get() = repository.resetBalanceTask

    val findingRoomTask: LiveData<NetworkResult<String>>
        get() = repository.findingRoomTask

    val agoraToken: LiveData<Any?>
        get() = repository.agoraToken

    val currentRoom: LiveData<Room?>
        get() = repository.currentRoom

    val roomNotExist: LiveData<Boolean?>
        get() = repository.roomNotExist

    val updateProfileTask: LiveData<NetworkResult<String>>
        get() = repository.updateProfileTask

    fun logout() {
        repository.logOut()
    }

    fun buildQuery() {
        if(user.value!!.model && currentLobbyType=="Royal"){
            query = FirebaseDatabase.getInstance().reference.child("Rooms").child(currentLobbyType)
                .orderByChild("orderFilter")
                .equalTo("notJoined,0")
                .limitToFirst(1)
        }else if(!user.value!!.model && currentLobbyType=="Royal"){
            query = FirebaseDatabase.getInstance().reference.child("Rooms").child(currentLobbyType)
                .orderByChild("orderFilter")
                .equalTo("notJoined,1")
                .limitToFirst(1)
        }else{
            query = FirebaseDatabase.getInstance().reference.child("Rooms").child(currentLobbyType)
                .orderByChild("joined")
                .equalTo(false)
                .limitToFirst(1)
        }
    }

    fun getNextRoom(myUid: String) {
        repository.getNextRoom(query, myUid)
    }

    fun changeFragment(index: Int) {
        _currentFragment.postValue(index)
    }

    fun setChannelName(channel: String, room: Room) {
        repository.setChannelName(channel, room)
    }

    fun getChannelName(room: Room) {
        repository.getChannelName(room)
    }

    fun withdrawBalance() {
        if (!amount.toString().isDigitsOnly() ||
            amount.toString().toLongOrNull() == null
        )
            return
        else if (amount!! > user.value!!.balance.toFloat())
            return
        else
            repository.withdrawBalance(amount!!, accountName!!, accountNumber!!, ifscCode!!, haveWithdrawRequest.value?.requestId)
    }

    fun resetChannelName() {
        repository.resetChannelName()
    }

    fun resetInitTask() {
        repository.resetInitTask()
    }

    fun setFindingRoomTaskNull() {
        repository.setFindingRoomTaskNull()
    }

    fun setCurrentRoomNull() {
        repository.setCurrentRoomNull()
    }

    fun removeCurrentRoom(roomID: String) {
        repository.removeCurrentRoom(roomID)
    }

    fun setRoomExistNull() {
        repository.setRoomExistNull()
    }

    fun updateProfile(newImage: ByteArray?, newName: String, newAge: String, myUid: String) {
        repository.updateProfile(newImage, newName, newAge, myUid)
    }

    fun setUpdateTaskNull() {
        repository.setUpdateTaskNull()
    }

    fun getAgoraToken(channel: String, uid: Int) {
        repository.getAgoraToken(channel, uid)
    }

    fun initiatePayment(co: Checkout) {
        repository.initiatePayment(co, amount!!)
    }

    fun resetBalance() {
        if (amount.toString().isDigitsOnly() ||
            amount.toString().toFloatOrNull() != null
        )
            repository.resetBalance(amount!!/100)
    }

    fun resetAgoraToken() {
        repository.resetAgoraToken()
    }

    fun resetTransactionTask() {
        repository.resetTransactionTask()
    }

    fun resetTask() {
        repository.resetTask()
    }

    fun chargeUser() {
        repository.chargeUser()
    }

    fun chargeModel() {
        repository.chargeModel()
    }

    fun clearWithdrawDetails() {
        amount=null
        accountNumber=null
        accountName=null
        ifscCode=null
    }

    fun checkWithdrawRequest() {
        repository.checkWithdrawRequest()
    }

    fun removeWithdrawRequest() {
        repository.removeWithdrawRequest()
    }
}