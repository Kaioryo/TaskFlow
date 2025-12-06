package com.taskflow.app

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("HomeFragment", "✅ onViewCreated started")

        // Initialize network helper
        networkHelper = NetworkHelper(requireContext())

        // Initialize views
        tvQuote = view.findViewById(R.id.tv_quote)
        tvQuoteAuthor = view.findViewById(R.id.tv_quote_author)
        tvScheduleTitle = view.findViewById(R.id.tv_schedule_title)
        tvScheduleDesc = view.findViewById(R.id.tv_schedule_desc)
        tvScheduleLocation = view.findViewById(R.id.tv_schedule_location)

        Log.d("HomeFragment", "✅ All views initialized")

        // Initialize database
        val database = AppDatabase.getDatabase(requireContext())
        repository = TaskRepository(database.taskDao())

        Log.d("HomeFragment", "✅ Database initialized")

        // Set username
        try {
            val tvUsername = view.findViewById<TextView>(R.id.tv_user_name)
            val sharedPref = requireContext().getSharedPreferences("TaskFlowPrefs", Context.MODE_PRIVATE)
            val username = sharedPref.getString("username", "User")
            tvUsername.text = username
            Log.d("HomeFragment", "✅ Username set: $username")
        } catch (e: Exception) {
            Log.e("HomeFragment", "❌ tv_user_name not found: ${e.message}")
        }

        // Fetch quote
        fetchMotivationalQuote()

        // Load schedule from database
        loadScheduleFromDatabase()
    }

    private fun fetchMotivationalQuote() {
        if (networkHelper.isNetworkAvailable()) {
            tvQuote.text = "Loading fresh quote..."
        } else {
            tvQuote.text = "Loading from cache..."
        }
        tvQuoteAuthor.text = ""

        RetrofitClient.quoteApiService.getRandomQuote().enqueue(object : Callback<List<QuoteResponse>> {
            override fun onResponse(
                call: Call<List<QuoteResponse>>,
                response: Response<List<QuoteResponse>>
            ) {
                if (response.isSuccessful && response.body() != null && response.body()!!.isNotEmpty()) {
                    val quote = response.body()!![0]
                    tvQuote.text = "\"${quote.quote}\""

                    // ✅ Hanya show 💾 jika benar-benar offline
                    val cacheInfo = if (!networkHelper.isNetworkAvailable()) {
                        " 💾" // Offline mode
                    } else {
                        "" // Online - fresh quote
                    }

                    tvQuoteAuthor.text = "- ${quote.author}$cacheInfo"

                    val source = if (cacheInfo.isNotEmpty()) "from cache (offline)" else "fresh from network"
                    Log.d("HomeFragment", "✅ Quote loaded $source: ${quote.quote}")
                } else {
                    showFallbackQuote()
                }
            }

            override fun onFailure(call: Call<List<QuoteResponse>>, t: Throwable) {
                showFallbackQuote()
                Log.e("HomeFragment", "❌ Quote API failed: ${t.message}")
            }
        })
    }

    private fun showFallbackQuote() {
        tvQuote.text = "\"The only way to do great work is to love what you do.\""
        tvQuoteAuthor.text = "- Steve Jobs 📴"
    }


    private fun loadScheduleFromDatabase() {
        Log.d("HomeFragment", "📊 Loading schedule from database...")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.incompleteTasks.collect { tasks ->
                    Log.d("HomeFragment", "📊 Tasks received: ${tasks.size} items")

                    if (tasks.isNotEmpty()) {
                        // Get first task (most urgent)
                        val nextTask = tasks.first()

                        Log.d("HomeFragment", "📌 Next task: ${nextTask.title}")

                        // Update UI dengan data dari database
                        tvScheduleTitle.text = nextTask.title
                        tvScheduleDesc.text = nextTask.description
                        tvScheduleLocation.text = "${nextTask.location} | ${nextTask.dueTime}"

                        Log.d("HomeFragment", "✅ Schedule updated successfully!")

                    } else {
                        Log.d("HomeFragment", "⚠️ No tasks found")

                        // Set default text if no tasks
                        tvScheduleTitle.text = "No upcoming tasks"
                        tvScheduleDesc.text = "Add a task to see it here"
                        tvScheduleLocation.text = "---"
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "❌ Error loading schedule: ${e.message}", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("HomeFragment", "🔄 onResume - reloading schedule")
        loadScheduleFromDatabase()
    }
}
