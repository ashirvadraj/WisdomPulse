package com.wisdompulse.app.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Quote(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String? = null,
    @SerializedName("text") val text: String,
    @SerializedName("author") val author: String,
    @SerializedName("authorRole") val authorRole: String,
    @SerializedName("category") val category: String,
    @SerializedName("section") val section: String? = null,
    @SerializedName("language") val language: String = "hi",
    @SerializedName("isPoem") val isPoem: Boolean = true,
    @SerializedName("avatarKey") val avatarKey: String = "avatar_vajpayee",
    @SerializedName("illustrationKey") val illustrationKey: String? = null,
    @SerializedName("englishTranslation") val englishTranslation: String? = null
) : Serializable
