package com.iosexperience.music

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private val bg = Color.rgb(12, 12, 16)
    private val card = Color.rgb(30, 30, 38)
    private val accent = Color.rgb(245, 65, 92)
    private val white = Color.WHITE
    private val muted = Color.rgb(170, 170, 180)
    private lateinit var content: LinearLayout
    private lateinit var miniTitle: TextView
    private lateinit var miniPlay: TextView
    private lateinit var progress: SeekBar
    private lateinit var timeLabel: TextView
    private var player: MediaPlayer? = null
    private val queue = mutableListOf<Track>()
    private var index = -1
    private var currentTitle = "Nothing playing"
    private var playing = false
    private var shuffle = false
    private var repeat = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("sonic_music", MODE_PRIVATE) }
    private data class Track(val uri: String, val title: String)

    private val ticker = object : Runnable {
        override fun run() {
            val p = player
            if (p != null && playing) {
                try {
                    progress.max = p.duration.coerceAtLeast(1)
                    progress.progress = p.currentPosition
                    timeLabel.text = formatTime(p.currentPosition) + " / " + formatTime(p.duration)
                } catch (_: Exception) { }
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        buildShell()
        showHome()
        handler.post(ticker)
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(18, 20, 18, 8)
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(label("SONIC", 26, white, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(label("♪  MUSIC", 12, accent, true))
        root.addView(header)
        root.addView(label("Your sound. Your space.", 13, muted, false).apply { setPadding(0, 3, 0, 14) })
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { isFillViewport = true; addView(content) }, LinearLayout.LayoutParams(-1, 0, 1f))

        val mini = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(13, 9, 8, 9)
            background = rounded(card, 17)
        }
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        miniTitle = label(currentTitle, 14, white, true)
        info.addView(miniTitle)
        info.addView(label("NOW PLAYING", 10, muted, true))
        mini.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        miniPlay = label("▶", 22, white, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { togglePlayback() }
        }
        mini.addView(miniPlay, LinearLayout.LayoutParams(46, 42))
        mini.setOnClickListener { showPlayer() }
        root.addView(mini, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 9 })

        val nav = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(0, 10, 0, 4) }
        listOf("Home", "Search", "Library").forEach { item ->
            nav.addView(label(item, 13, muted, true).apply {
                gravity = Gravity.CENTER
                setOnClickListener { when (item) { "Home" -> showHome(); "Search" -> showSearch(); else -> showLibrary() } }
            }, LinearLayout.LayoutParams(0, 44, 1f))
        }
        root.addView(nav)
        setContentView(root)
    }

    private fun showHome() {
        content.removeAllViews()
        section("Listen now")
        hero("A world of music", "Your music library, playlists and official music discovery in one place.")
        action("Open your music library", "Browse and play audio stored on this phone") { pickAudio() }
        action("Search songs and artists", "Find music through official YouTube search") { showSearch() }
        section("Made for your mood")
        listOf("Chill mix", "Today's energy", "Focus flow", "Late night", "Bollywood hits", "Kashmiri music").forEach {
            action(it, "Discover " + it.lowercase()) { youtube(it) }
        }
        section("Your collection")
        action("Favorites", "Your saved local tracks") { showLibrary() }
        action("Playlists", "Create and manage your playlists") { showPlaylists() }
    }

    private fun showSearch() {
        content.removeAllViews()
        section("Search")
        val input = EditText(this).apply {
            hint = "Songs, artists, albums…"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 16f
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setPadding(16, 12, 16, 12)
            background = rounded(card, 15)
        }
        content.addView(input, LinearLayout.LayoutParams(-1, 54))
        action("Search YouTube  ↗", "Open official search results") {
            val q = input.text.toString().trim()
            if (q.isNotEmpty()) youtube(q) else Toast.makeText(this, "Enter a song or artist", Toast.LENGTH_SHORT).show()
        }
        section("Browse categories")
        listOf("Trending songs", "New music", "Bollywood hits", "Hindi songs", "Kashmiri music", "English hits", "Lo-fi", "Workout music").forEach {
            action(it, "Search on YouTube") { youtube(it) }
        }
        content.addView(label("YouTube opens in its official app or website. This app does not extract streams or bypass YouTube restrictions.", 12, muted, false).apply {
            setPadding(4, 14, 4, 20)
        })
    }

    private fun showLibrary() {
        content.removeAllViews()
        section("Your library")
        action("Add music from this device", "Select one or more audio files") { pickAudio() }
        action("Playlists", "Create, rename and play your collections") { showPlaylists() }
        section("Recently added")
        if (queue.isEmpty()) empty("Choose audio files to build your library.")
        queue.forEachIndexed { i, track ->
            trackRow(track.title, "On this device", "▶") { playAt(i) }
        }
        section("Favorites")
        val fav = favorites()
        if (fav.isEmpty()) empty("Favorite tracks appear here. Tap the ♡ control while playing.")
        fav.forEach { saved ->
            val track = queue.find { it.uri == saved }
            if (track != null) trackRow(track.title, "Favorite", "▶") { playAt(queue.indexOf(track)) }
        }
    }

    private fun showPlaylists() {
        content.removeAllViews()
        section("Playlists")
        action("＋ Create playlist", "Make a collection from your local tracks") { createPlaylist() }
        val lists = readPlaylists()
        if (lists.length() == 0) empty("Your playlists will appear here.")
        for (i in 0 until lists.length()) {
            val name = lists.optJSONObject(i)?.optString("name") ?: continue
            action(name, "Open playlist") { showPlaylist(name) }
        }
    }

    private fun showPlaylist(name: String) {
        content.removeAllViews()
        section(name)
        action("＋ Add current library tracks", "Choose tracks to add") {
            val tracks = queue.map { it.title }.toTypedArray()
            if (tracks.isEmpty()) { pickAudio(); return@action }
            val selected = BooleanArray(tracks.size)
            AlertDialog.Builder(this).setTitle("Add tracks").setMultiChoiceItems(tracks, selected) { _, which, checked -> selected[which] = checked }
                .setPositiveButton("Add") { _, _ ->
                    val ids = (0 until selected.size).filter { selected[it] }.map { queue[it].uri }
                    val all = readPlaylists()
                    for (i in 0 until all.length()) {
                        val obj = all.optJSONObject(i) ?: continue
                        if (obj.optString("name") == name) {
                            val arr = obj.optJSONArray("tracks") ?: JSONArray()
                            ids.forEach { if (!contains(arr, it)) arr.put(it) }
                            obj.put("tracks", arr)
                        }
                    }
                    savePlaylists(all)
                    showPlaylist(name)
                }.setNegativeButton("Cancel", null).show()
        }
        val list = readPlaylists()
        for (i in 0 until list.length()) {
            val obj = list.optJSONObject(i) ?: continue
            if (obj.optString("name") != name) continue
            val ids = obj.optJSONArray("tracks") ?: JSONArray()
            for (j in 0 until ids.length()) {
                val uri = ids.optString(j)
                val track = queue.find { it.uri == uri }
                if (track != null) trackRow(track.title, "In playlist", "▶") { playAt(queue.indexOf(track)) }
            }
        }
        action("Delete playlist", "Remove this playlist from your library") {
            AlertDialog.Builder(this).setMessage("Delete " + name + "?").setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    val all = readPlaylists()
                    val kept = JSONArray()
                    for (i in 0 until all.length()) {
                        val obj = all.optJSONObject(i) ?: continue
                        if (obj.optString("name") != name) kept.put(obj)
                    }
                    savePlaylists(kept); showPlaylists()
                }.show()
        }
    }

    private fun showPlayer() {
        content.removeAllViews()
        section("Now playing")
        val art = TextView(this).apply {
            text = "♫"
            textSize = 88f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(255, 190, 205))
            background = rounded(Color.rgb(74, 27, 57), 28)
        }
        content.addView(art, LinearLayout.LayoutParams(-1, 230).apply { bottomMargin = 20 })
        content.addView(label(currentTitle, 21, white, true).apply { gravity = Gravity.CENTER })
        content.addView(label(if (index >= 0) "ON THIS DEVICE" else "SONIC PLAYER", 11, muted, true).apply {
            gravity = Gravity.CENTER; setPadding(0, 5, 0, 16)
        })
        progress = SeekBar(this).apply {
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            thumbTintList = android.content.res.ColorStateList.valueOf(white)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, value: Int, fromUser: Boolean) { if (fromUser) timeLabel.text = formatTime(value) + " / " + formatTime(s?.max ?: 0) }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) { try { player?.seekTo(s?.progress ?: 0) } catch (_: Exception) {} }
            })
        }
        content.addView(progress)
        timeLabel = label("0:00 / 0:00", 11, muted, false).apply { gravity = Gravity.CENTER }
        content.addView(timeLabel)
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(0, 12, 0, 12) }
        controls.addView(control("⤨", shuffle) { shuffle = !shuffle; showPlayer() })
        controls.addView(control("◀", false) { previous() })
        controls.addView(control(if (playing) "Ⅱ" else "▶", false) { togglePlayback(); showPlayer() })
        controls.addView(control("▶|", false) { next() })
        controls.addView(control("↻", repeat) { repeat = !repeat; showPlayer() })
        content.addView(controls)
        action(if (isFavoriteCurrent()) "♥  Remove from favorites" else "♡  Add to favorites", "Save this track in your library") { toggleFavorite(); showPlayer() }
        action("Queue", "View tracks waiting to play") { showQueue() }
    }

    private fun showQueue() {
        content.removeAllViews()
        section("Up next")
        if (queue.isEmpty()) empty("Add music from your device to start a queue.")
        queue.forEachIndexed { i, track -> trackRow(track.title, if (i == index) "NOW PLAYING" else "Tap to play", if (i == index) "♫" else "▶") { playAt(i) } }
    }

    private fun pickAudio() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, 401)
    }

    private fun onPicked(data: Intent?) {
        val added = mutableListOf<Track>()
        data?.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) clip.getItemAt(i).uri?.let { added.add(trackFrom(it)) }
        }
        data?.data?.let { if (added.isEmpty()) added.add(trackFrom(it)) }
        added.forEach { track -> if (queue.none { it.uri == track.uri }) queue.add(track) }
        if (added.isNotEmpty()) {
            Toast.makeText(this, added.size.toString() + " track(s) added", Toast.LENGTH_SHORT).show()
            showLibrary()
        }
    }

    private fun trackFrom(uri: Uri): Track {
        var name = uri.lastPathSegment ?: "Audio track"
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0) ?: name
        } catch (_: Exception) { } finally { cursor?.close() }
        return Track(uri.toString(), name.substringBeforeLast('.', name))
    }

    private fun playAt(position: Int) {
        if (position !in queue.indices) return
        index = position
        val track = queue[index]
        try {
            player?.release()
            playing = false
            currentTitle = track.title
            miniTitle.text = "Loading: " + currentTitle
            player = MediaPlayer().apply {
                setDataSource(this@MainActivity, Uri.parse(track.uri))
                setOnPreparedListener {
                    it.start(); playing = true; currentTitle = track.title; refreshMini()
                    if (::progress.isInitialized) showPlayer()
                }
                setOnCompletionListener { if (repeat) playAt(index) else next() }
                setOnErrorListener { _, _, _ ->
                    Toast.makeText(this@MainActivity, "This audio format could not be played.", Toast.LENGTH_LONG).show()
                    playing = false; refreshMini(); true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open this audio file.", Toast.LENGTH_LONG).show()
        }
    }

    private fun next() {
        if (queue.isEmpty()) return
        val target = if (shuffle && queue.size > 1) {
            (queue.indices.filter { it != index }).random()
        } else (index + 1) % queue.size
        playAt(target)
    }

    private fun previous() {
        val p = player
        if (p != null && try { p.currentPosition > 3000 } catch (_: Exception) { false }) {
            p.seekTo(0); return
        }
        if (queue.isNotEmpty()) playAt(if (index <= 0) queue.lastIndex else index - 1)
    }

    private fun togglePlayback() {
        val p = player ?: run { if (queue.isNotEmpty()) playAt(if (index < 0) 0 else index); return }
        try {
            if (p.isPlaying) { p.pause(); playing = false }
            else { p.start(); playing = true }
            refreshMini()
        } catch (_: Exception) { }
    }

    private fun refreshMini() {
        miniTitle.text = currentTitle
        miniPlay.text = if (playing) "Ⅱ" else "▶"
    }

    private fun favorites(): Set<String> = prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()
    private fun isFavoriteCurrent(): Boolean {\n        val uri = queue.getOrNull(index)?.uri ?: return false\n        return uri in favorites()\n    }
    private fun toggleFavorite() {
        if (index !in queue.indices) return
        val set = favorites().toMutableSet()
        val uri = queue[index].uri
        if (!set.add(uri)) set.remove(uri)
        prefs.edit().putStringSet("favorites", set).apply()
    }

    private fun readPlaylists(): JSONArray = try { JSONArray(prefs.getString("playlists", "[]")) } catch (_: Exception) { JSONArray() }
    private fun savePlaylists(arr: JSONArray) { prefs.edit().putString("playlists", arr.toString()).apply() }
    private fun contains(arr: JSONArray, value: String): Boolean = (0 until arr.length()).any { arr.optString(it) == value }

    private fun createPlaylist() {
        val input = EditText(this).apply { hint = "Playlist name"; setTextColor(white); setHintTextColor(muted) }
        AlertDialog.Builder(this).setTitle("New playlist").setView(input)
            .setNegativeButton("Cancel", null).setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val all = readPlaylists()
                    all.put(JSONObject().put("name", name).put("tracks", JSONArray()))
                    savePlaylists(all); showPlaylists()
                }
            }.show()
    }

    private fun youtube(query: String) {
        val uri = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))
        val app = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.youtube")
        try { startActivity(app) } catch (_: Exception) { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }

    private fun section(title: String) {
        content.addView(label(title, 21, white, true).apply { setPadding(0, 20, 0, 12) })
    }

    private fun hero(title: String, subtitle: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 22, 20, 22)
            background = rounded(Color.rgb(83, 29, 65), 23)
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
            isClickable = true; isFocusable = true
            setOnClickListener { click() }
        }
        row.addView(label(title, 15, white, true))
        row.addView(label(subtitle, 12, muted, false).apply { setPadding(0, 4, 0, 0) })
        content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 9 })
    }

    private fun trackRow(title: String, subtitle: String, symbol: String, click: () -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(13, 13, 10, 13); background = rounded(card, 15)
            setOnClickListener { click() }
        }
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        info.addView(label(title, 14, white, true))
        info.addView(label(subtitle, 11, muted, false).apply { setPadding(0, 3, 0, 0) })
        row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(label(symbol, 18, white, true))
        content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8 })
    }

    private fun control(symbol: String, active: Boolean, click: () -> Unit): TextView =
        label(symbol, 20, if (active) accent else white, true).apply {
            gravity = Gravity.CENTER; setOnClickListener { click() }
            layoutParams = LinearLayout.LayoutParams(0, 52, 1f)
        }

    private fun empty(message: String) {
        content.addView(label(message, 13, muted, false).apply { setPadding(4, 8, 4, 18) })
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean) =
        TextView(this).apply {
            this.text = text; textSize = size.toFloat(); setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun rounded(color: Int, radius: Int) =
        android.graphics.drawable.GradientDrawable().apply { setColor(color); cornerRadius = radius.toFloat() }

    private fun formatTime(ms: Int): String {
        val total = (ms / 1000).coerceAtLeast(0)
        return (total / 60).toString() + ":" + (total % 60).toString().padStart(2, '0')
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 401 && resultCode == RESULT_OK) onPicked(data)
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        player?.release()
        player = null
        super.onDestroy()
    }
}
