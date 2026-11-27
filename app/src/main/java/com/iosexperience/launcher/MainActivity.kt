package com.iosexperience.launcher

import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("launcher_settings", Context.MODE_PRIVATE) }
    private val apps = mutableListOf<ResolveInfo>()
    private val shown = mutableListOf<ResolveInfo>()
    private lateinit var root: LinearLayout
    private lateinit var grid: GridView
    private lateinit var search: EditText
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var pageLabel: TextView
    private var page = 0
    private var query = ""
    private var columns = 4
    private var darkMode = true
    private var wallpaperIndex = 0
    private val wallpaperPalettes = listOf(
        intArrayOf(0xFF182848.toInt(), 0xFF4B6CB7.toInt(), 0xFF182848.toInt()),
        intArrayOf(0xFF0F2027.toInt(), 0xFF2C5364.toInt(), 0xFF0F2027.toInt()),
        intArrayOf(0xFF42275A.toInt(), 0xFF734B6D.toInt(), 0xFF42275A.toInt()),
        intArrayOf(0xFF134E5E.toInt(), 0xFF71B280.toInt(), 0xFF134E5E.toInt()),
        intArrayOf(0xFF3A1C71.toInt(), 0xFFD76D77.toInt(), 0xFFFFAF7B.toInt()),
        intArrayOf(0xFF232526.toInt(), 0xFF414345.toInt(), 0xFF141E30.toInt())
    )
    private val wallpaperNames = listOf("Blue Depth", "Ocean", "Violet", "Emerald", "Sunset", "Midnight")
    private var startX = 0f
    private var startY = 0f
    private val bg: Int get() = if (darkMode) Color.rgb(27, 37, 67) else Color.rgb(225, 233, 249)
    private val fg: Int get() = if (darkMode) Color.WHITE else Color.rgb(25, 35, 58)
    private val panel: Int get() = if (darkMode) 0x44FFFFFF else 0xAAFFFFFF.toInt()
    private val pm get() = packageManager

    private val ticker = object : Runnable {
        override fun run() {
            if (::clock.isInitialized) clock.text = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())
            if (::date.isInitialized) date.text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        columns = prefs.getInt("columns", 4).coerceIn(3, 5)
        darkMode = prefs.getBoolean("dark", true)
        wallpaperIndex = prefs.getInt("wallpaper", 0).coerceIn(wallpaperPalettes.indices)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        loadApps()
        renderHome()
        handler.post(ticker)
    }

    override fun onResume() {
        super.onResume()
        if (apps.isNotEmpty()) loadApps()
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun label(value: String, size: Float, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(fg)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun shape(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun baseColumn(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(42), dp(20), dp(14))
        background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
            if (darkMode) wallpaperPalettes[wallpaperIndex]
            else intArrayOf(0xFFF3F6FC.toInt(), 0xFFDCE6F5.toInt(), 0xFFF8F3FF.toInt()))
    }

    private fun renderHome() {
        root = baseColumn()
        setContentView(root)
        val status = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        status.addView(label("●", 12f), LinearLayout.LayoutParams(0, dp(24), 1f))
        status.addView(label("▮▮▮   ◉", 12f), LinearLayout.LayoutParams(-2, dp(24)))
        root.addView(status)

        date = label("", 15f, true).apply { gravity = Gravity.CENTER }
        root.addView(date, LinearLayout.LayoutParams(-1, dp(30)).apply { topMargin = dp(28) })
        clock = label("", 62f, true).apply { gravity = Gravity.CENTER }
        root.addView(clock, LinearLayout.LayoutParams(-1, dp(82)))

        search = EditText(this).apply {
            hint = "Search apps"
            setHintTextColor(if (darkMode) 0xCCFFFFFF.toInt() else 0xFF52617A.toInt())
            setTextColor(fg)
            textSize = 15f
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setPadding(dp(18), 0, dp(18), 0)
            background = shape(panel, 26)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    query = s?.toString().orEmpty()
                    page = 0
                    updateGrid()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(16) })

        grid = GridView(this).apply {
            numColumns = columns
            verticalSpacing = dp(8)
            horizontalSpacing = dp(4)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            selector = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            setPadding(0, dp(12), 0, dp(6))
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { startX = event.x; startY = event.y }
                    MotionEvent.ACTION_UP -> {
                        val dx = event.x - startX
                        val dy = event.y - startY
                        if (abs(dx) > dp(65) && abs(dx) > abs(dy) && query.isEmpty()) {
                            page = if (dx < 0) 1 else 0
                            updateGrid()
                            return@setOnTouchListener true
                        }
                    }
                }
                false
            }
        }
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        pageLabel = label("●   ○", 13f).apply { gravity = Gravity.CENTER }
        root.addView(pageLabel, LinearLayout.LayoutParams(-1, dp(22)))

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = shape(panel, 28)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        root.addView(dock, LinearLayout.LayoutParams(-1, dp(76)).apply { topMargin = dp(5) })
        addDock(dock, "Phone", android.R.drawable.sym_action_call)
        addDock(dock, "Messages", android.R.drawable.sym_action_email)
        addDock(dock, "Chrome", android.R.drawable.ic_menu_view)
        addDock(dock, "Camera", android.R.drawable.ic_menu_camera)

        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        bottom.addView(actionText("Wallpapers") { openWallpaperGallery() })
        bottom.addView(actionText("Library") { openLibrary() })
        bottom.addView(actionText("Settings") { openSettings() })
        root.addView(bottom, LinearLayout.LayoutParams(-1, dp(40)))
        updateGrid()
    }

    private fun actionText(title: String, action: () -> Unit) = label(title, 12f, true).apply {
        gravity = Gravity.CENTER
        setPadding(dp(12), dp(6), dp(12), dp(6))
        setOnClickListener { action() }
    }

    private fun addDock(dock: LinearLayout, name: String, fallback: Int) {
        val savedPackage = prefs.getString("dock_$name", null)
        val app = apps.firstOrNull { it.activityInfo.packageName == savedPackage }
            ?: apps.firstOrNull { it.loadLabel(pm).toString().contains(name, true) }
        val item = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        val icon = ImageView(this).apply {
            if (app != null) setImageDrawable(app.loadIcon(pm)) else setImageResource(fallback)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        item.addView(icon, LinearLayout.LayoutParams(dp(42), dp(42)))
        dock.addView(item, LinearLayout.LayoutParams(0, -1, 1f))
        item.setOnClickListener { launch(app) }
        item.setOnLongClickListener { chooseDockApp(name); true }
    }

    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps.clear()
        apps.addAll(pm.queryIntentActivities(intent, 0).distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase(Locale.getDefault()) })
        if (::grid.isInitialized) updateGrid()
    }

    private fun updateGrid() {
        if (!::grid.isInitialized) return
        shown.clear()
        shown.addAll(apps.filter { it.loadLabel(pm).toString().contains(query, true) })
        val pageSize = columns * 4
        val start = if (query.isEmpty() && page == 1) pageSize else 0
        val pageItems = if (query.isNotEmpty()) shown else shown.drop(start).take(pageSize)
        grid.numColumns = columns
        grid.adapter = object : BaseAdapter() {
            override fun getCount() = pageItems.size
            override fun getItem(position: Int) = pageItems[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val info = pageItems[position]
                val cell = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(3), dp(5), dp(3), dp(5))
                }
                val icon = ImageView(this@MainActivity).apply {
                    setImageDrawable(info.loadIcon(pm))
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    background = shape(0x22FFFFFF, 17)
                    setPadding(dp(5), dp(5), dp(5), dp(5))
                }
                cell.addView(icon, LinearLayout.LayoutParams(dp(56), dp(56)))
                cell.addView(label(info.loadLabel(pm).toString(), 10.5f).apply {
                    gravity = Gravity.CENTER
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }, LinearLayout.LayoutParams(-1, dp(22)))
                cell.setOnClickListener { launch(info) }
                cell.setOnLongClickListener {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + info.activityInfo.packageName))
                    startActivity(intent)
                    true
                }
                return cell
            }
        }
        if (::pageLabel.isInitialized) pageLabel.text = if (page == 0) "●   ○" else "○   ●"
    }

    private fun launch(info: ResolveInfo?) {
        if (info == null) {
            Toast.makeText(this, "App not found on this device", Toast.LENGTH_SHORT).show()
            return
        }
        pm.getLaunchIntentForPackage(info.activityInfo.packageName)?.let { startActivity(it) }
    }

    private fun openLibrary() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(bg)
        }
        content.addView(label("App Library", 27f, true), LinearLayout.LayoutParams(-1, dp(54)))
        val input = EditText(this).apply {
            hint = "Find an app"
            setTextColor(fg)
            setHintTextColor(0xFF9AA5B8.toInt())
            background = shape(panel, 18)
            setPadding(dp(14), 0, dp(14), 0)
        }
        content.addView(input, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val categories = apps.groupBy { category(it.loadLabel(pm).toString()) }
        categories.toSortedMap().forEach { (group, items) ->
            list.addView(label(group, 17f, true).apply { setPadding(0, dp(18), 0, dp(8)) })
            items.forEach { info ->
                val row = label("▣   " + info.loadLabel(pm).toString(), 15f).apply {
                    setPadding(dp(8), dp(10), dp(8), dp(10))
                    setOnClickListener { launch(info) }
                }
                list.addView(row)
            }
        }
        scroll.addView(list)
        content.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                list.removeAllViews()
                apps.filter { it.loadLabel(pm).toString().contains(s.toString(), true) }.forEach { info ->
                    list.addView(label("▣   " + info.loadLabel(pm).toString(), 15f).apply {
                        setPadding(dp(8), dp(10), dp(8), dp(10))
                        setOnClickListener { launch(info) }
                    })
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        showPanel("App Library", content)
    }

    private fun category(name: String): String {
        val s = name.lowercase(Locale.getDefault())
        return when {
            listOf("camera", "photo", "gallery", "video", "music", "youtube", "netflix").any { s.contains(it) } -> "Entertainment & Media"
            listOf("phone", "message", "contact", "mail", "whatsapp", "telegram").any { s.contains(it) } -> "Communication"
            listOf("chrome", "browser", "maps", "drive", "file", "setting").any { s.contains(it) } -> "Utilities"
            listOf("game", "play", "bgmi", "pubg").any { s.contains(it) } -> "Games"
            else -> "Other apps"
        }
    }

    private fun openSettings() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(20))
            setBackgroundColor(bg)
        }
        content.addView(label("Personalise your home screen", 22f, true), LinearLayout.LayoutParams(-1, dp(58)))
        content.addView(label("Icon grid", 15f, true).apply { setPadding(0, dp(14), 0, dp(8)) })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(3, 4, 5).forEach { count ->
            row.addView(label("$count columns", 14f, true).apply {
                gravity = Gravity.CENTER
                background = shape(panel, 16)
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setOnClickListener {
                    columns = count
                    prefs.edit().putInt("columns", count).apply()
                    renderHome()
                    Toast.makeText(this@MainActivity, "Home grid updated", Toast.LENGTH_SHORT).show()
                }
            }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        content.addView(row)
        content.addView(label("Appearance", 15f, true).apply { setPadding(0, dp(22), 0, dp(8)) })
        content.addView(label(if (darkMode) "Switch to Light appearance" else "Switch to Dark appearance", 15f).apply {
            background = shape(panel, 16)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setOnClickListener {
                darkMode = !darkMode
                prefs.edit().putBoolean("dark", darkMode).apply()
                renderHome()
            }
        })
        content.addView(label("Wallpaper gallery", 15f).apply {
            background = shape(panel, 16)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setOnClickListener { openWallpaperGallery() }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(12) })
        content.addView(label("Customise dock apps", 15f, true).apply { setPadding(0, dp(22), 0, dp(8)) })
        listOf("Phone", "Messages", "Chrome", "Camera").forEach { slot ->
            content.addView(label("$slot  ·  Change app", 14f).apply {
                background = shape(panel, 14)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                setOnClickListener { chooseDockApp(slot) }
            }, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(6) })
        }
        content.addView(label("Set as default Home app", 15f).apply {
            background = shape(panel, 16)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setOnClickListener {
                try { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                catch (_: Exception) { Toast.makeText(this@MainActivity, "Choose iOS Experience in Android Home settings", Toast.LENGTH_LONG).show() }
            }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(12) })
        content.addView(label("About iOS Experience  •  Version 0.2.0", 12f).apply { setPadding(0, dp(22), 0, 0) })
        showPanel("Settings", content)
    }

    private fun showPanel(title: String, content: View) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(label(title, 18f, true), LinearLayout.LayoutParams(0, dp(46), 1f))
        header.addView(label("Done", 15f, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setOnClickListener { renderHome() }
        })
        container.addView(header)
        container.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(container)
    }

    private fun openWallpaperGallery() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(18))
            setBackgroundColor(bg)
        }
        content.addView(label("Choose a home screen wallpaper", 20f, true),
            LinearLayout.LayoutParams(-1, dp(54)))
        wallpaperNames.forEachIndexed { index, name ->
            val preview = TextView(this).apply {
                text = if (wallpaperIndex == index) "$name   ✓" else name
                textSize = 16f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(18), 0, dp(18), 0)
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    wallpaperPalettes[index]).apply { cornerRadius = dp(18).toFloat() }
                setOnClickListener {
                    wallpaperIndex = index
                    prefs.edit().putInt("wallpaper", index).apply()
                    renderHome()
                    Toast.makeText(this@MainActivity, "$name wallpaper applied", Toast.LENGTH_SHORT).show()
                }
            }
            content.addView(preview, LinearLayout.LayoutParams(-1, dp(66)).apply {
                bottomMargin = dp(10)
            })
        }
        content.addView(label("More wallpapers from Android", 14f).apply {
            background = shape(panel, 16)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setOnClickListener {
                try { startActivity(Intent(Intent.ACTION_SET_WALLPAPER)) }
                catch (_: Exception) { Toast.makeText(this@MainActivity, "Wallpaper picker unavailable", Toast.LENGTH_SHORT).show() }
            }
        })
        showPanel("Wallpapers", content)
    }

    private fun chooseDockApp(slot: String) {
        if (apps.isEmpty()) return
        val labels = apps.map { it.loadLabel(pm).toString() }.toTypedArray()
        val packages = apps.map { it.activityInfo.packageName }
        val selected = prefs.getString("dock_$slot", null)
        val checked = packages.indexOf(selected)
        AlertDialog.Builder(this)
            .setTitle("Choose $slot dock app")
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                prefs.edit().putString("dock_$slot", packages[which]).apply()
                dialog.dismiss()
                renderHome()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
