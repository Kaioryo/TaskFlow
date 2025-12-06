package com.taskflow.app

object QuoteManager {
    private val quoteCache = mutableListOf<QuoteResponse>()
    private var currentIndex = 0
    private var apiCallCount = 0
    private var apiWindowStart: Long = 0
    private var lastFetchTime: Long = 0
    private var isRequesting = false

    private const val MAX_API_CALLS = 5
    private const val MIN_FETCH_INTERVAL = 2000L // 2 seconds

    fun addQuote(quote: QuoteResponse) {
        if (!quoteCache.any { it.quote == quote.quote && it.author == quote.author }) {
            quoteCache.add(quote)
        }
    }

    fun getNextCachedQuote(): QuoteResponse? {
        if (quoteCache.isEmpty()) return null

        val quote = quoteCache[currentIndex]
        currentIndex = (currentIndex + 1) % quoteCache.size
        return quote
    }

    fun canMakeAPICall(): Boolean {
        val now = System.currentTimeMillis()

        // Check rate limit
        if ((now - lastFetchTime) < MIN_FETCH_INTERVAL) {
            return false
        }

        // Check API window
        if (apiCallCount >= MAX_API_CALLS) {
            val elapsed = now - apiWindowStart
            if (elapsed >= 30000) {
                // Reset window
                apiCallCount = 0
                apiWindowStart = now
            } else {
                return false
            }
        }

        return true
    }

    fun markAPICallStarted() {
        val now = System.currentTimeMillis()
        if (apiCallCount == 0) {
            apiWindowStart = now
        }
        lastFetchTime = now
        isRequesting = true
    }

    fun markAPICallFinished() {
        isRequesting = false
    }

    fun incrementAPICount() {
        apiCallCount++
    }

    fun isRequestInProgress(): Boolean = isRequesting
}
