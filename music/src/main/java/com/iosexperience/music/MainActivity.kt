package com.iosexperience.music

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private val bg = Color.rgb(12, 12, 16)
    private val card = Color.rgb( thirty(), 30, 38)
    private val white = Color.WHITE
    private val muted = Color.rgb(170, 170, 180)
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var miniTitle: TextView
    private lateinit var playButton: TextView
    private var player: MediaPlayer? = null
    private var currentTitle = "Nothing playing"
    private var playing = false
    private val prefs by lazy { getSharedPreferences("sonic_music", MODE_PRIVATE) }
    private val favorites get() = prefs.getStringSet("favorites", emptySet())!!.toMutableSet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildShell()
        showHome()
    }

    private fun thirty() = 30

    private fun buildShell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(20, 22, 20, 8)
        }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        header.addView(label("SONIC", 25, white, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(label("♪  MUSIC", 12, Color.rgb(255, 95, 105), true))
        root.addView(header)
        root.addView(label("Your sound. Your space.", 13, muted, false).apply {
            setPadding(0, 4, 0, 14)
        })
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(content) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val mini = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 10, 12, 10)
            background = rounded(card, 18)
        }
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        miniTitle = label(currentTitle, 14, white, true)
        info.addView(miniTitle)
        info.addView(label("SONIC PLAYER", 10, muted, false))
        mini.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        playButton = label("▶", 22, white, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { togglePlayback() }
        }
        mini.addView(playButton, LinearLayout.LayoutParams(44, 42))
        root.addView(mini, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 10 })
        val nav = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 8)
        }
        listOf("Home", "Search", "Library").forEach { item ->
            val b = label(item, 13, muted, true).apply {
                gravity = Gravity.CENTER
                setOnClickListener {
                    when (item) { "Home" -> showHome(); "Search" -> showSearch(); else -> showLibrary() }
                }
            }
            nav.addView(b, LinearLayout.LayoutParams(0, 44, 1f))
        }
        root.addView(nav)
        setContentView(root)
    }

    private fun showHome() {
        content.removeAllViews()
        section("Listen now")
        hero("A world of music", "Find songs on YouTube or play audio stored on your phone.")
        action("Search YouTube", "Discover official music videos and tracks") { showSearch() }
        action("Play music on this device", "Choose MP3, AAC, FLAC and other audio files") {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "audio/*" }, 401)
        }
        section("Quick picks")
        listOf("Chill mix", "Today's energy", "Focus flow", "Late night").forEachIndexed { i, name ->
            action(name, listOf("Relaxed listening", "Upbeat discoveries", "Instrumental mood", "Slow sounds")[i]) {
                youtube(name)
            }
        }
        section("Your library")
        action("Favorites", "Your saved tracks") { showLibrary() }
    }

    private fun showSearch() {
        content.removeAllViews()
        section("Search music")
        val input = EditText(this).apply {
            hint = "Songs, artists, albums…"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 16f
            singleLine = true
            setPadding(16, 14, 16, 14)
            background = rounded(card, 16)
        }
        content.addView(input, LinearLayout.LayoutParams(-1, 54))
        val search = label("Search YouTube  ↗", 15, white, true).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(225, 55, 75), 16)
            setOnClickListener {
                val q = input.text.toString().trim()
                if (q.isNotEmpty()) youtube(q)
            }
        }
        content.addView(search, LinearLayout.LayoutParams(-1, 52).apply { topMargin = 12 })
        section("Browse")
        listOf("Trending songs", "New music", "Bollywood hits", "Hindi songs", "Kashmiri music", "English hits", "Lo-fi", "Workout music").forEach {
            action(it, "Search on YouTube") { youtube(it) }
        }
        content.addView(label("YouTube results open through the official YouTube app or website. Playback, background audio and account features follow YouTube's rules and availability.", 12, muted, false).apply {
            setPadding(4, 14, 4, 20)
        })
    }

    private fun showLibrary() {
        content.removeAllViews()
        section("Your library")
        action("Choose audio from device", "Play music files stored on your phone") {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "audio/*" }, 401)
        }
        section("Favorites")
        val saved = favorites
        if (saved.isEmpty()) content.addView(label("Your saved tracks will appear here.", 13, muted, false).apply { setPadding(4, 12, 4, 18) })
        saved.forEach { item -> action(item, "Search this track on YouTube") { youtube(item) } }
    }

    private fun section(title: String) {
        content.addView(label(title, 21, white, true).apply { setPadding(0, 22, 0, 12) })
    }

    private fun hero(title: String, subtitle: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 22, 20, 22)
            background = rounded(Color.rgb(83, 29, 65), 24)
        }
        box.addView(label("SONIC  •  VERSION 40.0", 11, Color.rgb(255, 205, 220), true))
        box.addView(label(title, 27, white, true).apply { setPadding(0, 12, 0, 6) })
        box.addView(label(subtitle, 13, Color.rgb(245, 220, 232), false))
        content.addView(box)
    }

    private fun action(title: String, subtitle: String, click: () -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            background = rounded(card, 17)
            isClickable = true
            isFocusable = true
            setOnClickListener { click() }
        }
        row.addView(label(title, 15, white, true))
        row.addView(label(subtitle, 12, muted, false).apply { setPadding(0, 4, 0, 0) })
        content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 9 })
        row.setOnLongClickListener {
            favorites.let { set ->
                if (set.contains(title)) set.remove(title) else set.add(title)
                prefs.edit().putStringSet("favorites", set).apply()
            }
            Toast.makeText(this, "Favorite updated", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun youtube(query: String) {
        val url = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))
        val appIntent = Intent(Intent.ACTION_VIEW, url).setPackage("com.google.android.youtube")
        try { startActivity(appIntent) }
        catch (_: Exception) { startActivity(Intent(Intent.ACTION_VIEW, url)) }
    }

    private fun playLocal(uri: Uri, title: String) {
        try {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(this@MainActivity, uri)
                setOnPreparedListener {
                    it.start()
                    playing = true
                    currentTitle = title
                    refreshMini()
                }
                setOnCompletionListener { playing = false; refreshMini() }
                prepareAsync()
            }
            currentTitle = title
            miniTitle.text = "Loading: $title"
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot play this audio file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun togglePlayback() {
        val p = player ?: return
        try {
            if (p.isPlaying) { p.pause(); playing = false }
            else { p.start(); playing = true }
            refreshMini()
        } catch (_: Exception) {}
    }

    private fun refreshMini() {
        miniTitle.text = currentTitle
        playButton.text = if (playing) "Ⅱ" else "▶"
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean) =
        TextView(this).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun rounded(color: Int, radius: Int) =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 401 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            playLocal(uri, uri.lastPathSegment?.substringAfterLast('/') ?: "Local audio")
        }
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
