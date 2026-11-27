package com.iosexperience.launcher

import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.Typeface
import android.text.TextUtils
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.ScrollView

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var appsGrid: GridView
    private lateinit var search: EditText
    private val apps = mutableListOf<android.content.pm.ResolveInfo>()
    private val visibleApps = mutableListOf<android.content.pm.ResolveInfo>()
    private val pm get() = packageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.rgb(22, 27, 48)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        buildHome()
        loadApps()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun text(value: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun buildHome() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(46), dp(22), dp(18))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(42, 61, 103), Color.rgb(91, 91, 143), Color.rgb(30, 40, 70)))
        }
        setContentView(root)

        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(text("9:41", 15f, Color.WHITE, true), LinearLayout.LayoutParams(0, dp(30), 1f))
        top.addView(text("●  ▮▮▮  ◉", 12f), LinearLayout.LayoutParams(-2, dp(30)))
        root.addView(top)

        val date = text("Friday, November 27", 16f, Color.WHITE, true)
        date.gravity = Gravity.CENTER
        val dateParams = LinearLayout.LayoutParams(-1, dp(32)).apply { topMargin = dp(34) }
        root.addView(date, dateParams)

        val clock = text("9:41", 58f, Color.WHITE, true).apply { gravity = Gravity.CENTER }
        root.addView(clock, LinearLayout.LayoutParams(-1, dp(82)))

        search = EditText(this).apply {
            hint = "Search apps"
            setHintTextColor(0xCCFFFFFF.toInt())
            setTextColor(Color.WHITE)
            textSize = 15f
            singleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setPadding(dp(18), 0, dp(18), 0)
            background = rounded(0x44FFFFFF, 28)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { filterApps(s?.toString().orEmpty()) }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(18) })

        appsGrid = GridView(this).apply {
            numColumns = 4
            verticalSpacing = dp(12)
            horizontalSpacing = dp(4)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            selector = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            clipToPadding = false
            setPadding(0, dp(18), 0, dp(8))
        }
        root.addView(appsGrid, LinearLayout.LayoutParams(-1, 0, 1f))

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(0x55FFFFFF, 30)
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }
        root.addView(dock, LinearLayout.LayoutParams(-1, dp(82)).apply { topMargin = dp(8) })
        addDockItem(dock, "Phone", android.R.drawable.sym_action_call)
        addDockItem(dock, "Messages", android.R.drawable.sym_action_email)
        addDockItem(dock, "Browser", android.R.drawable.ic_menu_view)
        addDockItem(dock, "Camera", android.R.drawable.ic_menu_camera)

        val tools = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        val wallpaper = text("Wallpaper", 13f).apply { gravity = Gravity.CENTER; setPadding(dp(16), dp(12), dp(16), dp(8)); setOnClickListener { openWallpaperPicker() } }
        val allApps = text("All apps", 13f).apply { gravity = Gravity.CENTER; setPadding(dp(16), dp(12), dp(16), dp(8)); setOnClickListener { search.requestFocus(); search.post { (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showSoftInput(search, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT) } } }
        tools.addView(wallpaper); tools.addView(allApps)
        root.addView(tools)
    }

    private fun addDockItem(dock: LinearLayout, label: String, icon: Int) {
        val item = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        val image = ImageView(this).apply { setImageResource(icon); setColorFilter(Color.WHITE); scaleType = ImageView.ScaleType.CENTER_INSIDE }
        item.addView(image, LinearLayout.LayoutParams(dp(30), dp(30)))
        item.addView(text(label, 10f).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, dp(18)))
        dock.addView(item, LinearLayout.LayoutParams(0, -1, 1f))
        item.setOnClickListener { launchByLabel(label) }
    }

    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps.clear()
        apps.addAll(pm.queryIntentActivities(intent, 0).sortedBy { it.loadLabel(pm).toString().lowercase() })
        filterApps("")
    }

    private fun filterApps(query: String) {
        if (!::appsGrid.isInitialized) return
        visibleApps.clear()
        visibleApps.addAll(apps.filter { it.loadLabel(pm).toString().contains(query, true) })
        appsGrid.adapter = object : BaseAdapter() {
            override fun getCount() = visibleApps.size
            override fun getItem(position: Int) = visibleApps[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val app = visibleApps[position]
                val cell = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(3), dp(6), dp(3), dp(6))
                }
                val icon = ImageView(this@MainActivity).apply {
                    setImageDrawable(app.loadIcon(pm))
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    background = rounded(0x22FFFFFF, 18)
                    setPadding(dp(7), dp(7), dp(7), dp(7))
                }
                cell.addView(icon, LinearLayout.LayoutParams(dp(54), dp(54)))
                val label = text(app.loadLabel(pm).toString(), 11f, Color.WHITE).apply {
                    gravity = Gravity.CENTER
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }
                cell.addView(label, LinearLayout.LayoutParams(-1, dp(24)))
                cell.setOnClickListener {
                    val launch = pm.getLaunchIntentForPackage(app.activityInfo.packageName)
                    if (launch != null) startActivity(launch)
                }
                return cell
            }
        }
    }

    private fun launchByLabel(label: String) {
        val match = apps.firstOrNull { it.loadLabel(pm).toString().contains(label, true) }
        match?.let {
            pm.getLaunchIntentForPackage(it.activityInfo.packageName)?.let { launch -> startActivity(launch) }
        } ?: Toast.makeText(this, "Open $label from your app list", Toast.LENGTH_SHORT).show()
    }

    private fun openWallpaperPicker() {
        try {
            startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, android.content.ComponentName(this@MainActivity, MainActivity::class.java))
            })
        } catch (_: Exception) {
            try { startActivity(Intent(Intent.ACTION_SET_WALLPAPER)) }
            catch (_: Exception) { Toast.makeText(this, "Wallpaper settings are unavailable", Toast.LENGTH_SHORT).show() }
        }
    }
}
