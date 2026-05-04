package com.ultrazg.xyztv.data.model

import com.google.gson.annotations.SerializedName

data class SendCodeRequest(
    @SerializedName("mobilePhoneNumber") val mobilePhoneNumber: String,
    @SerializedName("areaCode") val areaCode: String = "+86"
)

data class LoginRequest(
    @SerializedName("mobilePhoneNumber") val mobilePhoneNumber: String,
    @SerializedName("verifyCode") val verifyCode: String,
    @SerializedName("areaCode") val areaCode: String = "+86"
)
