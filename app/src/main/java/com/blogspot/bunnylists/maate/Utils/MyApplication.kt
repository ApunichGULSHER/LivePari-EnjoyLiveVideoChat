package com.blogspot.bunnylists.maate.Utils

import android.app.Application
import androidx.core.content.ContentProviderCompat.requireContext
import com.blogspot.bunnylists.maate.repository.MyRepository
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage

class MyApplication : Application() {
    lateinit var mFAuth : FirebaseAuth
    lateinit var mFDb : FirebaseDatabase
    lateinit var mFStorage: FirebaseStorage
    lateinit var repository: MyRepository
    lateinit var mFunctions: FirebaseFunctions
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(applicationContext)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        initialize()
    }

    private fun initialize(){
        mFAuth = FirebaseAuth.getInstance()
        mFDb = FirebaseDatabase.getInstance()
        mFStorage = FirebaseStorage.getInstance()
        mFunctions = FirebaseFunctions.getInstance()
        repository = MyRepository(mFAuth, mFDb, mFStorage, mFunctions)
    }
}