package com.taskflow.app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private lateinit var tvQuote: TextView
    private lateinit var tvQuoteAuthor: TextView
    private lateinit var tvScheduleTitle: TextView
    private lateinit var tvScheduleDesc: TextView
    private lateinit var tvScheduleLocation: TextView
    private lateinit var repository: TaskRepository
    private lateinit var networkHelper: NetworkHelper

    // ✅ Track if this is first load
    private var isFirstLoad = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        networkHelper = NetworkHelper(requireContext())

        tvQuote = view.findViewById(R.id.tv_quote)
        tvQuoteAuthor = view.findViewById(R.id.tv_quote_author)
        tvScheduleTitle = view.findViewById(R.id.tv_schedule_title)
        tvScheduleDesc = view.findViewById(R.id.tv_schedule_desc)
        tvScheduleLocation = view.findViewById(R.id.tv_schedule_location)

        val database = AppDatabase.getDatabase(requireContext())
        repository = TaskRepository(database.taskDao())

        try {
            val tvUsername = view.findViewById<TextView>(R.id.tv_user_name)
            val sharedPref = requireContext().getSharedPreferences("TaskFlowPrefs", Context.MODE_PRIVATE)
            val username = sharedPref.getString("username", "User")
            tvUsername.text = username
        } catch (e: Exception) {
            // Ignore
        }

        // ✅ Show loading immediately
        showLoadingState()

        // ✅ DON'T fetch here - let onResume handle it
        loadScheduleFromDatabase()
    }

    private fun fetchQuote() {
        // Prevent multiple simultaneous requests
        if (QuoteManager.isRequestInProgress()) {
            return
        }

        // Check if we should fetch from API or use cache
        if (!networkHelper.isNetworkAvailable() || !QuoteManager.canMakeAPICall()) {
            showCachedQuote()
            return
        }

        // Show loading state
        showLoadingState()

        // Fetch fresh quote from API
        QuoteManager.markAPICallStarted()

        RetrofitClient.quoteApiService.getRandomQuote().enqueue(object : Callback<List<QuoteResponse>> {
            override fun onResponse(call: Call<List<QuoteResponse>>, response: Response<List<QuoteResponse>>) {
                QuoteManager.markAPICallFinished()

                if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
                    val quote = response.body()!![0]

                    QuoteManager.addQuote(quote)
                    QuoteManager.incrementAPICount()

                    displayQuote(quote)
                } else {
                    showCachedQuote()
                }
            }

            override fun onFailure(call: Call<List<QuoteResponse>>, t: Throwable) {
                QuoteManager.markAPICallFinished()
                showCachedQuote()
            }
        })
    }

    private fun showLoadingState() {
        tvQuote.text = "Loading inspiring quote..."
        tvQuoteAuthor.text = ""
    }

    private fun showCachedQuote() {
        val quote = QuoteManager.getNextCachedQuote()

        if (quote == null) {
            val defaultQuote = QuoteResponse(
                quote = "The only way to do great work is to love what you do.",
                author = "Steve Jobs"
            )
            displayQuote(defaultQuote)
        } else {
            displayQuote(quote)
        }
    }

    private fun displayQuote(quote: QuoteResponse) {
        tvQuote.text = "\"${quote.quote}\""
        tvQuoteAuthor.text = "- ${quote.author}"
    }

    private fun loadScheduleFromDatabase() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.incompleteTasks.collect { tasks ->
                    if (tasks.isNotEmpty()) {
                        val nextTask = tasks.first()
                        tvScheduleTitle.text = nextTask.title
                        tvScheduleDesc.text = nextTask.description
                        tvScheduleLocation.text = "${nextTask.location} | ${nextTask.dueTime}"
                    } else {
                        tvScheduleTitle.text = "No upcoming tasks"
                        tvScheduleDesc.text = "Add a task to see it here"
                        tvScheduleLocation.text = "---"
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override fun onResume() {
        super.onResume()

        viewLifecycleOwner.lifecycleScope.launch {
            // ✅ Only show loading if we're going to fetch new quote
            if (!isFirstLoad && QuoteManager.canMakeAPICall() && networkHelper.isNetworkAvailable()) {
                showLoadingState()
            }

            // Small delay for smooth transition
            if (isFirstLoad) {
                delay(500) // Longer delay on first load
                isFirstLoad = false
            } else {
                delay(300) // Shorter delay on refresh
            }

            fetchQuote()
        }

        loadScheduleFromDatabase()
    }
}
