package com.blogspot.bunnylists.maate.viewModels

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import com.blogspot.bunnylists.maate.Utils.NetworkResult
import com.blogspot.bunnylists.maate.repository.MyRepository
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

class RegisterScreenViewModel(
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
                    return RegisterScreenViewModel(myRepository, handle) as T
                }
            }
    }

    private val _modelImage = MutableLiveData<Uri?>(null)
    var modelImageStorageUrl : String? = null
    var modelVideoStorageUrl : String? = null
    val phoneNumber = MutableLiveData<String>()
    val otp = MutableLiveData<String>()
    var verificationID: String? = null
    val name = MutableLiveData<String>()
    val age = MutableLiveData<String>()

    val loginTask: LiveData<NetworkResult<String>>
        get() = repository.loginTask

    val userImage: LiveData<Uri?>
        get() = _modelImage

    val imageUploadTask: LiveData<NetworkResult<String>>
        get() = repository.imageUploadTask

    val videoUploadTask: LiveData<NetworkResult<String>>
        get() = repository.videoUploadTask

    fun signInWithGoogleNormal(idToken: String?) {
        repository.signInWithGoogleNormal(idToken!!)
    }

    fun signInWithPhoneModel(){
        val credential : PhoneAuthCredential = PhoneAuthProvider.getCredential(
            verificationID!!, otp.value.toString()
        )
        repository.signInWithPhoneModel(
            credential,
            name.value.toString(),
            age.value.toString(),
            modelImageStorageUrl!!,
            modelVideoStorageUrl!!
        )
    }

    fun uploadModelImage(ba: ByteArray, data: Uri) {
        _modelImage.postValue(data)
        deletePreviousImage()
        repository.postImageToStorage(ba)
    }

    fun deletePreviousImage(){
        if(modelImageStorageUrl!=null){
            repository.deletePreviousImage(modelImageStorageUrl!!)
        }
    }

    fun signInWithGoogleModel(idToken: String?) {
        repository.signInWithGoogleModel(idToken!!, name.value.toString(), age.value.toString(), modelImageStorageUrl!!, modelVideoStorageUrl!!)
    }

    fun signInWithPhoneNormal(){
        val credential: PhoneAuthCredential = PhoneAuthProvider.getCredential(
            verificationID!!, otp.value.toString())
        repository.signInWithPhoneNormal(credential)
    }

    fun setUserImageNull() {
        _modelImage.postValue(null)
    }

    fun uploadSelfieVideo(uri: Uri) {
        deletePreviousVideo()
        repository.postVideoToServer(uri)
    }

    fun deletePreviousVideo(){
        if(modelVideoStorageUrl!=null){
            repository.deletePreviousVideo(modelVideoStorageUrl!!)
        }
    }
}