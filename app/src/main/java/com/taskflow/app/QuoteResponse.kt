package com.taskflow.app

import com.google.gson.annotations.SerializedName

// ZenQuotes response format
data class QuoteResponse(
    @SerializedName("q")  // ZenQuotes uses "q" for quote
    val quote: String,

    @SerializedName("a")  // ZenQuotes uses "a" for author
    val author: String,

    @SerializedName("h")  // HTML version (optional)
    val html: String? = null
)
