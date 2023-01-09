package com.blogspot.bunnylists.maate.models

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName

@Keep
data class User(
    @PropertyName("age")
    val age: String,
    @PropertyName("balance")
    val balance: String,
    @PropertyName("model")
    val model: Boolean,
    @PropertyName("verified")
    val verified: Boolean,
    @PropertyName("name")
    val name: String,
    @PropertyName("profilePic")
    val profilePic: String,
    @PropertyName("selfieVideoUrl")
    val selfieVideoUrl: String?
){
    constructor() : this("","",false,false,"","","")
}
