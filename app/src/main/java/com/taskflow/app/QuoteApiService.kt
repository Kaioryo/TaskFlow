package com.taskflow.app

import retrofit2.Call
import retrofit2.http.GET

interface QuoteApiService {

    // ZenQuotes returns array, we take first item [0]
    @GET("random")
    fun getRandomQuote(): Call<List<QuoteResponse>>
}
