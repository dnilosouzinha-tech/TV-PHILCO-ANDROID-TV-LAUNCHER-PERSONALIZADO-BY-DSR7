package dsr7.home

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Calendar

class MainActivity : Activity() {
    private val gold = Color.parseColor("#D4AF37")
    private val muted = Color.parseColor("#B3A36A")
    private val line = Color.parseColor("#2E2716")
    private val white = Color.parseColor("#F2EFE6")
    private lateinit var greet: TextView
    private lateinit var memText: TextView
    private lateinit var memBar: ProgressBar
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            updateMem()
            handler.postDelayed(this, 2000)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun shape(fill: Int, stroke: Int, w: Int, r: Int) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(r).toFloat()
        setStroke(dp(w), stroke)
    }

    private fun btn(label: String, sp: Float, r: Int, onClick: () -> Unit): TextView {
        val t = TextView(this)
        t.text = label
        t.textSize = sp
        t.setTextColor(white)
        t.typeface = Typeface.DEFAULT_BOLD
        t.gravity = Gravity.CENTER
        t.setPadding(dp(6), dp(6), dp(6), dp(6))
        val sl = StateListDrawable()
        sl.addState(intArrayOf(android.R.attr.state_focused), shape(Color.parseColor("#1F1B0E"), gold, 2, r))
        sl.addState(intArrayOf(), shape(Color.parseColor("#181818"), line, 1, r))
        t.background = sl
        t.isFocusable = true
        t.isClickable = true
        t.setOnClickListener { onClick() }
        return t
    }

    private fun safeStart(i: Intent) {
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível abrir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchByLabel(label: String) {
        val pm = packageManager
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val list = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(cat), 0)
            val m = list.firstOrNull { it.loadLabel(pm).toString().contains(label, true) }
            if (m != null) {
                safeStart(
                    Intent(Intent.ACTION_MAIN).addCategory(cat)
                        .setClassName(m.activityInfo.packageName, m.activityInfo.name)
                )
                return
            }
        }
        Toast.makeText(this, "$label não encontrado", Toast.LENGTH_SHORT).show()
    }

    private fun showAllApps() {
        val pm = packageManager
        val list = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0
        ).sortedBy { it.loadLabel(pm).toString().lowercase() }
        val names = list.map { it.loadLabel(pm).toString() }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Todos os Apps").setItems(names) { _, which ->
            pm.getLeanbackLaunchIntentForPackage(list[which].activityInfo.packageName)?.let { safeStart(it) }
        }.show()
    }

    private fun group(title: String, items: List<View>): LinearLayout {
        val g = LinearLayout(this)
        g.orientation = LinearLayout.VERTICAL
        val l = TextView(this)
        l.text = title
        l.textSize = 10f
        l.setTextColor(muted)
        l.setPadding(dp(3), 0, 0, dp(6))
        g.addView(l)
        val row = LinearLayout(this)
        items.forEach {
            val p = LinearLayout.LayoutParams(dp(92), dp(52))
            p.setMargins(0, 0, dp(9), 0)
            row.addView(it, p)
        }
        g.addView(row)
        return g
    }

    private fun small(t: String): TextView {
        val v = TextView(this)
        v.text = t
        v.textSize = 9f
        v.setTextColor(muted)
        return v
    }

    private fun big(t: String): TextView {
        val v = TextView(this)
        v.text = t
        v.textSize = 20f
        v.setTextColor(gold)
        v.typeface = Typeface.DEFAULT_BOLD
        return v
    }

    private fun spacer(h: Int) = View(this).also { it.layoutParams = LinearLayout.LayoutParams(1, dp(h)) }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_VERTICAL
        root.setBackgroundColor(Color.parseColor("#0A0A0A"))
        root.setPadding(dp(48), dp(36), dp(48), dp(36))

        // Cabeçalho: saudação + logo
        greet = TextView(this)
        greet.textSize = 22f
        greet.typeface = Typeface.DEFAULT_BOLD
        greet.setTextColor(white)
        val header = LinearLayout(this)
        header.gravity = Gravity.CENTER_VERTICAL
        header.addView(greet, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        try {
            val bmp = BitmapFactory.decodeStream(assets.open("logo.png"))
            val img = ImageView(this)
            img.setImageBitmap(bmp)
            img.adjustViewBounds = true
            header.addView(img, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(40)))
        } catch (e: Exception) {
        }
        root.addView(header)
        root.addView(spacer(26))

        // Linha de apps
        val apps = LinearLayout(this)
        apps.background = shape(Color.parseColor("#111111"), line, 1, 24)
        apps.setPadding(dp(16), dp(16), dp(16), dp(16))
        fun tileLp(): LinearLayout.LayoutParams {
            val p = LinearLayout.LayoutParams(0, dp(75), 1f)
            p.setMargins(dp(8), 0, dp(8), 0)
            return p
        }
        listOf("Netflix", "YouTube", "Globoplay").forEach { n ->
            apps.addView(btn(n, 16f, 12) { launchByLabel(n) }, tileLp())
        }
        apps.addView(btn("Configurações", 16f, 12) { safeStart(Intent(Settings.ACTION_SETTINGS)) }, tileLp())
        root.addView(apps)
        root.addView(spacer(26))

        // Entradas e atalhos
        val inputs = mutableListOf<View>()
        inputs.add(btn("TV aberta", 10f, 11) {
            safeStart(Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI))
        })
        try {
            val tim = getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
            tim.tvInputList.filter { it.isPassthroughInput }.take(4).forEach { info ->
                val name = info.loadLabel(this)?.toString() ?: info.id
                inputs.add(btn(name, 10f, 11) {
                    safeStart(Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(info.id)))
                })
            }
        } catch (e: Exception) {
        }
        val shortcuts = listOf<View>(
            btn("Todos os Apps", 10f, 11) { showAllApps() },
            btn("Ajustes da TV", 10f, 11) { safeStart(Intent(Settings.ACTION_SETTINGS)) }
        )
        val mid = LinearLayout(this)
        mid.addView(group("Entradas", inputs), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        mid.addView(group("Atalhos", shortcuts))
        root.addView(mid)
        root.addView(spacer(26))

        // Relógio + FPS + memória
        val card = LinearLayout(this)
        card.background = shape(Color.parseColor("#111111"), line, 1, 20)
        card.setPadding(dp(24), dp(18), dp(24), dp(18))
        card.gravity = Gravity.CENTER_VERTICAL

        val clockCol = LinearLayout(this)
        clockCol.orientation = LinearLayout.VERTICAL
        val clock = TextClock(this)
        clock.format24Hour = "HH:mm"
        clock.format12Hour = "HH:mm"
        clock.textSize = 60f
        clock.setTextColor(gold)
        clock.typeface = Typeface.DEFAULT_BOLD
        val date = TextClock(this)
        date.format24Hour = "EEEE, d 'de' MMMM"
        date.format12Hour = "EEEE, d 'de' MMMM"
        date.textSize = 11f
        date.setTextColor(white)
        clockCol.addView(clock)
        clockCol.addView(date)
        card.addView(clockCol)

        val divider = View(this)
        divider.setBackgroundColor(line)
        val dp1 = LinearLayout.LayoutParams(dp(1), dp(70))
        dp1.setMargins(dp(24), 0, dp(24), 0)
        card.addView(divider, dp1)

        @Suppress("DEPRECATION")
        val hz = windowManager.defaultDisplay.refreshRate.toInt()
        memText = big("-")
        memBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        memBar.max = 100
        memBar.progressTintList = ColorStateList.valueOf(gold)
        memBar.progressBackgroundTintList = ColorStateList.valueOf(line)
        val stats = LinearLayout(this)
        stats.orientation = LinearLayout.VERTICAL
        stats.addView(small("FPS (taxa da tela)"))
        stats.addView(big("$hz"))
        stats.addView(spacer(8))
        stats.addView(small("Memória"))
        stats.addView(memText)
        stats.addView(memBar, LinearLayout.LayoutParams(dp(120), dp(6)))
        card.addView(stats)
        root.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        setContentView(root)
        apps.getChildAt(0).requestFocus()
    }

    private fun updateGreeting() {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val g = if (h < 12) "Bom dia" else if (h < 18) "Boa tarde" else "Boa noite"
        val s = SpannableString("Olá Nilo. $g, o que vamos assistir hoje?")
        s.setSpan(ForegroundColorSpan(gold), 4, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        greet.text = s
    }

    private fun updateMem() {
        val mi = ActivityManager.MemoryInfo()
        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        val gb = 1024.0 * 1024.0 * 1024.0
        val used = mi.totalMem - mi.availMem
        memText.text = String.format("%.1f / %.1f GB", used / gb, mi.totalMem / gb)
        memBar.progress = (100.0 * used / mi.totalMem).toInt()
    }

    override fun onResume() {
        super.onResume()
        updateGreeting()
        handler.post(tick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Tela inicial: o botão Voltar não faz nada.
    }
}
