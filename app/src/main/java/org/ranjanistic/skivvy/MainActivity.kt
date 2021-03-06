@file:Suppress("PropertyName")

package org.ranjanistic.skivvy

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.*
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import org.ranjanistic.skivvy.R.drawable.*
import org.ranjanistic.skivvy.R.string.*
import org.ranjanistic.skivvy.manager.*
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.Executor
import kotlin.ConcurrentModificationException
import kotlin.collections.ArrayList

@ExperimentalStdlibApi
open class MainActivity : AppCompatActivity() {

    lateinit var skivvy: Skivvy

    //TODO: Create onBoarding, with first preference as theme choice
    //TODO: lock screen activity, its brightness, charging status, incoming notifications on lock screen view, charging view
    //TODO: widget for actions (calculations first, or a calculator widget)
    private lateinit var outputText: TextView
    private lateinit var inputText: TextView
    private lateinit var greet: TextView
    private lateinit var feedback: TextView

    private data class Animations(
        var fallDown: Animation,
        var riseUp: Animation,
        var waveDamped: Animation,
        var zoomInOutRotate: Animation,
        var focusDefocusRotate: Animation,
        var focusRotate: Animation,
        var zoomInRotate: Animation,
        var fadeOnFadeOff: Animation,
        var fadeOn: Animation,
        var fadeOff: Animation,
        var fadeOnFast: Animation,
        var extendDownStartSetup: Animation,
        var slideToRight: Animation,
        var rotateClock: Animation,
        var revolveRotateToLeft: Animation
    )

    private lateinit var anim: Animations

    companion object {
        private val CALLTASK = 0
        private val NOTIFTASK = 1
        private val CALCUTASK = 2
        private val PERMITASK = 3
    }

    private var lastTxt: String? = null
    private var tasksOngoing: ArrayList<Boolean> = arrayListOf(false, false, false, false)
    private fun anyTaskRunning(): Boolean {
        return tasksOngoing[CALLTASK] ||
                tasksOngoing[NOTIFTASK] ||
                tasksOngoing[CALCUTASK] ||
                tasksOngoing[PERMITASK]
    }

    private lateinit var receiver: ImageButton
    private lateinit var setting: ImageButton
    private lateinit var loading: ImageView
    private lateinit var backfall: ImageView
    private lateinit var icon: ImageView
    private var nothing = String()
    private var space = String()
    private var txt: String? = null
    private lateinit var context: Context
    private lateinit var calculation: CalculationManager
    private lateinit var packages: PackageDataManager
    private lateinit var audioManager: AudioManager
    private lateinit var wifiManager: WifiManager
    private lateinit var recognitionIntent: Intent
    private var input = InputSpeechManager()
    private var contact = ContactModel()
    private var temp: TempDataManager = TempDataManager()
    private lateinit var feature: SystemFeatureManager

    override fun onCreate(savedInstanceState: Bundle?) {
        skivvy = this.application as Skivvy
        setTheme(skivvy.getThemeState())
        super.onCreate(savedInstanceState)
        context = this
        skivvy.isHomePageRunning = true
        setContentView(R.layout.activity_homescreen)
        window.statusBarColor = when (skivvy.shouldFullScreen()) {
            true -> ContextCompat.getColor(context, android.R.color.transparent)
            else -> getSysBarColorByTheme(context, true)
        }
        window.navigationBarColor = when (skivvy.shouldFullScreen()) {
            true -> window.statusBarColor
            else -> getSysBarColorByTheme(context, false)
        }
        hideSysUI()
        setViewAndDefaults()
        loadDefaultAnimations()
        resetVariables()
        setListeners()
        initiateRecognitionIntent()
        skivvy.tts = TextToSpeech(this, TextToSpeech.OnInitListener {
            if (it == TextToSpeech.SUCCESS) {
                skivvy.tts?.language = skivvy.locale
            } else speakOut(getString(output_error))
        })
        initialView()
        setOutput(getString(im_ready))
        if (skivvy.shouldListenStartup())
            startVoiceRecIntent(skivvy.CODE_SPEECH_RECORD)
        else {
            inputText.text = getString(tap_the_button)
        }
    }

