package com.casper.lab

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.GradientDrawable
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

/**
 * Native, offline-first CASPER research interface.
 * All artificial spectrum and spatial data is explicitly identified as simulation.
 */
class MainActivity : Activity() {
    private val bg = Color.rgb(3, 9, 7)
    private val cardBg = Color.rgb(9, 23, 18)
    private val forest = Color.rgb(14, 46, 33)
    private val pale = Color.rgb(238, 246, 241)
    private val emerald = Color.rgb(91, 226, 166)
    private val muted = Color.rgb(132, 168, 149)
    private val outline = Color.rgb(36, 72, 54)

    private lateinit var content: LinearLayout
    private lateinit var bottomNav: LinearLayout
    private lateinit var sessions: SessionStore
    private lateinit var flipper: FlipperUsb
    private val tabs = listOf("HOME", "SPECTRUM", "HRV", "3D LAB", "DEVICES", "LOGS")
    private val navTexts = mutableListOf<TextView>()
    private var currentTab = 0
    private var demoRunning = false
    private var intervals = "812, 795, 824, 806, 839, 801, 821, 790, 825, 810, 803, 835"
    private var hrvStats: HrvStats? = null
    private var lastError: String? = null
    private var lastUsbLine = "No diagnostic messages yet"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        sessions = SessionStore(this)
        flipper = FlipperUsb(this,
            onState = { if (::content.isInitialized && currentTab in listOf(0,4)) render() },
            onText = { message ->
                lastUsbLine = message
                sessions.add("USB DIAGNOSTIC", message)
                if (::content.isInitialized && currentTab == 4) render()
            }
        )
        buildShell()
        render()
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(
                Color.rgb(1,4,3), Color.rgb(5,19,13), Color.rgb(10,38,26), Color.rgb(2,8,6)
            ))
        }
        setContentView(root)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20),dp(13),dp(20),dp(13))
        }
        val ghost = ImageView(this).apply {
            setImageResource(R.drawable.ghost_mark)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(bg, 14, outline, 1)
            clipToOutline = true
        }
        header.addView(ghost, LinearLayout.LayoutParams(dp(48),dp(48)))
        val titleArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11),0,0,0)
        }
        titleArea.addView(text("CASPER", 22f, pale, bold=true, spacing=0.16f))
        titleArea.addView(text("RF  /  RESEARCH SYSTEMS", 9f, muted, spacing=0.09f))
        header.addView(titleArea, LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f))
        header.addView(chip("OFFLINE",emerald,forest))
        root.addView(header)

        val separator = View(this).apply { setBackgroundColor(outline) }
        root.addView(separator, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(1)))

        val scroll = ScrollView(this).apply {
            isFillViewport=true
            clipToPadding=false
            overScrollMode=View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        content = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(18),dp(19),dp(18),dp(28))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f))

        val navScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled=false
            setBackgroundColor(Color.rgb(4, 13, 10))
            setPadding(dp(10),dp(9),dp(10),dp(11))
        }
        bottomNav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabs.forEachIndexed { index, name ->
            val label = text(name,10f,muted,bold=true,spacing=0.03f).apply {
                gravity = Gravity.CENTER
                setPadding(dp(13),dp(13),dp(13),dp(13))
                setOnClickListener { currentTab=index; render() }
            }
            navTexts.add(label)
            bottomNav.addView(label, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,dp(42)))
        }
        navScroll.addView(bottomNav)
        root.addView(navScroll)
    }

    private fun render() {
        content.removeAllViews()
        navTexts.forEachIndexed { index, label ->
            label.setTextColor(if (index==currentTab) pale else muted)
            label.background=if (index==currentTab) rounded(forest,12,outline,1) else null
        }
        when(currentTab) {
            0->homePage()
            1->spectrumPage()
            2->hrvPage()
            3->roomPage()
            4->devicesPage()
            else->logsPage()
        }
    }

    private fun homePage() {
        heading("RESEARCH CONSOLE", "Instrument-ready software. Simulation is enabled until validated hardware is connected.")
        card {
            addView(rowText("SYSTEM STATUS", "v0.1.0 / ALPHA"))
            gap(12)
            addView(text("Casper Lab",28f,pale,true))
            gap(8)
            addView(text("Local-first signal research and spatial visualization.",13f,muted))
            gap(16)
            addView(chip(if (demoRunning) "SIMULATION ACTIVE" else "SIMULATION STANDBY",emerald,forest))
            gap(16)
            addView(action(if (demoRunning) "STOP DEMO SESSION" else "START DEMO SESSION") {
                demoRunning = !demoRunning
                sessions.add(if (demoRunning) "DEMO START" else "DEMO STOP", "Synthetic test vectors only")
                render()
            })
        }
        gap(18)
        addSectionLabel("MODULES")
        metric("Flipper USB", flipper.state, "USB CDC diagnostic interface", 4)
        metric("RF Spectrum", if (demoRunning) "DEMO RUNNING" else "READY", "Synthetic waterfall and signal trace", 1)
        metric("HRV LAB", "NN INTERVALS", "Local statistical calculations", 2)
        metric("Spatial Viewer", "3D SIMULATION", "Rotatable room and test point cloud", 3)
        gap(16)
        info("DATA INTEGRITY", "A synthetic waveform is never presented as live radio data. Cardiac measurements must be imported from a legitimate ECG/PPG source; visualizations are not diagnostics.")
    }

    private fun spectrumPage() {
        heading("RF SPECTRUM", "Signal-processing preview · synthetic vectors")
        card {
            addView(rowText("FFT VISUALIZER",if (demoRunning) "SIMULATING" else "PAUSED"))
            gap(14)
            val chart = SpectrumDemoView(this@MainActivity).apply { running=demoRunning }
            addView(chart, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(270)))
            gap(12)
            addView(text("Frequency axis is arbitrary. No RF or IQ data is being acquired.",12f,muted))
            gap(15)
            addView(action(if (demoRunning) "PAUSE DEMO" else "RUN SYNTHETIC DEMO") {
                demoRunning = !demoRunning
                sessions.add("SPECTRUM DEMO",if (demoRunning) "Started synthetic FFT" else "Paused synthetic FFT")
                render()
            })
        }
        gap(15)
        info("SENSOR REQUIREMENTS", "The Flipper diagnostic connection does not produce broadband IQ samples. Actual FFT input will require a compatible SDR adapter and documented sampling metadata.")
        gap(12)
        addSectionLabel("PIPELINE")
        metric("SOURCE", "SYNTHETIC", "Deterministic test peaks and controlled noise",null)
        metric("PROCESSING", "LOCAL", "FFT and windowing available as integration boundary",null)
        metric("NEXT ADAPTER", "USB SDR", "Requires device-specific Android driver",4)
    }

    private fun hrvPage() {
        heading("HRV ANALYSIS", "Offline computation from normal-to-normal cardiac intervals")
        card {
            addView(rowText("NN INTERVAL INPUT", "MILLISECONDS"))
            gap(10)
            val editor = EditText(this@MainActivity).apply {
                setText(intervals)
                setTextColor(pale)
                setHintTextColor(muted)
                setBackgroundColor(Color.rgb(6, 20, 14))
                textSize=14f
                setPadding(dp(12),dp(12),dp(12),dp(12))
                minLines=4
                inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }
            addView(editor, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(108)))
            gap(10)
            addView(text("Paste comma-separated, artifact-screened NN intervals from ECG or a compatible validated source. Default values are examples.",12f,muted))
            gap(14)
            addView(action("CALCULATE HRV") {
                intervals = editor.text.toString()
                try {
                    hrvStats = HrvMath.calculate(HrvMath.parseNnCsv(intervals))
                    lastError = null
                    sessions.add("HRV CALCULATION", "Analyzed ${hrvStats?.count} entered NN intervals")
                } catch (e: Exception) {
                    hrvStats = null
                    lastError = e.message ?: "Invalid intervals"
                }
                render()
            })
        }
        lastError?.let { gap(12); info("INPUT ERROR",it) }
        hrvStats?.let { stats ->
            gap(15)
            addSectionLabel("CALCULATED METRICS")
            metric("RMSSD", String.format(Locale.US,"%.1f ms",stats.rmssdMs),"Root mean square of successive differences",null)
            metric("SDNN", String.format(Locale.US,"%.1f ms",stats.sdnnMs),"Standard deviation of NN intervals",null)
            metric("pNN50", String.format(Locale.US,"%.1f%%",stats.pnn50Percent),"Successive changes over 50 ms",null)
            metric("MEAN RATE", String.format(Locale.US,"%.1f BPM",stats.meanHeartRateBpm),"Approximation from mean NN interval; n=${stats.count}",null)
        }
        gap(12)
        info("RESEARCH LIMITATION", "These statistics do not determine stress, emotions, identity, disease, or autonomic diagnoses. They do not originate from Flipper RF measurements.")
    }

    private fun roomPage() {
        heading("3D SIMULATION", "Synthetic room and mathematical point-cloud demo")
        card {
            addView(rowText("SPATIAL LAB", "SIMULATION ONLY"))
            gap(14)
            val scene = RoomSceneView(this@MainActivity)
            addView(scene, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(305)))
            gap(14)
            addView(text("Drag inside the viewer to rotate. Every point is mathematically generated; nothing here represents a building or a person.",12f,muted))
            gap(12)
            val controls = LinearLayout(this@MainActivity).apply { orientation=LinearLayout.HORIZONTAL }
            val minus = action("−  ZOOM") { scene.zoom = (scene.zoom-0.2).coerceAtLeast(0.4); scene.invalidate() }
            val plus = action("+  ZOOM") { scene.zoom = (scene.zoom+0.2).coerceAtMost(2.0); scene.invalidate() }
            controls.addView(minus,LinearLayout.LayoutParams(0,dp(44),1f))
            controls.addView(View(this@MainActivity),LinearLayout.LayoutParams(dp(10),dp(1)))
            controls.addView(plus,LinearLayout.LayoutParams(0,dp(44),1f))
            addView(controls)
        }
        gap(15)
        info("MEASUREMENTS ≠ INFERENCE", "A real radar point cloud would require an authorized external radar, validated capture geometry, calibration, and a compatible device parser. The simulated room cannot detect hidden people or building interiors.")
    }

    private fun devicesPage() {
        heading("CONNECTED DEVICES", "USB-C host diagnostics and future sensor adapters")
        card {
            addView(rowText("FLIPPER / USB", flipper.state))
            gap(12)
            addView(text("Connect Flipper Zero to your Android device with a USB-C data cable. Accept the USB permission prompt when displayed.",13f,muted))
            gap(14)
            addView(action("REFRESH USB DEVICES") { render() })
            gap(11)
            val discovered = flipper.available()
            if (discovered.isEmpty()) {
                addView(text("No USB devices detected. Confirm that your phone supports USB host mode and the cable transfers data.",12f,muted))
            } else {
                discovered.forEach { device ->
                    gap(10)
                    addView(text("${device.deviceName}\nVID ${device.vendorId.toString(16).uppercase()} · PID ${device.productId.toString(16).uppercase()} · ${device.interfaceCount} interfaces",12f,pale))
                    gap(6)
                    addView(outlineAction("REQUEST ACCESS / CONNECT") {
                        flipper.connect(device)
                        sessions.add("USB CONNECT REQUEST", "VID=${device.vendorId}, PID=${device.productId}")
                    })
                }
            }
        }
        gap(14)
        card {
            addView(rowText("USB DIAGNOSTIC", "NO RF CONTROL"))
            gap(12)
            addView(text(lastUsbLine,12f,pale))
            gap(15)
            addView(outlineAction("REQUEST DEVICE HELP") { flipper.sendDiagnostic() })
            gap(9)
            addView(outlineAction("DISCONNECT") { flipper.disconnect(); render() })
        }
        gap(14)
        info("HARDWARE BOUNDARIES", "Flipper's USB CLI supports diagnostics and compatible commands. It does not expose a citywide RF network, full-band IQ, radar imagery, or physiological measurements. External sensors need separate drivers and validation.")
    }

    private fun logsPage() {
        heading("LOCAL SESSION LOG", "Explicit activity history stored only on this device")
        card {
            addView(rowText("RECENT ACTIVITY", "${sessions.entries().size} RECORDS"))
            gap(15)
            addView(action("SHARE TEXT EXPORT") {
                val text = sessions.exportText()
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type="text/plain"; putExtra(Intent.EXTRA_TEXT,text)
                }
                startActivity(Intent.createChooser(intent,"Export Casper session log"))
            })
            gap(10)
            addView(outlineAction("CLEAR LOG") {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Clear local log?")
                    .setMessage("This deletes all activity records saved by Casper on this device.")
                    .setNegativeButton("CANCEL",null)
                    .setPositiveButton("CLEAR") { _,_ -> sessions.clear();render() }
                    .show()
            })
        }
        gap(15)
        val events = sessions.entries()
        if (events.isEmpty()) info("NO EVENTS", "Run an experiment or calculate HRV to create a local record.")
        events.take(25).forEach { event ->
            card { addView(text(event,12f,pale)) }
            gap(8)
        }
        gap(12)
        info("PRIVACY", "No account required. No network permission, no location permission, and no automatic upload. Logs can be exported explicitly using Android's share sheet.")
    }

    private fun heading(title: String, sub: String) {
        content.addView(text(title,22f,pale,bold=true,spacing=0.045f))
        gap(7)
        content.addView(text(sub,12.5f,muted))
        gap(20)
    }
    private fun addSectionLabel(value: String) {
        content.addView(text(value,10f,emerald,bold=true,spacing=0.13f))
        gap(10)
    }
    private fun metric(title: String, value: String, description: String, page: Int?) {
        card {
            addView(text(title.uppercase(Locale.US),10f,emerald,bold=true,spacing=0.1f))
            gap(7)
            addView(text(value,18f,pale,bold=true))
            gap(6)
            addView(text(description,12f,muted))
            if (page!=null) {
                gap(12)
                addView(outlineAction("OPEN  →") { currentTab=page; render() })
            }
        }
        gap(10)
    }
    private fun info(title: String, description: String) {
        val box = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(16),dp(15),dp(16),dp(15))
            background=rounded(Color.rgb(9, 31, 22),14,outline,1)
            addView(text(title,10f,emerald,bold=true,spacing=0.13f))
            addView(text(description,12f,muted).also { it.setPadding(0,dp(9),0,0) })
        }
        content.addView(box, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT))
    }
    private fun card(build: LinearLayout.() -> Unit) {
        val view = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(16),dp(16),dp(16),dp(16))
            background=rounded(cardBg,16,outline,1)
            build()
        }
        content.addView(view,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT))
    }
    private fun LinearLayout.gap(value:Int) {
        addView(View(this@MainActivity),LinearLayout.LayoutParams(dp(1),dp(value)))
    }
    private fun gap(value:Int) { content.gap(value) }
    private fun rowText(left:String,right:String): LinearLayout {
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        row.addView(text(left,11f,pale,true,spacing=0.11f), LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f))
        row.addView(text(right,9f,emerald,true))
        return row
    }
    private fun text(value:String,size:Float,color:Int,bold:Boolean=false,spacing:Float=0f):TextView = TextView(this).apply {
        text=value
        textSize=size
        setTextColor(color)
        includeFontPadding=true
        if(bold) setTypeface(typeface,Typeface.BOLD)
        letterSpacing=spacing
        setLineSpacing(dp(2).toFloat(),1f)
    }
    private fun chip(value:String,color:Int,back:Int):TextView=text(value,9f,color,true,0.09f).apply {
        setPadding(dp(10),dp(8),dp(10),dp(8))
        gravity=Gravity.CENTER
        background=rounded(back,10,outline,1)
    }
    private fun action(value:String,callback:()->Unit): TextView = text(value,11f,Color.BLACK,true,0.07f).apply {
        gravity=Gravity.CENTER
        setPadding(dp(14),dp(13),dp(14),dp(13))
        background=rounded(emerald,11,emerald,1)
        setOnClickListener { callback() }
        isClickable=true; isFocusable=true
    }
    private fun outlineAction(value:String,callback:()->Unit):TextView=text(value,11f,pale,true,0.07f).apply {
        gravity=Gravity.CENTER
        setPadding(dp(12),dp(11),dp(12),dp(11))
        background=rounded(forest,10,outline,1)
        setOnClickListener { callback() }
        isClickable=true; isFocusable=true
    }
    private fun rounded(color:Int,radius:Int,stroke:Int,width:Int)=GradientDrawable().apply {
        setColor(color); cornerRadius=dp(radius).toFloat(); setStroke(dp(width),stroke)
    }
    private fun dp(value:Int):Int=(resources.displayMetrics.density*value+0.5f).toInt()

    override fun onDestroy() {
        flipper.close()
        super.onDestroy()
    }
}
