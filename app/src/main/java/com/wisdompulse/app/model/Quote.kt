package com.wisdompulse.app.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Quote(
    @SerializedName("id") val id: Int,
    @SerializedName("text") val text: String,
    @SerializedName("author") val author: String,
    @SerializedName("authorRole") val authorRole: String,
    @SerializedName("category") val category: String,
    @SerializedName("language") val language: String = "en",
    @SerializedName("isPoem") val isPoem: Boolean = false
) : Serializable