    private fun initiateRecognitionIntent() {
        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, skivvy.locale)
    }

    private fun getSysBarColorByTheme(
        context: Context,
        isStatus: Boolean
    ): Int {
        return ContextCompat.getColor(
            context,
            when (skivvy.getThemeState()) {
                R.style.LightTheme -> {
                    if (isStatus) R.color.dull_white
                    else R.color.pitch_white
                }
                R.style.BlackTheme -> {
                    if (isStatus) R.color.pitch_black
                    else R.color.dull_white
                }
                R.style.BlueTheme -> {
                    if (isStatus) R.color.blue
                    else R.color.pitch_white
                }
                else -> {
                    if (isStatus) R.color.dead_blue
                    else R.color.charcoal
                }
            }
        )
    }

    private fun setViewAndDefaults() {
        feature = SystemFeatureManager(skivvy)
        space = skivvy.space
        nothing = skivvy.nothing
        setting = findViewById(R.id.setting)
        outputText = findViewById(R.id.textOutput)
        //TODO: Handy view
        //setHandyView()
        inputText = findViewById(R.id.textInput)
        receiver = findViewById(R.id.receiverBtn)
        feedback = findViewById(R.id.feedbackOutput)
        loading = findViewById(R.id.loader)
        icon = findViewById(R.id.actionIcon)
        greet = findViewById(R.id.greeting)
        backfall = findViewById(R.id.backdrop)
        packages = skivvy.packageDataManager
        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        calculation = CalculationManager(skivvy)
        greet.text = getString(app_name)
        greet.setCompoundDrawablesWithIntrinsicBounds(
            if (skivvy.isColorfulSkivvy()) dots_in_circle_colorful
            else dots_in_circle
            , 0, 0, 0
        )
        if (skivvy.isLocaleHindi()) {
            greet.typeface = Typeface.DEFAULT
            inputText.typeface = Typeface.DEFAULT
        }
    }

    private fun loadDefaultAnimations() {
        anim = Animations(
            AnimationUtils.loadAnimation(context, R.anim.fall_back),
            AnimationUtils.loadAnimation(context, R.anim.rise_back),
            AnimationUtils.loadAnimation(context, R.anim.bubble_wave),
            AnimationUtils.loadAnimation(context, R.anim.rotate_emerge_demerge),
            AnimationUtils.loadAnimation(context, R.anim.rotate_focus),
            AnimationUtils.loadAnimation(context, R.anim.rotate_slow),
            AnimationUtils.loadAnimation(context, R.anim.rotate_exit),
            AnimationUtils.loadAnimation(context, R.anim.fade),
            AnimationUtils.loadAnimation(context, R.anim.fade_on),
            AnimationUtils.loadAnimation(context, R.anim.fade_off),
            AnimationUtils.loadAnimation(context, R.anim.fade_on_quick),
            AnimationUtils.loadAnimation(context, R.anim.extend_back),
            AnimationUtils.loadAnimation(context, R.anim.slide_right),
            AnimationUtils.loadAnimation(context, R.anim.rotate_clock),
            AnimationUtils.loadAnimation(context, R.anim.pill_slide_left)
        )
        backfall.startAnimation(anim.fallDown)
        receiver.startAnimation(anim.waveDamped)
        greet.startAnimation(anim.waveDamped)
        setting.startAnimation(anim.revolveRotateToLeft)
    }

    private fun startSettingAnimate() {
        setting.startAnimation(anim.slideToRight)
        backfall.startAnimation(anim.extendDownStartSetup)
        greet.startAnimation(anim.fadeOff)
        outputText.startAnimation(anim.fadeOff)
        receiver.startAnimation(anim.fadeOff)
        inputText.startAnimation(anim.fadeOff)
    }

    private fun finishAnimate() {
        loading.startAnimation(anim.zoomInRotate)
        setting.startAnimation(anim.slideToRight)
        backfall.startAnimation(AnimationUtils.loadAnimation(context, R.anim.extend_back))
        greet.startAnimation(anim.fadeOff)
        outputText.startAnimation(anim.fadeOff)
        receiver.startAnimation(anim.fadeOff)
        inputText.startAnimation(anim.fadeOff)
    }

    private fun startResumeAnimate() {
        greet.text = getString(app_name)
        greet.startAnimation(anim.fadeOn)
        receiver.startAnimation(anim.fadeOn)
        receiver.visibility = View.VISIBLE
        greet.startAnimation(anim.waveDamped)
        receiver.startAnimation(anim.waveDamped)
        setting.startAnimation(anim.revolveRotateToLeft)
        backfall.startAnimation(anim.riseUp)
        outputText.startAnimation(anim.fadeOn)
        inputText.startAnimation(anim.fadeOn)
    }

    private fun setListeners() {
        callStateListener()
        greet.setOnClickListener {
            it.startAnimation(anim.waveDamped)
            speakOut(getString(i_am) + getString(app_name))
        }
        setting.setOnClickListener {
            startSettingAnimate()
        }
        receiver.setOnClickListener {
            initialView(anyTaskRunning())
            if (!anyTaskRunning()) {
                speakOut(nothing, skivvy.CODE_SPEECH_RECORD, parallelReceiver = true)
                setFeedback(nothing)
                setOutput(getString(im_ready))
            } else {
                startVoiceRecIntent(skivvy.CODE_SPEECH_RECORD)
            }
        }
        anim.extendDownStartSetup.setAnimationListener(object :
            Animation.AnimationListener {
            override fun onAnimationEnd(p0: Animation?) {
                startActivity(Intent(context, Setup::class.java))
                overridePendingTransition(R.anim.fade_on, R.anim.fade_off)
            }

            override fun onAnimationRepeat(p0: Animation?) {}
            override fun onAnimationStart(p0: Animation?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        hideSysUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus)
            hideSysUI()
    }

    override fun onStart() {
        super.onStart()
        initiateRecognitionIntent()
        this.registerReceiver(this.mBatInfoReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        this.registerReceiver(this.mNotificationReceiver, IntentFilter(skivvy.actionNotification))
        if (isNotificationServiceRunning() && skivvy.showNotifications()) {
            startService(Intent(this, NotificationWatcher::class.java))
        }
        setTheme(skivvy.getThemeState())
        lightSensor()
    }

    private fun hideSysUI() {
        if (skivvy.shouldFullScreen()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            window.decorView.apply {
                systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        )
            }
        } else return
    }

    override fun onRestart() {
        super.onRestart()
        hideSysUI()
        startResumeAnimate()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        tasksOngoing[PERMITASK] = false
        when (requestCode) {
            skivvy.CODE_ALL_PERMISSIONS -> {
                speakOut(
                    if (skivvy.hasPermissions(context)) getString(have_all_permits)
                    else getString(all_permissions_not_granted)
                )
            }
            skivvy.CODE_CALL_REQUEST -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    when {
                        contact.phoneList != null -> {
                            speakOut(
                                getString(should_i_call) + "${contact.displayName}?",
                                skivvy.CODE_CALL_CONF
                            )
                        }
                        temp.getPhone() != null -> {
                            speakOut(
                                getString(should_i_call) + "${temp.getPhone()}?",
                                skivvy.CODE_CALL_CONF
                            )
                        }
                        else -> {
                            speakOut(getString(null_variable_error))
                        }
                    }
                } else {
                    temp.setPhone(null)
                    errorView()
                    speakOut(getString(call_permit_denied))
                }
            }
            skivvy.CODE_CALL_LOG_REQUEST -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ArrangeViaLogs().execute(contact.displayName)
                } else {
                    contact.phoneList?.let {
                        speakOut(
                            getString(should_i_call) + contact.displayName +
                                    " at ${it[temp.getPhoneIndex()]}?",
                            skivvy.CODE_CALL_CONF
                        )
                    }
                }
            }
            skivvy.CODE_ANSWER_CALL -> {
                txt?.let { manageIncomingCall(it) }
            }
            skivvy.CODE_SMS_REQUEST -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    when {
                        contact.phoneList != null -> {
                            speakOut(
                                getString(should_i_text) + "${contact.displayName}?",
                                skivvy.CODE_SMS_CONF
                            )
                        }
                        temp.getPhone() != null -> {
                            speakOut(
                                getString(should_i_text) + "${temp.getPhone()}?",
                                skivvy.CODE_SMS_CONF
                            )
                        }
                        else -> {
                            speakOut(getString(null_variable_error))
                        }
                    }
                } else {
                    temp.setPhone(null)
                    errorView()
                    speakOut(getString(sms_permit_denied))
                }
            }

            skivvy.CODE_STORAGE_REQUEST -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takeScreenshot()
                } else {
                    errorView()
                    speakOut(getString(storage_permission_denied))
                }
            }
            skivvy.CODE_CONTACTS_REQUEST -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (msgCode.message != nothing && msgCode.code != 0) {
                        SearchContact().execute(msgCode)
                    } else {
                        speakOut(getString(null_variable_error))
                    }
                } else {
                    errorView()
                    speakOut(getString(contact_permission_denied))
                }
            }
        }
    }

    private fun isAcceptingCall(response: String): Boolean? {
        val ignorance = arrayOf("ignore", "standby")
        val acceptance = arrayOf("pick up", "pickup", "answer", "accept")
        val abortions = arrayOf("abort", "discard", "cut the call", "call cut", "reject")
        return when {
            input.containsString(
                input.removeBeforeLastStringsIn(
                    response, arrayOf(ignorance, abortions)
                ), arrayOf(acceptance)
            ) -> {
                Log.d("isAcception", "true")
                true
            }
            input.containsString(
                input.removeBeforeLastStringsIn(
                    response, arrayOf(ignorance, acceptance)
                ), arrayOf(abortions)
            ) -> {
                Log.d("isAcception", "false")
                false
            }
            input.containsString(
                input.removeBeforeLastStringsIn(
                    response, arrayOf(abortions, acceptance)
                ), arrayOf(ignorance)
            ) -> {
                Log.d("isAcception", "null")
                null
            }
            else -> {
                Log.d("isAcception", "nullElse")
                null
            }
        }
    }

    private fun manageIncomingCall(response: String) {
        when (isAcceptingCall(response)) {
            null -> {        //ignoring
                return
            }
            else -> {
                if (!isAcceptingCall(response)?.let {
                        feature.respondToCall(
                            this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager,
                            this.getSystemService(Context.TELECOM_SERVICE) as TelecomManager,
                            it
                        )
                    }!!) {
                    speakOut("Can't control calling on your phone.", isFeedback = true)
                }
            }
        }
    }

    private fun setFeedback(text: String?, isNotOngoing: Boolean = true) {
        if (isNotOngoing) {
            feedback.startAnimation(anim.fadeOnFast)
            feedback.text = text
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                finishAnimate()
            }
            KeyEvent.KEYCODE_HEADSETHOOK -> {
                initialView(anyTaskRunning())
                if (!anyTaskRunning()) {
                    speakOut(nothing, skivvy.CODE_SPEECH_RECORD, parallelReceiver = true)
                    setFeedback(nothing)
                    setOutput(getString(im_ready))
                } else {
                    startVoiceRecIntent(skivvy.CODE_SPEECH_RECORD)
                }
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                skivvy.setVoicePreference(normalizeVolume = false)
                if (!anyTaskRunning()) {
                    setFeedback(
                        getString(volume_raised_to_) + "${feature.getMediaVolume(audioManager)}" + getString(
                            percent
                        )
                    )
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                skivvy.setVoicePreference(normalizeVolume = false)
                if (!anyTaskRunning()) {
                    setFeedback(
                        getString(volume_low_to_) + "${feature.getMediaVolume(audioManager)}" + getString(
                            percent
                        )
                    )
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    //TODO: when output is 'okay', continue listening if preferred.
    //TODO: check for other commands in agreements or denial inputs
    private fun inGlobalCommands(text: String?): Boolean {
        try {
            if (!directActions(text!!)) {     //the commands which require intrusion of other services of device, such as calling or SMS functionality.
                if (!computerOps(text)) {       //the commands which are mathematical in nature, performed directly by Skivvy itself.
                    tasksOngoing[CALCUTASK] = false
                    if (!respondToCommand(text)) {        //the commands which skivvy can respond promptly and execute directly by itself.
                        if (!appOptions(text))        //the commands requiring other apps or intents on the device to be opened, leaving Skivvy in the background.
                            throw IllegalArgumentException()
                    } else {
                        throw ConcurrentModificationException()
                    }
                } else {
                    tasksOngoing[CALCUTASK] = true
                    throw ConcurrentModificationException()
                }
            }
        } catch (e: IllegalArgumentException) {
            return false
        } catch (e: ConcurrentModificationException) {
            //If a permission task is ongoing, always false so that input window doesn't show up.
            if (skivvy.shouldContinueInput() && !tasksOngoing[PERMITASK])
                startVoiceRecIntent(skivvy.CODE_SPEECH_RECORD)
        }
        return true
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        var result: ArrayList<String> = ArrayList(1)
        if (!skivvy.nonVocalRequestCodes.contains(requestCode)) {
            tasksOngoing[PERMITASK] = false
            if (data == null || data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.get(0).toString().toLowerCase(skivvy.locale) == nothing
            ) {
                initialView(anyTaskRunning())
                if (!anyTaskRunning()) {
                    speakOut(getString(no_input))
                }
                return
            } else {
                data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.let { result = it }
                inputText.text = result[0]
                txt = result[0].toLowerCase(skivvy.locale)
            }
        }
        when (requestCode) {
            skivvy.CODE_SPEECH_RECORD -> {
                if (lastTxt != null)
                    txt = lastTxt + space + txt
                inputText.text = txt
                if (!inGlobalCommands(txt)) {
                    errorView()
                    speakOut(getString(recognize_error))
                }
            }
            skivvy.CODE_VOICE_AUTH_CONFIRM -> {
                if (txt != skivvy.getVoiceKeyPhrase()) {
                    if (skivvy.getBiometricStatus()) {
                        speakOut(
                            getString(vocal_auth_failed) + getString(
                                need_physical_verification
                            )
                        )
                        authStateAction(skivvy.CODE_VOICE_AUTH_CONFIRM)
                        biometricPrompt.authenticate(promptInfo)
                    } else {
                        speakOut(getString(vocal_auth_failed))
                    }
                } else {
                    skivvy.setSecurityPref(vocalAuthOn = false)
                    speakOut(getString(vocal_auth_disabled))
                }
            }
            skivvy.CODE_BIOMETRIC_CONFIRM -> {
                if (txt != skivvy.getVoiceKeyPhrase()) {
                    if (skivvy.getBiometricStatus()) {
                        speakOut(
                            getString(vocal_auth_failed) + getString(
                                need_physical_verification
                            )
                        )
                        authStateAction(skivvy.CODE_BIOMETRIC_CONFIRM)
                        biometricPrompt.authenticate(promptInfo)
                    } else {
                        errorView()
                        speakOut(getString(vocal_auth_failed))
                    }
                } else {
                    skivvy.setSecurityPref(biometricOn = false)
                    speakOut(getString(biometric_is_off))
                }
            }
            skivvy.CODE_VOLUME_CONFIRM -> {
                when (isCooperative(txt!!)) {
                    true -> {
                        skivvy.setVoicePreference(normalizeVolume = false)
                        feature.setMediaVolume(
                            temp.getVolumePercent(),
                            audioManager
                        )
                        speakOut(
                            getString(volume_at_) + " ${temp.getVolumePercent()
                                .toInt()}" + getString(percent)
                        )
                    }
                    false -> {
                        setOutput(getString(okay))
                    }
                    else -> {
                        if (isDisruptive(txt!!)) {
                            initialView()
                            speakOut(getString(okay))
                        } else {
                            speakOut(
                                "Are you sure about the harmful" + "${temp.getVolumePercent()}% volume?",
                                skivvy.CODE_VOLUME_CONFIRM
                            )
                        }
                    }
                }
            }
            skivvy.CODE_APP_CONF -> {
                when (isInitiative(txt!!)) {
                    true -> {
                        successView(packages.getPackageIcon(temp.getPackageIndex()))
                        packages.getPackageAppName(temp.getPackageIndex())?.let {
                            speakOut(
                                getString(opening) + it.capitalize(skivvy.locale)
                            )
                        }
                        startActivity(Intent(packages.getPackageIntent(temp.getPackageIndex())))
                    }
                    false -> {
                        initialView()
                        speakOut(getString(okay))
                    }
                    else -> {
                        if (!inGlobalCommands(txt!!)) {
                            speakOut(
                                getString(recognize_error) + "\n" + getString(do_u_want_open) + "${packages.getPackageAppName(
                                    temp.getPackageIndex()
                                )!!.capitalize(skivvy.locale)}?",
                                skivvy.CODE_APP_CONF
                            )
                        }
                    }
                }
            }
            skivvy.CODE_LOCATION_SERVICE -> {
                val locationManager =
                    applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    successView(getDrawable(ic_location_pointer))
                    speakOut(getString(gps_enabled))
                } else {
                    errorView()
                    speakOut(getString(gps_is_off))
                }
            }
            skivvy.CODE_DEVICE_ADMIN -> {
                if (resultCode == Activity.RESULT_OK) {
                    deviceLockOps()
                } else {
                    errorView()
                    speakOut(getString(device_admin_failure))
                }
            }
            skivvy.CODE_SYSTEM_SETTINGS -> {
                if (!Settings.System.canWrite(context)) {
                    speakOut(getString(write_settings_permit_denied))
                    errorView()
                } else {
                    speakOut(getString(say_again), skivvy.CODE_SPEECH_RECORD)
                }
            }
            skivvy.CODE_CALL_CONF -> {

                when (isCooperative(txt!!)) {
                    true -> {
                        if (!skivvy.hasThisPermission(context, skivvy.CODE_CALL_REQUEST)) {
                            speakOut(getString(require_physical_permission))
                            requestThisPermission(skivvy.CODE_CALL_REQUEST)
                        } else {
                            if (temp.getContactPresence()) {
                                successView(null)
                                callingOps(
                                    contact.phoneList!![temp.getPhoneIndex()],
                                    contact.displayName
                                )
                            } else {
                                successView(getDrawable(ic_phone_dialer))
                                callingOps(temp.getPhone())
                                temp.setPhone(null)
                            }
                        }
                    }
                    false -> {
                        temp.setPhoneIndex(temp.getPhoneIndex() + 1)
                        if (temp.getContactPresence() && temp.getPhoneIndex() < contact.phoneList!!.size) {
                            speakOut(
                                getLocalisedString(
                                    getString(at) + "${contact.phoneList!![temp.getPhoneIndex()]}?",
                                    "${contact.phoneList!![temp.getPhoneIndex()]}" + getString(
                                        at
                                    ) + "?"
                                ),
                                skivvy.CODE_CALL_CONF
                            )
                        } else {
                            initialView()
                            speakOut(getString(okay))
                        }
                    }
                    else -> {
                        if (isDisruptive(txt!!)) {
                            initialView()
                            speakOut(getString(okay))
                        } else {
                            if (!inGlobalCommands(txt!!)) {
                                if (temp.getContactPresence()) {
                                    speakOut(
                                        getString(recognize_error) + "\n" + getString(
                                            should_i_call
                                        ) + contact.displayName + space +
                                                getLocalisedString(
                                                    "at ${contact.phoneList!![temp.getPhoneIndex()]}?",
                                                    "को ${contact.phoneList!![temp.getPhoneIndex()]}${getString(
                                                        at
                                                    )}?"
                                                ),
                                        skivvy.CODE_CALL_CONF
                                    )
                                } else {
                                    speakOut(
                                        getString(recognize_error) + "\n" + getString(
                                            should_i_call
                                        ) + "${temp.getPhone()}" + getLocalisedString(
                                            "?",
                                            "${getString(at)}?"
                                        ),
                                        skivvy.CODE_CALL_CONF
                                    )
                                }
                            }
                        }
                    }
                }
            }
            skivvy.CODE_ANSWER_CALL -> {
                if (txt != nothing && txt != null) {
                    if (!skivvy.hasThisPermission(context, skivvy.CODE_ANSWER_CALL)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            requestThisPermission(skivvy.CODE_ANSWER_CALL)
                            //TODO: picking calls
                        } else speakOut("I can't pick calls on this device.")
                    } else {
                        manageIncomingCall(txt!!)
                    }
                }
            }
            skivvy.CODE_EMAIL_CONTENT -> {
                txt = result[0]
                    .toLowerCase(skivvy.locale)
                if (txt != null) {
                    if (input.isStringInArray(
                            txt!!,
                            arrayOf(resources.getStringArray(R.array.disruptions))
                        )
                    ) {
                        initialView()
                        speakOut(getString(okay))
                    } else {
                        if (temp.getEmailSubject() == null) {
                            temp.setEmailSubject(txt)
                            setFeedback("Subject: ${temp.getEmailSubject()}")
                            speakOut(
                                getString(subject_added) + "\n" + getString(
                                    what_is_body
                                ),
                                skivvy.CODE_EMAIL_CONTENT
                            )
                        } else if (temp.getEmailBody() == null) {
                            temp.setEmailBody(txt)
                            setFeedback("Subject: ${temp.getEmailSubject()}\nBody: ${temp.getEmailBody()}")
                            if (temp.getContactPresence()) {
                                //not displaying total emails because of email content
                                speakOut(
                                    getString(body_added) +
                                            getString(should_i_email) + "${contact.displayName}${space}" +
                                            getLocalisedString(
                                                "at ${contact.emailList!![temp.getEmailIndex()]}?",
                                                "को ${contact.emailList!![temp.getEmailIndex()]}${getString(
                                                    at
                                                )}?"
                                            ),
                                    skivvy.CODE_EMAIL_CONF
                                )
                            } else {
                                speakOut(
                                    getString(body_added) + "\n" +
                                            getString(should_i_email) + "${temp.getEmail()}" + getLocalisedString(
                                        "?",
                                        "${getString(at)}?"
                                    ),
                                    skivvy.CODE_EMAIL_CONF
                                )
                            }
                        }
                    }
                } else if (txt == null) {
                    if (temp.getEmailSubject() == null) {
                        speakOut(
                            getString(recognize_error) + "\n" + getString(
                                what_is_subject
                            ),
                            skivvy.CODE_EMAIL_CONTENT
                        )
                    } else if (temp.getEmailBody() == null) {
                        speakOut(
                            getString(recognize_error) + "\n" + getString(
                                what_is_body
                            ),
                            skivvy.CODE_EMAIL_CONTENT
                        )
                    }
                }
            }
            skivvy.CODE_EMAIL_CONF -> {
                txt = result[0]
                    .toLowerCase(skivvy.locale)
                when (isCooperative(txt!!)) {
                    true -> {
                        successView(null)
                        speakOut(getString(preparing_email))
                        if (temp.getContactPresence()) {
                            emailingOps(
                                contact.emailList!![temp.getEmailIndex()],
                                temp.getEmailSubject(),
                                temp.getEmailBody()
                            )
                        } else {
                            emailingOps(
                                temp.getEmail(),
                                temp.getEmailSubject(),
                                temp.getEmailBody()
                            )
                        }
                    }
                    false -> {
                        temp.setEmailIndex(temp.getEmailIndex() + 1)
                        if (temp.getContactPresence() && temp.getEmailIndex() < contact.emailList!!.size) {
                            speakOut(
                                getLocalisedString(
                                    "${getString(at)}${contact.emailList!![temp.getEmailIndex()]}?",
                                    "${contact.emailList!![temp.getEmailIndex()]}${getString(
                                        at
                                    )}?"
                                )
                                ,
                                skivvy.CODE_EMAIL_CONF
                            )
                        } else {
                            initialView()
                            speakOut(getString(okay))
                        }
                    }
                    else -> {
                        if (isDisruptive(txt!!)) {
                            initialView()
                            speakOut(getString(okay))
                        } else {
                            if (!inGlobalCommands(txt!!)) {
                                if (temp.getContactPresence()) {
                                    speakOut(
                                        getString(recognize_error) + "\n" +
                                                getString(should_i_email) + "${contact.displayName}${space}" +
                                                getLocalisedString(
                                                    getString(at) + "\n${contact.emailList!![temp.getEmailIndex()]}?",
                                                    "को \n${contact.emailList!![temp.getEmailIndex()]}${getString(
                                                        at
                                                    )}?"
                                                ),
                                        skivvy.CODE_EMAIL_CONF
                                    )
                                } else {
                                    speakOut(
                                        getString(recognize_error) + "\n" +
                                                getString(should_i_email) + "${temp.getEmail()}" + getLocalisedString(
                                            "?",
                                            "${getString(at)}?"
                                        ),
                                        skivvy.CODE_EMAIL_CONF
                                    )
                                }
                            }
                        }
                    }
                }
            }

            skivvy.CODE_TEXT_MESSAGE_BODY -> {
                txt = result[0]
                    .toLowerCase(skivvy.locale)
                when (isCooperative(txt!!)) {
                    false -> {
                        initialView()
                        speakOut(getString(okay))
                    }
                    else -> {
                        if (isDisruptive(txt!!)) {
                            initialView()
                            speakOut(getString(okay))
                        } else if (txt != null) {
                            waitingView()
                            temp.setTextBody(txt)
                            if (temp.getContactPresence()) {
                                speakOut(
                                    getString(should_i_text) + contact.displayName + space +
                                            getLocalisedString(
                                                "at${contact.phoneList!![temp.getPhoneIndex()]}",
                                                "को ${contact.phoneList!![temp.getPhoneIndex()]}" + getString(
                                                    at
                                                )
                                            ) + getString(via_sms) + "?",
                                    skivvy.CODE_SMS_CONF
                                )
                            } else {
                                speakOut(
                                    getString(should_i_text) + "${temp.getPhone()}" + getLocalisedString(
                                        nothing,
                                        "${getString(at)}$space"
                                    ) + getString(
                                        via_sms
                                    ) + "?",
                                    skivvy.CODE_SMS_CONF
                                )
                            }
                        } else {
                            speakOut(
                                getString(recognize_error) + getString(what_is_message),
                                skivvy.CODE_TEXT_MESSAGE_BODY
                            )
                        }
                    }
                }
            }

            skivvy.CODE_SMS_CONF -> {
                txt = result[0]
                    .toLowerCase(skivvy.locale)
                when (isCooperative(txt!!)) {
                    true -> {
                        if (!skivvy.hasThisPermission(context, skivvy.CODE_SMS_REQUEST)) {
                            speakOut(getString(require_physical_permission))
                            requestThisPermission(skivvy.CODE_SMS_REQUEST)
                        } else {
                            if (temp.getContactPresence()) {
                                speakOut(getString(sending_sms_at) + "${contact.phoneList!![temp.getPhoneIndex()]}")
                                textMessageOps(
                                    contact.phoneList!![temp.getPhoneIndex()]!!,
                                    temp.getTextBody()!!,
                                    skivvy.CODE_SMS_CONF
                                )
                            } else if (temp.getPhone() != null) {
                                speakOut(getString(sending_sms_at) + "${temp.getPhone()}")
                                textMessageOps(
                                    temp.getPhone()!!,
                                    temp.getTextBody()!!,
                                    skivvy.CODE_SMS_CONF
                                )
                            }
                        }
                    }
                    false -> {
                        temp.setPhoneIndex(temp.getPhoneIndex() + 1)
                        if (temp.getContactPresence() && temp.getPhoneIndex() < contact.phoneList!!.size && !resources.getStringArray(
                                R.array.disruptions
                            ).contains(txt)
                        ) {
                            speakOut(
                                getLocalisedString(
                                    getString(at) + "${contact.phoneList!![temp.getPhoneIndex()]}?",
                                    "${contact.phoneList!![temp.getPhoneIndex()]}" + getString(
                                        at
                                    ) + "?"
                                ),
                                skivvy.CODE_SMS_CONF
                            )
                        } else {
                            initialView()
                            speakOut(getString(okay))
                        }
                    }
                    else -> {
                        if (isDisruptive(txt!!)) {
                            initialView()
                            speakOut(getString(okay))
                        } else {
                            if (!inGlobalCommands(txt!!)) {
                                if (temp.getContactPresence()) {
                                    speakOut(
                                        getString(should_i_text) + contact.displayName + space +
                                                getLocalisedString(
                                                    getString(at) + "${contact.phoneList!![temp.getPhoneIndex()]}",
                                                    "को ${contact.phoneList!![temp.getPhoneIndex()]}" + getString(
                                                        at
                                                    )
                                                ) + getString(via_sms) + "?",
                                        skivvy.CODE_SMS_CONF
                                    )
                                } else {
                                    speakOut(
                                        getString(should_i_text) + "${temp.getPhone()}" + getLocalisedString(
                                            nothing,
                                            "${getString(at)}$space"
                                        ) + getString(
                                            via_sms
                                        ) + "?",
                                        skivvy.CODE_SMS_CONF
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setBrightness(value: Int) {
        if (feature.getSystemBrightness(skivvy.cResolver) == null) {        //if brightness setting not found
            speakOut(getString(brightness_inaccessible))
        } else {
            Settings.System.putInt(
                skivvy.cResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                value
            )
            val layoutparams: WindowManager.LayoutParams = window.attributes
            layoutparams.screenBrightness = value / 255.toFloat()
            window.attributes = layoutparams
        }
    }

    private fun manageLogBase(action: String) {
        if (action.contains(skivvy.numberPattern)) {
            val base = action.replace(skivvy.nonNumeralPattern, nothing)
            if (base.toFloat() == 0F)
                speakOut(getString(invalid_log_base))
            else {
                skivvy.setMathsPref(logBase = base.toFloat().toInt())
                speakOut("$base is the log base.")
            }
        } else {
            speakOut("${skivvy.getLogBase()} is the log base.")
        }
    }

    private fun manageVocalAuth(action: String) {
        when {
            action.contains("enable") -> {
                if (!skivvy.getPhraseKeyStatus()) {
                    if (skivvy.getVoiceKeyPhrase() != null) {
                        skivvy.setSecurityPref(vocalAuthOn = true)
                        speakOut(getString(vocal_auth_enabled))
                    } else {
                        speakOut(getString(vocal_auth_unset))
                        startActivity(Intent(context, Setup::class.java))
                    }
                } else {
                    speakOut(getString(vocal_auth_already_on))
                }
            }
            action.contains("disable") -> {
                if (!skivvy.getPhraseKeyStatus()) {
                    speakOut(getString(vocal_auth_already_off))
                } else {
                    speakOut(
                        getString(get_passphrase_text),
                        skivvy.CODE_VOICE_AUTH_CONFIRM
                    )
                }
            }
            else -> {
                if (action.replace("voice authentication", nothing).trim() == nothing) {
                    lastTxt = action
                    speakOut(
                        "Vocal authentication what?",
                        skivvy.CODE_SPEECH_RECORD
                    )
                }
            }
        }
    }

    private fun manageBiometrics(action: String) {
        val enables = getArray(R.array.initiators)
        val disables = getArray(R.array.finishers)
        val actions = arrayOf(enables, disables)
        when (input.indexOfFinallySaidArray(action, actions).index) {
            actions.indexOf(enables) -> {
                if (skivvy.getBiometricStatus()) {
                    speakOut(getString(biometric_already_on))
                } else {
                    skivvy.setSecurityPref(biometricOn = true)
                    if (skivvy.getBiometricStatus()) speakOut(
                        getString(
                            biometric_on
                        )
                    )
                    else speakOut(getString(biometric_enable_error))
                }
            }
            actions.indexOf(disables) -> {
                if (!skivvy.getBiometricStatus()) {
                    speakOut(getString(biometric_already_off))
                } else {
                    if (skivvy.getPhraseKeyStatus()) {
                        speakOut(
                            getString(get_passphrase_text),
                            skivvy.CODE_BIOMETRIC_CONFIRM
                        )
                    } else {
                        speakOut(getString(physical_auth_request))
                        authStateAction(skivvy.CODE_BIOMETRIC_CONFIRM)
                        biometricPrompt.authenticate(promptInfo)
                    }
                }
            }
            else -> {
                if (action.replace("biometric", nothing).trim() == nothing) {
                    lastTxt = action
                    speakOut("Biometric what?", skivvy.CODE_SPEECH_RECORD)
                }
            }
        }
    }

    //TODO: Setup commands here
    val TAG = "indexCheck"
    private fun settingsCommand(text: String): Boolean {
        val enables = getArray(R.array.initiators)
        val disables = getArray(R.array.finishers)
        val setups = getArray(R.array.setup_list)
        val unmutes = arrayOf("unmute", "speak")
        val mutes = arrayOf("mute", "stop speaking")
        val logbases = arrayOf("log base", "base of log")
        val voiceAuths = arrayOf("voice auth", "vocal auth")
        val bioAuths = arrayOf("biometric", "fingerprint")
        val permits = arrayOf("permissions", "grant permissions")
        val setters = arrayOf("set", "change")
        val actions = arrayOf(
            setups, mutes, unmutes, logbases,
            voiceAuths, bioAuths, permits
        )
        if (!input.containsString(text, arrayOf(enables, disables, setters))) {
            Log.d(TAG, "settingsCommand: nodisableenable")
            return false
        } else {
            val finally = input.indexOfFinallySaidArray(text, actions).remaining
            when (input.indexOfFinallySaidArray(text, actions).index) {
                actions.indexOf(mutes) -> setSpeaking(false)
                actions.indexOf(unmutes) -> setSpeaking(true)
                actions.indexOf(logbases) -> {
                    Log.d(TAG, "settingsCommand: logbase")
                    manageLogBase(finally)
                }
                actions.indexOf(voiceAuths) -> {
                    Log.d(TAG, "settingsCommand: voiceauth")
                    manageVocalAuth(finally)
                }
                actions.indexOf(bioAuths) -> {
                    Log.d(TAG, "settingsCommand: biomentruc")
                    manageBiometrics(finally)
                }
                actions.indexOf(setups) -> {
                    Log.d(TAG, "settingsCommand: setup")
                    startActivity(Intent(context, Setup::class.java))
                    overridePendingTransition(R.anim.fade_on, R.anim.fade_off)
                }
                actions.indexOf(permits) -> {
                    Log.d(TAG, "settingsCommand: permissions")
                    if (!skivvy.hasPermissions(context)) {
                        speakOut(getString(need_all_permissions))
                        requestThisPermission(skivvy.CODE_ALL_PERMISSIONS)
                    } else {
                        speakOut(getString(have_all_permits))
                    }
                }
                else -> {
                    Log.d(TAG, "settingsCommand: nothing")
                    return false
                }
            }
        }
        return true
    }

    private fun inSystemToggles(text: String): Boolean {
        val bts = arrayOf("bluetooth")
        val wifis = getArray(R.array.wifi_list)
        val gpss = getArray(R.array.gps_list)
        val snaps = getArray(R.array.snap_list)
        val flashes = arrayOf("flashlight", "flash", "torch")
        val airplanes = arrayOf("airplane mode", "aeroplane mode", "aeroplane", "airplane")
        val actions = arrayOf(
            bts, wifis, gpss, snaps, flashes, airplanes
        )
        when (input.indexOfFinallySaidArray(text, actions).index) {
            actions.indexOf(flashes) -> {
                if (skivvy.isFlashAvailable()) {
                    val mCameraManager =
                        getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    if (text.contains("off")) {
                        feature.setFlashLight(mCameraManager, false)
                        speakOut(getString(flash_off))
                    } else {
                        if (!feature.setFlashLight(mCameraManager, false)) {
                            speakOut(getString(flash_access_error))
                        } else speakOut(getString(flash_on))
                    }
                } else {
                    speakOut("Flashlight not available")
                }
            }
            actions.indexOf(bts) -> {
                //the first function of Skivvy  ;-)
                if (text.contains("on")) {
                    val stat = feature.bluetooth(true)
                    if (stat == null) {
                        successView(getDrawable(ic_bluetooth))
                        speakOut(getString(bt_already_on))
                    } else {
                        successView(getDrawable(ic_bluetooth))
                        speakOut(getString(bt_on))
                    }
                } else if (text.contains("off")) {
                    val stat = feature.bluetooth(false)
                    if (stat == null) {
                        waitingView(getDrawable(ic_bluetooth))
                        speakOut(getString(bt_already_off))
                    } else {
                        waitingView(getDrawable(ic_bluetooth))
                        speakOut(getString(bt_off))
                    }
                } else {
                    val stat = feature.bluetooth(null)
                    if (stat!!) {
                        successView(getDrawable(ic_bluetooth))
                        speakOut(getString(bt_on))
                    } else {
                        waitingView(getDrawable(ic_bluetooth))
                        speakOut(getString(bt_off))
                    }
                }
            }
            actions.indexOf(wifis) -> {
                if (text.contains("on") || text.contains("enable")) {
                    when (feature.wirelessFidelity(true, wifiManager)) {
                        null -> {
                            waitingView(getDrawable(ic_wifi_connected))
                            speakOut(getString(wifi_already_on))
                        }
                        true -> {
                            successView(getDrawable(ic_wifi_connected))
                            speakOut(getString(wifi_on))
                        }
                    }
                } else if (text.contains("off")) {
                    when (feature.wirelessFidelity(false, wifiManager)) {
                        null -> {
                            waitingView(getDrawable(ic_wifi_disconnected))
                            speakOut(getString(wifi_already_off))
                        }
                        false -> {
                            successView(getDrawable(ic_wifi_disconnected))
                            speakOut(getString(wifi_off))
                        }
                    }
                } else {
                    if (feature.wirelessFidelity(null, wifiManager)!!) {
                        waitingView(getDrawable(ic_wifi_connected))
                        speakOut(getString(wifi_on))
                    } else {
                        waitingView(getDrawable(ic_wifi_disconnected))
                        speakOut(getString(wifi_off))
                    }
                }
            }
            actions.indexOf(gpss) -> {
                waitingView(getDrawable(ic_location_pointer))
                startActivityForResult(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                    skivvy.CODE_LOCATION_SERVICE
                )
            }
            //TODO: reset txt when new input has no enablers/disablers/on/off etc.
            actions.indexOf(snaps) -> {
                if (!skivvy.hasThisPermission(context, skivvy.CODE_STORAGE_REQUEST)) {
                    speakOut(getString(require_physical_permission))
                    requestThisPermission(skivvy.CODE_STORAGE_REQUEST)
                } else {
                    takeScreenshot()
                }
            }
            actions.indexOf(airplanes) -> {
                handleAirplaneMode(text)
            }
            else -> return false
        }
        return true
    }

    private fun handleAirplaneMode(action: String) {
        if (action.contains("on") || action.contains("enable")) {
            if (feature.isAirplaneModeEnabled()) {
                speakOut(getString(airplane_already_on))
            } else {
                speakOut(getString(airplane_mode_on))
                feature.setAirplaneMode(true)
            }
        } else if (action.contains("off") || action.contains("disable")) {
            if (!feature.isAirplaneModeEnabled()) {
                speakOut(getString(airplane_already_off))
            } else {
                speakOut(getString(airplane_mode_off))
                feature.setAirplaneMode(false)
            }
        }
    }

    private fun setSpeaking(isSpeaking: Boolean) {
        if (isSpeaking) {
            if (skivvy.getMuteStatus()) {
                skivvy.setVoicePreference(voiceMute = false)
                speakOut(getString(okay))
            } else {
                speakOut(getString(voice_output_on))
            }
        } else {
            skivvy.setVoicePreference(voiceMute = true)
            speakOut("Muted")
        }
    }

    private fun handleWebSearch(query: String) {
        speakOut("Searching for $query")
    }

    //actions invoking quick commands
    @ExperimentalStdlibApi
    private fun respondToCommand(text: String): Boolean {
        val locks = getArray(R.array.lock_list)
        val volumes = arrayOf("volume", "sound")
        val lights = arrayOf("brightness", "backlight")
        val batteries = arrayOf("battery", "power")
        val searching = arrayOf("search for", "search", "what is")
        val exits = arrayOf("bye", "exit", "terminate")
        val actions = arrayOf(locks, volumes, batteries, lights, exits, searching)

        val finally = input.indexOfFinallySaidArray(text, actions).remaining
        val extras = input.indexOfFinallySaidArray(text, actions).extras
        val index = input.indexOfFinallySaidArray(text, actions).index

        //The actions that may need a following argument apart from the command itself.
        fun nonBlanks(pos: Int): Boolean {
            when (pos) {
                actions.indexOf(volumes) -> volumeOps(extras)
                actions.indexOf(lights) -> brightnessOps(extras)
                actions.indexOf(searching) -> handleWebSearch(extras)
                else -> return false
            }
            return true
        }

        if (!extras.isBlank()) {
            if (!nonBlanks(index))
                if (!inSystemToggles(text))
                    return settingsCommand(text)
        } else {
            when (index) {
                actions.indexOf(batteries) -> {
                    speakOut(getString(batttery_at_) + "$batteryLevel" + getString(percent))
                }
                actions.indexOf(locks) -> deviceLockOps()
                actions.indexOf(exits) -> {
                    speakOut(getString(exit))
                    finishAnimate()
                    finish()
                }
                else -> {
                    if (!nonBlanks(index))
                        if (!inSystemToggles(text))
                            return settingsCommand(text)
                }
            }
        }
        return true
    }

    /**
     * To check if [rawExpression] is expression and operate calculation upon it further if it is, and present output to user if valid expression.
     * @param rawExpression The string of expression spoken by user, passed raw to be validated and evaluated if so.
     * @param reusing This parameter is for short term usage in the method, for if the [rawExpression] contains two operands and one
     * operator only (binary operation), then it will not undergo the further complex methods, and will be evaluated
     * and presented at the beginning (for faster results). This parameter is used to check if the expression already has undergone the
     * binary operation check at the beginning, and is needed to go through the
     * actual complex methods of expression evaluation. This is achieved via recursion in the method itself.
     * @return Boolean value if the operation was successful or not (for further skivvy operations).
     */
    private fun computerOps(
        rawExpression: String,
        reusing: Boolean = false
    ): Boolean {
        val expression = input.expressionize(rawExpression)
        if (!expression.contains(skivvy.numberPattern))
            return false
        if (expression.length == 3 && !reusing) {
            try {
                val res = calculation.operate(
                    expression[0].toString().toFloat(),
                    expression[1],
                    expression[2].toString().toFloat()
                )
                if (res != null) {
                    speakOut(calculation.returnValidResult(arrayOf(res.toString())))
                    return true
                }
                //Will be caught further, and no 'else' here as returning in 'if' itself.
                throw Exception()
            } catch (e: Exception) {
                //to be evaluated again but as a non binary operation.
                computerOps(rawExpression, true)
            }
        }
        val operatorsAndFunctionsArray = arrayOf(calculation.operators, calculation.mathFunctions)
        val operators = operatorsAndFunctionsArray.indexOf(calculation.operators)
        val functions = operatorsAndFunctionsArray.indexOf(calculation.mathFunctions)
        val operatorsAndFunctionsBoolean = arrayOf(
            arrayOfNulls<Boolean>(operatorsAndFunctionsArray[operators].size),
            arrayOfNulls(operatorsAndFunctionsArray[functions].size)
        )

        /**
         * Storing availability of all operators and functions in given expression,
         * to arrays of booleans.
         */
        var of = 0
        while (of < operatorsAndFunctionsBoolean.size) {
            var f = 0
            while (f < operatorsAndFunctionsBoolean[of].size) {
                operatorsAndFunctionsBoolean[of][f] =
                    expression.contains(operatorsAndFunctionsArray[of][f])
                ++f
            }
            ++of
        }

        if (!operatorsAndFunctionsBoolean[operators].contains(true)) {     //if no operators
            if (operatorsAndFunctionsBoolean[functions].contains(true)) {       //has a mathematical function
                try {
                    if (expression.contains(skivvy.numberPattern)) {
                        setFeedback(
                            calculation.formatExpression(arrayOf(expression)),
                            !anyTaskRunning() || tasksOngoing[CALCUTASK]
                        )
                        calculation.operateFuncWithConstant(expression)
                            ?.let { saveCalculationResult(it) }
                        if (calculation.handleExponentialTerm(getLastCalculationResult()) == nothing) {
                            throw IllegalArgumentException()
                        } else
                            speakOut(
                                calculation.formatToProperValue(
                                    calculation.handleExponentialTerm(
                                        getLastCalculationResult()
                                    )
                                )
                            )
                        return true
                    } else throw IllegalArgumentException()
                } catch (e: IllegalArgumentException) {
                    errorView()
                    speakOut(getString(invalid_expression))
                    return true
                }
            } else return false
        }

        val totalOps = calculation.totalOperatorsInExpression(expression)
        val segmented = calculation.segmentizeExpression(expression, 2 * totalOps + 1)
        if (totalOps == 0 || !calculation.isExpressionOperatable(expression) || segmented == null)
            return false

        var arrayOfExpression = segmented

        //operator in place validity check (odd indices), proper segmented expression check.
        var index = 0
        var operand = 0
        while (index < arrayOfExpression.size && operand < arrayOfExpression.size) {
            if (arrayOfExpression[index] != null && arrayOfExpression[operand] != null) {
                if (arrayOfExpression[operand]!!.contains(skivvy.nonNumeralPattern)
                    && !operatorsAndFunctionsBoolean[functions].contains(true)  //neither has any functions
                    && !arrayOfExpression[operand]!!.contains(".")        //nor does contains a decimal point
                ) {
                    return false
                }
            } else return false
            ++index
            operand += 2
        }

        //segmentized expression to user as feedback
        setFeedback(
            calculation.formatExpression(arrayOfExpression),
            !anyTaskRunning() || tasksOngoing[CALCUTASK]
        )

        if (operatorsAndFunctionsBoolean[functions].contains(true)) {      //If expression has mathematical functions
            val temp =
                calculation.evaluateFunctionsInExpressionArray(arrayOfExpression)
            if (temp == null) {
                return false
            } else {
                //all functions solved, now only operands and operators in arrayOfExpression
                arrayOfExpression = temp
            }
        }
        //if array contains invalid values
        if (!calculation.isExpressionArrayOnlyNumbersAndOperators(arrayOfExpression))
            return false
        else {
            saveCalculationResult(calculation.expressionCalculation(arrayOfExpression))
            speakOut(getLastCalculationResult())
        }
        return true
    }

    private fun deviceLockOps() {
        if (skivvy.deviceManager.isAdminActive(skivvy.compName)) {
            successView(getDrawable(ic_locked))
            speakOut(getString(screen_locked))
            skivvy.deviceManager.lockNow()
        } else {
            tasksOngoing[PERMITASK] = true
            waitingView(getDrawable(ic_locked))
            speakOut(getString(device_admin_request))
            startActivityForResult(
                Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, skivvy.compName)
                    .putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        getString(device_admin_persuation)
                    ), skivvy.CODE_DEVICE_ADMIN
            )
        }
    }

    //TODO: Brightness specifications
    private fun brightnessOps(action: String) {
        when {
            action.contains(skivvy.numberPattern) -> {
                val percent =
                    action.replace(skivvy.nonNumeralPattern, nothing).toFloat().toInt()
                if (percent > 100) {
                    speakOut(getString(invalid_brightness))
                    return
                }
                setBrightness(percent)
            }
            else -> {
                if (feature.getSystemBrightness(skivvy.cResolver) != null) {
                    speakOut("${feature.getSystemBrightness(skivvy.cResolver)}%")
                } else {
                    speakOut(getString(brightness_inaccessible))
                }
            }
        }
    }

    private fun volumeOps(action: String) {
        when {
            action.contains(skivvy.numberPattern) -> {
                val percent = action.replace(skivvy.nonNumeralPattern, nothing).toFloat()
                if (percent > 100F) {
                    speakOut(getString(invalid_volume_level))
                    return
                }
                if (percent > 75F) {
                    temp.setVolumePercent(percent)
                    speakOut(
                        getString(volume_level_can_harm) + "Are you sure about ${percent.toInt()}%?",
                        skivvy.CODE_VOLUME_CONFIRM
                    )
                } else {
                    skivvy.setVoicePreference(normalizeVolume = false)
                    feature.setMediaVolume(percent, audioManager)
                    speakOut(
                        getString(volume_at_) + "${percent.toInt()}" + getString(R.string.percent),
                        null,
                        isFeedback = !anyTaskRunning()
                    )
                }
            }
            //TODO: Volume actions by using last command method, index of.
            action.contains("up") || action.contains("increase") ||
                    action.contains("raise") -> {
                skivvy.setVoicePreference(normalizeVolume = false)
                if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) ==
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                ) {
                    speakOut(getString(volume_highest))
                } else {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    speakOut(
                        getString(R.string.volume_increased),
                        null,
                        parallelReceiver = false,
                        isFeedback = !anyTaskRunning()
                    )
                }
            }
            action.contains("down") || action.contains("decrease") -> {
                skivvy.setVoicePreference(normalizeVolume = false)
                if (
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) ==
                                audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
                    } else {
                        audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
                    }
                ) {
                    speakOut(getString(volume_lowest))
                } else {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                    speakOut(
                        getString(volume_decreased),
                        null,
                        isFeedback = !anyTaskRunning()
                    )
                }
            }
            action.contains("max") ||
                    action.contains("full") || action.contains("highest") -> {
                skivvy.setVoicePreference(normalizeVolume = false)
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    AudioManager.FLAG_SHOW_UI
                )
                speakOut(getString(volume_highest))
            }
            action.contains("min") || action.contains("lowest") -> {
                skivvy.setVoicePreference(normalizeVolume = false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC),
                        AudioManager.FLAG_SHOW_UI
                    )
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    speakOut(getString(min_audible_volume))
                } else {
                    volumeOps(getString(ten_percent))
                }
            }
            action.contains("silence") || action.contains("zero") -> {
                skivvy.setVoicePreference(normalizeVolume = false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC),
                        AudioManager.FLAG_SHOW_UI
                    )
                    speakOut(getString(volume_off))
                } else {
                    volumeOps(getString(zero_percent))
                }
            }
            else -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_SAME,
                    AudioManager.FLAG_SHOW_UI
                )
                speakOut(
                    getString(volume_at_) + "${feature.getMediaVolume(audioManager)}" + getString(
                        percent
                    )
                )
            }
        }
    }

    //TODO: specific app actions
    // actions invoking other applications
    private fun appOptions(text: String?): Boolean {
        var localText: String
        if (text != null) {
            localText = input.removeBeforeLastStringsIn(
                text,
                arrayOf(resources.getStringArray(R.array.initiators))
            ).toLowerCase(skivvy.locale)
            if (localText == nothing) {
                localText = input.removeStringsIn(
                    text,
                    arrayOf(resources.getStringArray(R.array.initiators))
                )
                if (localText == nothing) {
                    speakOut("Open what?", skivvy.CODE_SPEECH_RECORD)
                    return true
                }
            }
            if (packages.getTotalPackages() > 0) {
                var i = 0
                while (i < packages.getTotalPackages()) {
                    if (packages.packageAppName() == null || packages.packagesName() == null || packages.packagesMain() == null) {
                        speakOut(
                            getString(what_),
                            skivvy.CODE_SPEECH_RECORD,
                            parallelReceiver = true
                        )
                        return true
                    }
                    //TODO: Retry on function end successful preference
                    when {
                        localText == getString(app_name).toLowerCase(skivvy.locale) -> {
                            initialView()
                            speakOut(getString(i_am) + getString(app_name))
                            return true
                        }
                        localText == packages.getPackageAppName(i) -> {
                            temp.setPackageIndex(i)
                            successView(packages.getPackageIcon(i))
                            speakOut(
                                getString(opening) + packages.getPackageAppName(i)!!
                                    .capitalize(skivvy.locale)
                            )
                            startActivity(Intent(packages.getPackageIntent(i)))
                            return true
                        }
                        localText.substringAfterLast(space) == packages.getPackageAppName(i) -> {
                            temp.setPackageIndex(i)
                            successView(packages.getPackageIcon(i))
                            speakOut(
                                getString(opening) + packages.getPackageAppName(i)!!
                                    .capitalize(skivvy.locale)
                            )
                            startActivity(Intent(packages.getPackageIntent(i)))
                            return true
                        }
                        packages.getPackageName(i)!!.contains(localText) -> {
                            temp.setPackageIndex(i)
                            waitingView(packages.getPackageIcon(i))
                            if (packages.getPackageAppName(i)!!.contains(localText)) {
                                speakOut(
                                    "Did you mean ${packages.getPackageAppName(i)!!
                                        .capitalize(skivvy.locale)}?",
                                    skivvy.CODE_APP_CONF
                                )
                            } else {
                                speakOut(
                                    getString(do_u_want_open) + "${packages.getPackageAppName(
                                        i
                                    )!!
                                        .capitalize(skivvy.locale)}?",
                                    skivvy.CODE_APP_CONF
                                )
                            }
                            return true
                        }
                        else -> ++i
                    }
                }
            } else {
                speakOut(getString(no_apps_installed))
            }
        } else speakOut(getString(null_variable_error))
        return false
    }

    //TODO: set feedback of calculating expression if calculation was the last thing
    private fun formatPhoneNumber(number: String): String {
        var num = number.replace(space, nothing).trim()
        when {
            num.length == 10 -> {
                val local = num
                num = nothing
                var k = 0
                while (k < local.length) {
                    num += local[k]
                    if (k == 4) {
                        num += space
                    }
                    ++k
                }
            }
            num.hasCountryCode("IN") -> {
                val local = num
                num = nothing
                var k = 0
                while (k < local.length) {
                    num += local[k]
                    if (k == 2 || k == 7) {
                        num += space
                    }
                    ++k
                }
            }
            num.isLandLine() -> {
                val local = num
                num = nothing
                var k = 0
                while (k < local.length) {
                    num += local[k]
                    if (k == 3) {
                        num += space
                    }
                    ++k
                }
            }
            else -> return number
        }
        return num
    }

    private fun lightSensor(regist: Boolean = true) {
        val mySensorManager: SensorManager =
            getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val lightSensor: Sensor? = mySensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (lightSensor != null) {
            //outputText.text = "Sensor.TYPE_LIGHT Available"
            if (regist) {
                mySensorManager.registerListener(
                    lightSensorListener,
                    lightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            } else {
                mySensorManager.unregisterListener(lightSensorListener)
            }
        } else {
            //outputText.text = "Sensor.TYPE_LIGHT NOT Available"
        }
    }

    private val lightSensorListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // TODO Auto-generated method stub
        }

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_LIGHT) {
                //feedback.text = event.values[0].toString()
                if (event.values[0] > 100) {
                    //setFeedback("Brighter")
                } else if (event.values[0] < 100) {
                    //TODO :set theme according to value
                }
            }
        }
    }

    private var msgCode = MessageCode()

    private fun initiateCallProcess(text: String, callingArray: Array<String>) {
        waitingView(getDrawable(ic_phone_dialer))
        val localTxt = input.removeBeforeLastStringsIn(text, arrayOf(callingArray)).trim()
        temp.setPhone(text.replace(skivvy.nonNumeralPattern, nothing))
        if (temp.getPhone() != null) {
            when {
                temp.getPhone()!!.contains(skivvy.numberPattern) -> {
                    temp.setPhone(formatPhoneNumber(temp.getPhone()!!))
                    if (!skivvy.hasThisPermission(context, skivvy.CODE_CALL_REQUEST)) {
                        speakOut(getString(require_physical_permission))
                        requestThisPermission(skivvy.CODE_CALL_REQUEST)
                    } else {
                        speakOut(
                            getString(should_i_call) + "${temp.getPhone()}?",
                            skivvy.CODE_CALL_CONF
                        )
                    }
                }
                else -> {
                    if (localTxt.length > 1) {
                        msgCode = MessageCode(
                            localTxt,
                            skivvy.CODE_CALL_CONF
                        )
                        if (skivvy.hasThisPermission(
                                context,
                                skivvy.CODE_CONTACTS_REQUEST
                            )
                        ) {
                            SearchContact().execute(msgCode)
                        } else {
                            speakOut(
                                getString(
                                    require_physical_permission
                                )
                            )
                            requestThisPermission(skivvy.CODE_CONTACTS_REQUEST)
                        }
                    } else {
                        lastTxt = text
                        speakOut(getString(call_who_), skivvy.CODE_SPEECH_RECORD)
                    }
                }
            }
        } else {
            lastTxt = text
            speakOut(getString(call_who_), skivvy.CODE_SPEECH_RECORD)
        }
    }

    private fun initiateEmailProcess(text: String, emailArray: Array<String>) {
        waitingView(getDrawable(ic_envelope_open))
        val localTxt = input.removeBeforeLastStringsIn(text, arrayOf(emailArray)).trim()
        temp.setEmail(localTxt.replace(space, nothing).trim())
        when {
            temp.getEmail()!!.matches(skivvy.emailPattern) -> {
                inputText.text = temp.getEmail()
                speakOut(
                    getString(what_is_subject),
                    skivvy.CODE_EMAIL_CONTENT
                )
            }
            localTxt.length > 1 -> {
                msgCode = MessageCode(localTxt, skivvy.CODE_EMAIL_CONF)
                if (skivvy.hasThisPermission(context, skivvy.CODE_CONTACTS_REQUEST)) {
                    SearchContact().execute(msgCode)
                } else {
                    speakOut(getString(require_physical_permission))
                    requestThisPermission(skivvy.CODE_CONTACTS_REQUEST)
                }
            }
            else -> {
                lastTxt = text
                speakOut(getString(email_who_), skivvy.CODE_SPEECH_RECORD)
            }
        }
    }

    private fun initiateSMSProcess(text: String, textArray: Array<String>) {
        waitingView(getDrawable(ic_message))
        val localTxt = input.removeBeforeLastStringsIn(text, arrayOf(textArray)).trim()
        temp.setPhone(
            localTxt.replace(
                skivvy.nonNumeralPattern,
                nothing
            )
        )
        when {
            temp.getPhone()!!.contains(skivvy.numberPattern) -> {
                speakOut(
                    getString(what_is_message),
                    skivvy.CODE_TEXT_MESSAGE_BODY
                )
            }
            localTxt.length > 1 -> {
                msgCode = MessageCode(localTxt, skivvy.CODE_SMS_CONF)
                if (skivvy.hasThisPermission(context, skivvy.CODE_CONTACTS_REQUEST)) {
                    SearchContact().execute(msgCode)
                } else {
                    speakOut(getString(require_physical_permission))
                    requestThisPermission(skivvy.CODE_CONTACTS_REQUEST)
                }
            }
            else -> {
                lastTxt = text
                speakOut(getString(text_who_), skivvy.CODE_SPEECH_RECORD)
            }
        }
    }

    //action invoking direct intents
    private fun directActions(text: String): Boolean {
        val calls: Array<String> = resources.getStringArray(R.array.calls)
        val emails: Array<String> = resources.getStringArray(R.array.emails)
        val texts: Array<String> = resources.getStringArray(R.array.texts)
        val actions = arrayOf(calls, emails, texts)
        when (input.indexOfFinallySaidArray(text, actions).index) {
            actions.indexOf(calls) -> initiateCallProcess(text, calls)
            actions.indexOf(emails) -> initiateEmailProcess(text, emails)
            actions.indexOf(texts) -> initiateSMSProcess(text, texts)
            else -> return false
        }
        return true
    }

    private fun textMessageOps(
        target: String,
        payLoad: String,
        code: Int
    ) {
        if (code == skivvy.CODE_SMS_CONF) {
            try {
                successView(null)
                val sms: SmsManager = SmsManager.getDefault()
                sms.sendTextMessage(target, null, payLoad, null, null)
            } catch (e: Exception) {
                speakOut("Failed to send SMS")
            }
        } else {
            speakOut("Not yet supported")
        }
    }

    @SuppressLint("MissingPermission")
    private fun callingOps(number: String?, name: String? = null) {
        if (number != null) {
            if (skivvy.hasThisPermission(context, skivvy.CODE_CALL_REQUEST)) {
                name?.let { speakOut(getString(calling) + name) }
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
            } else {
                requestThisPermission(skivvy.CODE_CALL_REQUEST)
            }
        } else {
            errorView()
            speakOut(getString(null_variable_error))
        }
    }

    private fun emailingOps(address: String?, subject: String?, body: String?) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("mailto:$address?subject=$subject&body=$body")
            )
        )
    }

    data class MessageCode(val message: String = "", val code: Int = -1)

    inner class SearchContact : AsyncTask<MessageCode, Void, ContactModel>() {
        override fun onPreExecute() {
            super.onPreExecute()
            temp.setPhoneIndex(0)
            temp.setEmailIndex(0)
            waitingView()
            speakOut(getString(searching))
        }

        override fun doInBackground(vararg params: MessageCode): ContactModel? {
            temp.setContactCode(params[0].code)
            return contactOps(params[0].message)
        }

        override fun onPostExecute(result: ContactModel?) {
            super.onPostExecute(result)
            handleSearchResults(result)
        }
    }

    private fun handleSearchResults(result: ContactModel?) {
        if (result == null) {
            errorView()
            speakOut(getString(no_contacts_available))
        } else if (!temp.getContactPresence()) {
            errorView()
            resetVariables()
            speakOut(getString(contact_not_found))
        } else {
            waitingView(cropToCircle(result.photoID))
            when (temp.getContactCode()) {
                skivvy.CODE_EMAIL_CONF -> {
                    if (result.emailList.isNullOrEmpty()) {
                        errorView()
                        speakOut(
                            getString(you_dont_seem_having) + result.displayName + getString(
                                someone_email_address
                            )
                        )
                    } else {
                        speakOut(
                            getString(what_is_subject),
                            skivvy.CODE_EMAIL_CONTENT
                        )
                    }
                }
                else -> {
                    when (temp.getContactCode()) {
                        skivvy.CODE_CALL_CONF -> {
                            when {
                                result.phoneList.isNullOrEmpty() -> {
                                    errorView()
                                    speakOut(
                                        getString(you_dont_seem_having) + result.displayName + getString(
                                            someones_phone_number
                                        )
                                    )
                                }
                                result.phoneList!!.size == 1 -> {
                                    speakOut(
                                        getString(should_i_call) + "${result.displayName}?",
                                        skivvy.CODE_CALL_CONF
                                    )
                                }
                                else -> {
                                    setFeedback(
                                        getString(i_have_) + result.phoneList!!.size + getString(
                                            _phone_nums_of_
                                        ) + result.displayName, !tasksOngoing[NOTIFTASK]
                                    )
                                    if (skivvy.hasThisPermission(
                                            context,
                                            skivvy.CODE_CALL_LOG_REQUEST
                                        )
                                    ) {
                                        ArrangeViaLogs().execute(result.displayName)        //to arrange phone list according to recently called
                                    } else {
                                        speakOut(
                                            getString(i_can_call_) + result.displayName + getString(
                                                _at_their_recent_num
                                            ) + ", " + getString(if_u_allow_call_logs)
                                        )
                                        requestThisPermission(skivvy.CODE_CALL_LOG_REQUEST)
                                    }
                                }
                            }
                        }
                        skivvy.CODE_SMS_CONF -> {
                            if (result.phoneList == null || result.phoneList!!.isEmpty()) {
                                errorView()
                                speakOut(
                                    getString(you_dont_seem_having) + result.displayName + getString(
                                        someones_phone_number
                                    )
                                )
                            } else {
                                speakOut(
                                    getString(what_is_message),
                                    skivvy.CODE_TEXT_MESSAGE_BODY
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    //to arrange phone list in background according to recent calls of given contact name
    inner class ArrangeViaLogs : AsyncTask<String, Void, Array<String?>>() {
        override fun onPreExecute() {
            super.onPreExecute()
            temp.setPhoneIndex(0)
        }

        override fun doInBackground(vararg params: String): Array<String?>? {
            return arrangeNumbersAsCallLogs(params[0])
        }

        override fun onPostExecute(result: Array<String?>?) {
            super.onPostExecute(result)
            result?.let {
                speakOut(
                    getString(should_i_call) + contact.displayName +
                            " at ${result[temp.getPhoneIndex()]}?",
                    skivvy.CODE_CALL_CONF
                )
            }
        }
    }

//TODO: match strings with first letter and so on

    // for nicknames
    private fun getNicknamesOf(contactID: String): Array<String?>? {
        var nickNames: Array<String?>? = null
        val cursor: Cursor? = skivvy.cResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Nickname.NAME),
            ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
            arrayOf(
                contactID, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE
            ),
            null
        )
        cursor?.let {
            if (it.count == 0) return null
            var nc = 0
            nickNames = arrayOfNulls(it.count)
            while (it.moveToNext()) {
                it.getString(0)?.let { it1 ->
                    nickNames!![nc] = it1.toLowerCase(skivvy.locale)
                    ++nc
                }
            }
            it.close()
            return nickNames?.let { it1 -> input.removeDuplicateStrings(it1) }
        }
        cursor?.close()
        return nickNames
    }

    private fun getPhoneNumbersOf(contactID: String): Array<String?>? {
        var phones: Array<String?>? = null
        val cursor: Cursor? = skivvy.cResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
            arrayOf(contactID),
            null
        )
        cursor?.let {
            if (it.count == 0) return null
            phones = arrayOfNulls(it.count)
            var k = 0
            while (it.moveToNext()) {
                it.getString(0)?.let { it1 ->
                    phones!![k] = formatPhoneNumber(it1)
                    ++k
                }
            }
            it.close()
            return phones?.let { it1 -> input.removeDuplicateStrings(it1) }
        }
        cursor?.close()
        return phones
    }

    private fun getEmailIDsOf(contactID: String): Array<String?>? {
        var emails: Array<String?>? = null
        val cursor: Cursor? = skivvy.cResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.DATA),
            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
            arrayOf(contactID),
            null
        )
        cursor?.let {
            if (it.count == 0) return null
            emails = arrayOfNulls(it.count)
            var k = 0
            while (it.moveToNext()) {
                it.getString(0)?.let { it1 ->
                    emails!![k] = it1
                    ++k
                }
            }
            it.close()
            return emails?.let { it1 -> input.removeDuplicateStrings(it1) }
        }
        cursor?.close()
        return emails
    }

    private fun getNameAndImageUri(number: String): ArrayList<String?>? {
        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.NUMBER,
            ContactsContract.PhoneLookup.PHOTO_URI,
            ContactsContract.PhoneLookup.HAS_PHONE_NUMBER
        )

        // encode the phone number and build the filter URI
        val contactUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        var contactName: String? = null
        var image: String? = null
        val cursor = skivvy.cResolver.query(
            contactUri,
            projection, null, null, null
        )
        cursor?.let {
            if (it.moveToFirst()) {
                contactName =
                    it.getString(it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME))
                image = it.getString(it.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI))
            }
            it.close()
            return arrayListOf(contactName, image)
        }
        cursor?.close()
        return null
    }

    //TODO: Create suitable projection and search argument for cursor
    private fun contactOps(keyPhrase: String): ContactModel? {
        val cursor: Cursor? = skivvy.cResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_URI
            ),
            null,
            null,
            null
        )
        cursor?.let {
            if (it.count > 0) {
                while (it.moveToNext()) {
                    contact.contactID = it.getString(0)
                    it.getString(1)?.let { it1 -> contact.displayName = it1 }
                    contact.nickName = getNicknamesOf(contact.contactID)
                    temp.setContactReceived(keyPhrase.trim())
                    if (temp.getContactReceived() == contact.displayName.toLowerCase(skivvy.locale)
                        || temp.getContactReceived() == contact.displayName.substringBefore(space)
                            .toLowerCase(skivvy.locale)
                        || !contact.nickName.isNullOrEmpty() && contact.nickName!!.contains(temp.getContactReceived())
                    ) {
                        temp.setContactPresence(true)
                        it.getString(2)?.let { uri -> contact.photoID = uri }
                        contact.contactID.let {
                            contact.emailList = getEmailIDsOf(it)
                            contact.phoneList = getPhoneNumbersOf(it)
                        }
                        break
                    } else
                        temp.setContactPresence(false)
                }
                it.close()
                return contact
            } else {
                it.close()
                return null
            }
        }
        cursor?.close()
        return null
    }

    private fun cropToCircle(uri: String?): RoundedBitmapDrawable? {
        if (uri == null) return null
        val rb: RoundedBitmapDrawable =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                RoundedBitmapDrawableFactory.create(
                    resources,
                    try {
                        ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(
                                context.contentResolver,
                                Uri.parse(uri)
                            )
                        )
                    } catch (e: Exception) {
                        return null
                    }
                )
            } else {
                RoundedBitmapDrawableFactory.create(
                    resources,
                    try {
                        MediaStore.Images.Media.getBitmap(
                            context.contentResolver,
                            Uri.parse(uri)
                        )
                    } catch (e: Exception) {
                        return null
                    }
                )
            }
        rb.isCircular = true
        rb.setAntiAlias(true)
        return rb
    }

    private fun takeScreenshot() {
        val now = Date()
        DateFormat.format("yyyy-MM-dd_hh:mm:ss", now)
        try {
            val mPath: String =
                Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg"
            val v1 = window.decorView.rootView
            v1.isDrawingCacheEnabled = true
            val bitmap = Bitmap.createBitmap(v1.drawingCache)
            v1.isDrawingCacheEnabled = false
            val imageFile = File(mPath)
            val outputStream = FileOutputStream(imageFile)
            val quality = 100
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()
            speakOut(getString(snap_success))
        } catch (e: Throwable) {
            errorView()
            speakOut(getString(snap_failed))
            e.printStackTrace()
        }
    }

