package com.akshansh.aktv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var heroTitle: TextView
    private lateinit var heroDescription: TextView
    private lateinit var movieGrid: RecyclerView
    private lateinit var channelGrid: RecyclerView
    private lateinit var search: EditText
    private var allMovies = emptyList<Movie>()
    private var allChannels = emptyList<Channel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        heroTitle = findViewById(R.id.heroTitle)
        heroDescription = findViewById(R.id.heroDescription)
        movieGrid = findViewById(R.id.grid)
        channelGrid = findViewById(R.id.channelGrid)
        search = findViewById(R.id.search)

        val moviesTab: Button = findViewById(R.id.moviesTab)
        val liveTab: Button = findViewById(R.id.liveTab)
        val searchButton: Button = findViewById(R.id.searchButton)

        movieAdapter = MovieAdapter { movie -> play(movie) }
        movieGrid.layoutManager = GridLayoutManager(this, 6)
        movieGrid.adapter = movieAdapter

        channelAdapter = ChannelAdapter { channel -> playChannel(channel) }
        channelGrid.layoutManager = GridLayoutManager(this, 6)
        channelGrid.adapter = channelAdapter

        moviesTab.setOnClickListener { showMovies() }
        liveTab.setOnClickListener { showLiveTv() }
        searchButton.setOnClickListener {
            if (channelGrid.visibility == View.VISIBLE) loadLiveTv(search.text.toString())
            else loadMovies(search.text.toString())
        }
        search.setOnEditorActionListener { _, _, _ ->
            if (channelGrid.visibility == View.VISIBLE) loadLiveTv(search.text.toString())
            else loadMovies(search.text.toString())
            true
        }

        showMovies()
    }

    private fun showMovies() {
        movieGrid.visibility = View.VISIBLE
        channelGrid.visibility = View.GONE
        search.hint = "Search movies"
        loadMovies("")
    }

    private fun showLiveTv() {
        movieGrid.visibility = View.GONE
        channelGrid.visibility = View.VISIBLE
        search.hint = "Search live channels"
        loadLiveTv("")
    }

    private fun loadMovies(term: String) {
        status.text = if (term.isBlank()) "Loading free licensed movies…" else "Searching movies…"
        lifecycleScope.launch {
            try {
                allMovies = ArchiveApi.search(term)
                movieAdapter.submit(allMovies)
                heroTitle.text = allMovies.firstOrNull()?.title ?: "AK TV"
                heroDescription.text = allMovies.firstOrNull()?.description
                    ?: "Free and licensed movies"
                status.text = "${allMovies.size} movie titles available"
            } catch (e: Exception) {
                status.text = "Movie catalog error: ${e.message ?: "unknown error"}"
            }
        }
    }

    private fun loadLiveTv(term: String) {
        status.text = if (term.isBlank()) "Loading Live TV…" else "Searching channels…"
        lifecycleScope.launch {
            try {
                allChannels = LiveTvApi.search(term)
                channelAdapter.submit(allChannels)
                heroTitle.text = "Live TV"
                heroDescription.text = "Live channels from your TG TV API"
                status.text = "${allChannels.size} channels available"
            } catch (e: Exception) {
                status.text = "Live TV error: ${e.message ?: "unknown error"}"
            }
        }
    }

    private fun play(movie: Movie) {
        lifecycleScope.launch {
            status.text = "Preparing ${movie.title}…"
            try {
                val resolved = ArchiveApi.resolvePlayableUrl(movie)
                if (resolved.videoUrl.isNullOrBlank()) {
                    status.text = "No playable video file was found."
                } else {
                    startActivity(Intent(this@MainActivity, PlayerActivity::class.java).apply {
                        putExtra("title", resolved.title)
                        putExtra("url", resolved.videoUrl)
                        putExtra("movieId", resolved.id)
                    })
                }
            } catch (e: Exception) {
                status.text = "Playback lookup failed: ${e.message ?: "unknown error"}"
            }
        }
    }

    private fun playChannel(channel: Channel) {
        val url = channel.streamUrl
        if (url.isNullOrBlank()) {
            Toast.makeText(this, "No playable stream URL was returned for this channel.", Toast.LENGTH_LONG).show()
            return
        }
        if (url.contains("/embed/", ignoreCase = true)) {
            startActivity(Intent(this, WebPlayerActivity::class.java).apply {
                putExtra("title", channel.name)
                putExtra("url", url)
            })
        } else {
            startActivity(Intent(this, PlayerActivity::class.java).apply {
                putExtra("title", channel.name)
                putExtra("url", url)
                putExtra("movieId", "live-${channel.id}")
            })
        }
    }
}
