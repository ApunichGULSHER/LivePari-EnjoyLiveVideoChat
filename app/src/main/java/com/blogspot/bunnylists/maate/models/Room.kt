package com.blogspot.bunnylists.maate.models

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName

@Keep
data class Room(
    @PropertyName("available")
    val Available: Boolean,
    @PropertyName("joined")
    val Joined: Boolean,
    @PropertyName("host")
    val Host: String,
    @PropertyName("joiner")
    val Joiner: String,
    @PropertyName("orderFilter")
    val OrderFilter: String
){
    constructor() : this(false, false, "","","")
}