//TODO:  airplane mode, power off, restart phone,auto rotation,hotspot, specific settings

    private fun saveCalculationResult(result: String) {
        getSharedPreferences(skivvy.PREF_HEAD_CALC, Context.MODE_PRIVATE).edit()
            .putString(skivvy.PREF_KEY_LAST_CALC, result).apply()
    }

    private fun getLastCalculationResult(): String {
        return getSharedPreferences(skivvy.PREF_HEAD_CALC, Context.MODE_PRIVATE)
            .getString(skivvy.PREF_KEY_LAST_CALC, "0")!!
    }

    //Handle incoming phone calls
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyManager: TelephonyManager? = null
    private var lastState: Int? = null

    //TODO: timer while call
    private fun startTimer(onFeedback: Boolean = false) {
        val timer = object : CountDownTimer(20000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
            }
        }
    }

    private var incomingName: String? = null
    private var incomingImage: Drawable? = null

    //TODO: manage incoming contact lookup
    inner class FindContactIncoming : AsyncTask<String, Void, ArrayList<String?>>() {
        var number: String? = null
        var callState: Int? = null
        override fun doInBackground(vararg params: String?): ArrayList<String?>? {
            params[0]?.let {
                number = it
                params[1]?.let { it1 ->
                    callState = it1.toFloat().toInt()
                }
                return getNameAndImageUri(it)
            }
            return null
        }

        override fun onPostExecute(result: ArrayList<String?>?) {
            super.onPostExecute(result)
            if (result != null) {
                if (result[0] != null || result[1] != null) {
                    callStateManager(callState!!, number!!, result[0], cropToCircle(result[1]))
                }
            }
        }
    }

    private fun callStateManager(
        state: Int,
        phone: String,
        name: String? = null,
        image: Drawable? = null
    ) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {        //incoming
                this.incomingName = name
                this.incomingImage = image
                tasksOngoing[CALLTASK] = true
                feature.silentRinger(audioManager, true)
                successView(image ?: getDrawable(ic_phone_dialer))
                speakOut(
                    if (name != null) name + getString(_is_calling_you)
                    else getString(incoming_call_from_) + phone,
                    taskCode = skivvy.CODE_ANSWER_CALL,
                    isUrgent = true
                )
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                tasksOngoing[CALLTASK] = true
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {     //user picked up
                    speakOut(
                        getString(speaking_to_) + (name ?: phone)
                    )
                    waitingView(image ?: getDrawable(ic_phone_dialer))
                } else {        //dialed by user
                    speakOut(
                        getString(calling_) + when {
                            name != null -> name
                            temp.getContactPresence() -> contact.displayName
                            else -> phone
                        }
                    )
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                tasksOngoing[CALLTASK] = false
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {     //missed call
                    speakOut(
                        getString(you_missed_call_from_) + (this.incomingName ?: phone)
                    )
                    errorView(
                        this.incomingImage ?: getDrawable(ic_phone_dialer)
                    )
                } else if (lastState == TelephonyManager.CALL_STATE_OFFHOOK) {      //call ended
                    speakOut(
                        getString(call_ended_with_) + when {
                            name != null -> name
                            temp.getContactPresence() -> contact.displayName
                            else -> phone
                        }
                    )
                }
            }
        }
        lastState = state
        this.phone = null
        this.image = null
    }

    //TODO : Call state refurbishment

    var name: String? = null
    var phone: String? = null
    var image: Drawable? = null
    private fun callStateListener() {
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, number: String) {
                if (state != lastState && number.isNotEmpty()) {
//                    callStateManager(state, number)
                    FindContactIncoming().execute(number, state.toString())
                }
            }
        }
        telephonyManager!!.listen(
            phoneStateListener,
            PhoneStateListener.LISTEN_CALL_STATE
        )
    }

    var batteryLevel: Int = 0
    private val mBatInfoReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context?, intent: Intent) {
            batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            if (!anyTaskRunning() && skivvy.readBatteryStatus()) {
                when (batteryLevel) {
                    100 -> speakOut(
                        "Battery is full, you may remove charger now.",
                        isFeedback = true
                    )
                    15 -> speakOut(
                        "Battery is low, device might need charging.",
                        isFeedback = true,
                        isUrgent = true
                    )
                    5 -> speakOut(
                        "Battery critically low, needs charging now!",
                        isFeedback = true,
                        isUrgent = true
                    )
                }
            }
        }
    }
    var lastTimeNotif: String? = null
    var lastMsgNotif: String? = null
    var lastNotifFrom: String? = null
    var lastNotifKey: String? = null
    var lastOngoing = false
    var ongoingNotifKey: String? = null
    private val mNotificationReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val key = intent.getStringExtra(skivvy.notificationID)
            val state = intent.getStringExtra(skivvy.notificationStatus)
            val from = intent.getStringExtra(skivvy.notificationAppName)
            val msg = intent.getStringExtra(skivvy.notificationTicker)?.replace("—", "by")
            val time = intent.getStringExtra(skivvy.notificationTime)
            val ongoing = intent.getBooleanExtra(skivvy.notificationOngoing, false)
            if (state == skivvy.notificationRemoved) {
                if (key == lastNotifKey) {
                    if (tasksOngoing[NOTIFTASK]) {
                        if (key == ongoingNotifKey) {
                            setFeedback(nothing)
                            tasksOngoing[NOTIFTASK] = false
                            initialView()
                        }
                        setOutput(getString(what_next))
                    }
                } else if (key == ongoingNotifKey) {
                    setFeedback(nothing)
                    initialView()
                }
            } else {
                if (ongoing) {        //if notification  is sticky
                    if (tasksOngoing[NOTIFTASK]) {        //if already has a sticky notification
                        if (key == lastNotifKey) {
                            if (msg == lastMsgNotif)
                                msg?.let { setFeedback(msg) }  //if same app post same notification sticky, replace previous one
                            else
                                msg?.let {
                                    speakOut(
                                        it,
                                        isFeedback = true
                                    )
                                }         //if same app post different notification sticky, speak and replace previous one
                        } else
                            msg?.let {
                                speakOut(
                                    it,
                                    isFeedback = false
                                )
                            }       //if other app post notification sticky, show on output
                    } else {
                        msg?.let {
                            speakOut(
                                it,
                                isFeedback = true
                            )
                        }         //if no previous sticky notification, show as feedback
                    }
                    tasksOngoing[NOTIFTASK] = true
                    ongoingNotifKey = key
                    waitingView()
                } else {        //if notification is removable
                    if (tasksOngoing[NOTIFTASK]) {
                        setFeedback(lastMsgNotif)
                        if (key == ongoingNotifKey) {
                            speakOut(nothing)
                        } else {
                            msg?.let { speakOut(it, isFeedback = false) }
                        }
                        tasksOngoing[NOTIFTASK] = true
                    } else {
                        msg?.let { speakOut(it, isFeedback = true) }
                        tasksOngoing[NOTIFTASK] = false
                    }
                }
                lastNotifKey = key
                lastTimeNotif = time
                lastMsgNotif = msg
                lastNotifFrom = from
                lastOngoing = ongoing
            }
        }
    }

    //TODO: notification content display
    override fun onDestroy() {
        speakOut(getString(exit_msg))
        loading.startAnimation(anim.zoomInRotate)
        this.unregisterReceiver(this.mBatInfoReceiver)
        this.unregisterReceiver(this.mNotificationReceiver)
        sendBroadcast(
            Intent(skivvy.actionServiceRestart).putExtra(skivvy.serviceDead, true)
        )
        skivvy.tts?.let {
            it.stop()
            it.shutdown()
        }
        skivvy.isHomePageRunning = false
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        skivvy.isHomePageRunning = false
        speakOut(getString(exit_msg))
    }

    /**
     * Following function checks whether the given string has last response as acceptance or denial
     * @param response : the given string of response
     * @return : returns boolean value according to content of given response (true for positive, false for negative, null for invalid response)
     * TODO: extend this to other types of responses too.
     */
    private fun isCooperative(response: String): Boolean? {
        return when {
            input.containsString(
                input.removeBeforeLastStringsIn(
                    response,
                    arrayOf(
                        resources.getStringArray(R.array.acceptances),
                        resources.getStringArray(R.array.disruptions)
                    )
                ),
                arrayOf(resources.getStringArray(R.array.denials))
            ) -> false      //if denial was the last response in string
            input.containsString(
                input.removeBeforeLastStringsIn(
                    response,
                    arrayOf(
                        resources.getStringArray(R.array.denials),
                        resources.getStringArray(R.array.disruptions)
                    )
                ), arrayOf(resources.getStringArray(R.array.acceptances))
            ) -> true       //if acceptance was the last response in string
            input.containsString(
                response,
                arrayOf(resources.getStringArray(R.array.denials)),
                isSingleLine = true
            ) -> false      //if response contains denial
            input.containsString(
                response,
                arrayOf(resources.getStringArray(R.array.acceptances)),
                isSingleLine = true
            ) -> true       //if response contains acceptance
            else -> null        //if response was invalid
        }
    }

    private fun isInitiative(response: String): Boolean? {
        return when {
            input.containsString(
                input.removeBeforeLastStringsIn(
                    response,
                    arrayOf(
                        resources.getStringArray(R.array.acceptances),
                        resources.getStringArray(R.array.initiators)
                    )
                ),
                arrayOf(
                    resources.getStringArray(R.array.denials),
                    resources.getStringArray(R.array.disruptions)
                )
            ) -> false      //if denial was the last response in string
            input.containsString(
                input.removeBeforeLastStringsIn(
                    response,
                    arrayOf(
                        resources.getStringArray(R.array.denials),
                        resources.getStringArray(R.array.disruptions)
                    )
                ),
                arrayOf(
                    resources.getStringArray(R.array.initiators),
                    resources.getStringArray(R.array.acceptances)
                )
            ) -> true       //if acceptance was the last response in string
            input.containsString(
                response,
                arrayOf(
                    resources.getStringArray(R.array.denials),
                    resources.getStringArray(R.array.disruptions)
                ),
                isSingleLine = true
            ) -> false      //if response contains denial/cancellation
            input.containsString(
                response,
                arrayOf(
                    resources.getStringArray(R.array.initiators),
                    resources.getStringArray(R.array.acceptances)
                ),
                isSingleLine = true
            ) -> true      //if response contains acceptance/initiatives
            else -> null
        }
    }

    private fun isDisruptive(response: String): Boolean {
        return when {
            input.containsString(
                input.removeBeforeLastStringsIn(
                    response, arrayOf(resources.getStringArray(R.array.disruptions)),
                    excludeLast = true
                ), arrayOf(resources.getStringArray(R.array.disruptions))
            ) -> true
            else -> false
        }
    }

    private fun initialView(onGoing: Boolean = false) {
        if (!onGoing) {
            loading.startAnimation(anim.zoomInOutRotate)
            loading.setImageDrawable(
                getDrawable(
                    when (skivvy.getThemeState()) {
                        R.style.BlueTheme -> {
                            if (skivvy.isColorfulSkivvy()) dots_in_circle_colorful_white
                            else dots_in_circle_white
                        }
                        else -> {
                            if (skivvy.isColorfulSkivvy()) dots_in_circle_colorful
                            else dots_in_circle
                        }
                    }
                )
            )
            greet.setCompoundDrawablesWithIntrinsicBounds(
                if (skivvy.isColorfulSkivvy()) dots_in_circle_colorful
                else dots_in_circle
                , 0, 0, 0
            )
            setOutput(getString(what_next))
            setFeedback(null)
            icon.setImageDrawable(null)
            resetVariables()
        }
        lastTxt = null
        txt = null
        SearchContact().cancel(true)
    }

    private fun resetVariables() {
        contact = ContactModel()
        temp = TempDataManager()
        msgCode = MessageCode()
        this.incomingName = null
        this.incomingImage = null
        txt = null
    }

    private fun waitingView(image: Drawable? = null) {
        loading.startAnimation(anim.fadeOnFast)
        loading.startAnimation(anim.focusRotate)
        loading.setImageDrawable(getDrawable(dots_in_circle_yellow))
        if (image != null) {
            icon.setImageDrawable(image)
        }
    }

    private fun errorView(image: Drawable? = null) {
        loading.startAnimation(anim.fadeOnFadeOff)
        loading.setImageDrawable(getDrawable(dots_in_circle_red))
        image?.let { icon.setImageDrawable(image) }
        if (skivvy.shouldRetry())
            startVoiceRecIntent(skivvy.CODE_SPEECH_RECORD)
    }

    private fun successView(image: Drawable?) {
        loading.startAnimation(anim.fadeOnFast)
        loading.startAnimation(anim.focusDefocusRotate)
        loading.setImageDrawable(getDrawable(dots_in_circle_green))
        if (image != null) {
            icon.setImageDrawable(image)
        }
    }

    private fun setOutput(text: String?) {
        outputText.startAnimation(anim.fadeOnFast)
        outputText.text = text
    }

    private fun speakOut(
        text: String,
        taskCode: Int? = null,
        parallelReceiver: Boolean = skivvy.getParallelResponseStatus(),
        isFeedback: Boolean = false,
        isUrgent: Boolean = false
    ) {
        if (isFeedback) setFeedback(text)
        else setOutput(text)
        if (skivvy.getVolumeNormal()) feature.setMediaVolume(
            skivvy.getNormalVolume().toFloat(),
            audioManager,
            false
        )
        if (isUrgent) {
            if (skivvy.getVolumeUrgent()) {
                if (feature.getMediaVolume(audioManager) < skivvy.getUrgentVolume()) {
                    feature.setMediaVolume(
                        skivvy.getUrgentVolume().toFloat(),
                        audioManager,
                        false
                    )
                }
            }
        }
        skivvy.tts?.let {
            it.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onDone(utteranceId: String) {
                    if (!parallelReceiver)
                        taskCode?.let { code -> startVoiceRecIntent(code, text) }
                }

                override fun onError(utteranceId: String) {
                    taskCode?.let { code -> startVoiceRecIntent(code, text) }
                }

                override fun onStart(utteranceId: String) {
                    if (parallelReceiver)
                        taskCode?.let { code -> startVoiceRecIntent(code, text) }
                }
            })
            if (!skivvy.getMuteStatus()) it.speak(
                text.replace("\n", nothing),        //removing pause caused by newline character
                TextToSpeech.QUEUE_FLUSH,
                null,
                "$taskCode"
            )
            else taskCode?.let { it1 -> startVoiceRecIntent(it1, text) }
        }
    }

    //intent voice recognition, code according to action command, serving activity result
    private fun startVoiceRecIntent(
        code: Int,
        message: String = getString(generic_voice_rec_text)
    ) {
        recognitionIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, message)
        recognitionIntent.resolveActivity(packageManager)
            ?.let { startActivityForResult(recognitionIntent, code) }
    }

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private fun authStateAction(code: Int) {
        executor = ContextCompat.getMainExecutor(context)
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(auth_demand_title))
            .setSubtitle(getString(auth_demand_subtitle))
            .setDescription(getString(biometric_auth_explanation))
            .setNegativeButtonText(getString(discard))
            .build()
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    when (code) {
                        skivvy.CODE_BIOMETRIC_CONFIRM -> {
                            skivvy.setSecurityPref(biometricOn = false)
                            speakOut(getString(biometric_is_off))
                        }
                        skivvy.CODE_VOICE_AUTH_CONFIRM -> {
                            skivvy.setSecurityPref(vocalAuthOn = false)
                            speakOut(getString(voice_auth_disabled))
                        }
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (code) {
                        skivvy.CODE_BIOMETRIC_CONFIRM ->
                            speakOut(getString(biometric_off_error))
                        skivvy.CODE_VOICE_AUTH_CONFIRM ->
                            speakOut(getString(vocal_auth_off_error))
                    }

                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    speakOut(getString(verification_unsuccessfull))
                }
            })
    }

    private fun requestThisPermission(code: Int) {
        tasksOngoing[PERMITASK] = true
        if (code == skivvy.CODE_ALL_PERMISSIONS) {
            ActivityCompat.requestPermissions(
                this, skivvy.permissions,
                skivvy.CODE_ALL_PERMISSIONS
            )
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    when (code) {
                        skivvy.CODE_CONTACTS_REQUEST -> Manifest.permission.READ_CONTACTS
                        skivvy.CODE_CALL_REQUEST -> Manifest.permission.CALL_PHONE
                        skivvy.CODE_ANSWER_CALL -> Manifest.permission.ANSWER_PHONE_CALLS
                        skivvy.CODE_SMS_REQUEST -> Manifest.permission.SEND_SMS
                        skivvy.CODE_STORAGE_REQUEST -> Manifest.permission.WRITE_EXTERNAL_STORAGE
                        skivvy.CODE_CALENDER_REQUEST -> Manifest.permission.READ_CALENDAR
                        skivvy.CODE_CALL_LOG_REQUEST -> Manifest.permission.READ_CALL_LOG
                        else -> nothing
                    }
                ), code
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun arrangeNumbersAsCallLogs(name: String): Array<String?>? {
        var cursor: Cursor? = null
        if (skivvy.hasThisPermission(context, skivvy.CODE_CALL_LOG_REQUEST)) {
            cursor = skivvy.cResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER
                ),
                CallLog.Calls.CACHED_NAME + " = ?",
                arrayOf(name),
                null
            )
        } else requestThisPermission(skivvy.CODE_CALL_LOG_REQUEST)

        cursor?.let {
            it.moveToFirst()
            while (it.moveToNext()) {
                var number = nothing
                it.getString(1)?.let { num ->
                    number = formatPhoneNumber(num)        //number index column
                    contact.phoneList?.let { list ->
                        if (list.contains(number)) {
                            val index = list.indexOf(number)
                            val temp = list[index]
                            if (list[0] != temp) {
                                list[index] = list[0]
                                list[0] = temp
                            }
                        }
                    }
                }
                if (contact.phoneList!![0] == number) break
            }
            it.close()
        }
        cursor?.close()
        return contact.phoneList
    }

    private fun isNotificationServiceRunning(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    private fun getLocalisedString(eng: String, hindi: String): String {
        if (skivvy.isLocaleHindi()) {
            return hindi
        }
        return eng
    }

    private fun getArray(id: Int): Array<String> = resources.getStringArray(id)
    private fun String.isLandLine(): Boolean {
        return this[0] == '0' && this[1] == '1' && this[2] == '2' && this[3] == '0'
    }

    private fun String.hasCountryCode(country: String): Boolean {
        if (country == "IN")
            return this[0] == '+' && this[1] == '9' && this[2] == '1'
        return false
    }
}
