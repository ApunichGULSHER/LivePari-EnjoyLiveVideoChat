package com.blogspot.bunnylists.maate.models

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName

@Keep
data class WithdrawRequest(
    @PropertyName("requestId")
    val requestId: String?,
    @PropertyName("uid")
    val uid: String?,
    @PropertyName("status")
    val status: String?,
    @PropertyName("isCompleted")
    val isCompleted: Boolean,
    @PropertyName("acName")
    val acName: String?,
    @PropertyName("acNumber")
    val acNumber: Long?,
    @PropertyName("amount")
    val amount: Int?,
    @PropertyName("ifsc")
    val ifsc: String?,
    @PropertyName("time")
    val time: String?
){
    constructor() : this("","","",true,"",0,0,"","")
}
