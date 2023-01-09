package com.blogspot.bunnylists.maate.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class RazorpayOrder (
  @SerializedName("id"          ) var id         : String?           = null,
  @SerializedName("entity"      ) var entity     : String?           = null,
  @SerializedName("amount"      ) var amount     : Int?              = null,
  @SerializedName("amount_paid" ) var amountPaid : Int?              = null,
  @SerializedName("amount_due"  ) var amountDue  : Int?              = null,
  @SerializedName("currency"    ) var currency   : String?           = null,
  @SerializedName("receipt"     ) var receipt    : String?           = null,
  @SerializedName("status"      ) var status     : String?           = null,
  @SerializedName("attempts"    ) var attempts   : Int?              = null,
  @SerializedName("notes"       ) var notes      : ArrayList<String> = arrayListOf(),
  @SerializedName("created_at"  ) var createdAt  : Int?              = null
)