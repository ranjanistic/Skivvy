package org.ranjanistic.skivvy

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import org.ranjanistic.skivvy.R.drawable.*
import org.ranjanistic.skivvy.R.string.*
import org.ranjanistic.skivvy.manager.*
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.Executor
import kotlin.collections.ArrayList

@ExperimentalStdlibApi
open class MainActivity : AppCompatActivity() {

    lateinit var skivvy: Skivvy

    //TODO: lock screen activity, its brightness, battery level, charging status, incoming notifications on lock screen view, charging view
    //TODO:
    //TODO: widget for actions (calculations first, or a calculator widget)
    private lateinit var outputText: TextView
    private lateinit var inputText: TextView
    private lateinit var greet: TextView
    private lateinit var feedback: TextView

    private lateinit var focusDeFocusRotate: Animation
    private lateinit var zoomInOutRotate: Animation
    private lateinit var focusRotate: Animation
    private lateinit var zoomInRotate: Animation
    private lateinit var fadeOnFadeOff: Animation
    private lateinit var waveDamped: Animation
    private lateinit var fallDown: Animation
    private lateinit var riseUp: Animation
    private lateinit var extendDownStartSetup: Animation
    private lateinit var slideToRight: Animation
    private lateinit var rotateClock: Animation
    private lateinit var revolveRotateToLeft: Animation
    private lateinit var fadeOff: Animation
    private lateinit var fadeOn: Animation
    private lateinit var fadeOnFast: Animation
    private lateinit var receiver: ImageButton
    private lateinit var setting: ImageButton
    private lateinit var loading: ImageView
    private lateinit var backfall: ImageView
    private lateinit var icon: ImageView

    private var txt: String? = null

    private val nothing = ""
    private val space = " "
    private lateinit var context: Context
    private lateinit var calculationManager: CalculationManager
    private lateinit var input: InputSpeechManager
    private lateinit var packages: PackageDataManager
    private lateinit var audioManager: AudioManager
    private lateinit var wifiManager: WifiManager
    private lateinit var recognitionIntent: Intent
    private var contact: ContactModel = ContactModel()
    private var temp: TempDataManager =
        TempDataManager()
    private var feature: SystemFeatureManager = SystemFeatureManager()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        skivvy = this.application as Skivvy
        context = this
        setTheme(skivvy.getThemeState())
        //if (skivvy.getThemeState() == R.style.BlackTheme) {
        setContentView(R.layout.activity_main_black)
        //} else {
        //setContentView(R.layout.activity_main)
        //}
        window.statusBarColor = ContextCompat.getColor(context, android.R.color.transparent)
        window.navigationBarColor = ContextCompat.getColor(context, android.R.color.transparent)
        hideSysUI()
        setViewAndDefaults()
        loadDefaultAnimations()
        normalView()
        setListeners()
        setOutput(getString(im_ready))
        inputText.text = getString(tap_the_button)
    }

    private fun setViewAndDefaults() {
        setting = findViewById(R.id.setting)
        outputText = findViewById(R.id.textOutput)
        inputText = findViewById(R.id.textInput)
        feedback = findViewById(R.id.feedbackOutput)
        loading = findViewById(R.id.loader)
        icon = findViewById(R.id.actionIcon)
        receiver = findViewById(R.id.receiverBtn)
        greet = findViewById(R.id.greeting)
        backfall = findViewById(R.id.backdrop)
        packages = skivvy.packageDataManager
        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        calculationManager = CalculationManager(skivvy)
        input = InputSpeechManager()
        greet.text = getString(app_name)
        greet.setCompoundDrawablesWithIntrinsicBounds(R.drawable.dots_in_circle, 0, 0, 0)
    }

    private fun loadDefaultAnimations() {
        fallDown = AnimationUtils.loadAnimation(context, R.anim.fall_back)
        backfall.startAnimation(fallDown)
        riseUp = AnimationUtils.loadAnimation(context, R.anim.rise_back)
        waveDamped = AnimationUtils.loadAnimation(context, R.anim.bubble_wave)
        receiver.startAnimation(waveDamped)
        greet.startAnimation(waveDamped)
        zoomInOutRotate = AnimationUtils.loadAnimation(context, R.anim.rotate_emerge_demerge)
        focusDeFocusRotate = AnimationUtils.loadAnimation(context, R.anim.rotate_focus)
        focusRotate = AnimationUtils.loadAnimation(context, R.anim.rotate_slow)
        fadeOnFadeOff = AnimationUtils.loadAnimation(context, R.anim.fade)
        zoomInRotate = AnimationUtils.loadAnimation(context, R.anim.rotate_exit)
        fadeOff = AnimationUtils.loadAnimation(context, R.anim.fade_off)
        fadeOn = AnimationUtils.loadAnimation(context, R.anim.fade_on)
        fadeOnFast = AnimationUtils.loadAnimation(context, R.anim.fade_on_quick)
        revolveRotateToLeft = AnimationUtils.loadAnimation(context, R.anim.pill_slide_left)
        setting.startAnimation(revolveRotateToLeft)
        rotateClock = AnimationUtils.loadAnimation(context, R.anim.rotate_clock)
        slideToRight = AnimationUtils.loadAnimation(context, R.anim.slide_right)
        extendDownStartSetup = AnimationUtils.loadAnimation(context, R.anim.extend_back)
    }

    private fun startSettingAnimate() {
        slideToRight.setAnimationListener(object: Animation.AnimationListener{
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {}
            override fun onAnimationStart(animation: Animation?) {

            }
        })
        setting.startAnimation(slideToRight)
        backfall.startAnimation(extendDownStartSetup)
        greet.startAnimation(fadeOff)
        outputText.startAnimation(fadeOff)
        receiver.startAnimation(fadeOff)
        inputText.startAnimation(fadeOff)
    }

    private fun startResumeAnimate() {
        greet.text = getString(app_name)
        greet.startAnimation(fadeOn)
        receiver.startAnimation(fadeOn)
        receiver.visibility = View.VISIBLE
        greet.startAnimation(waveDamped)
        receiver.startAnimation(waveDamped)
        setting.startAnimation(revolveRotateToLeft)
        backfall.startAnimation(riseUp)
        outputText.startAnimation(fadeOn)
        inputText.startAnimation(fadeOn)
        //loading.setImageDrawable(getDrawable(dots_in_circle))
        //loading.startAnimation(zoomInOutRotate)
    }

    private fun setListeners() {
        callStateListener()
        setting.setOnClickListener {
            startSettingAnimate()
        }
        receiver.setOnClickListener {
            speakOut(nothing)
            normalView()
            startVoiceRecIntent(skivvy.CODE_SPEECH_RECORD)
        }
        extendDownStartSetup.setAnimationListener(object : Animation.AnimationListener {
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
        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, skivvy.locale)
        skivvy.tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
            when (it) {
                TextToSpeech.SUCCESS -> {
                    when (skivvy.tts!!.setLanguage(skivvy.locale)) {
                        TextToSpeech.LANG_MISSING_DATA,
                        TextToSpeech.LANG_NOT_SUPPORTED -> setOutput(
                            getString(
                                language_not_supported
                            )
                        )
                    }
                }
                else -> setOutput(getString(output_error))
            }
        })
    }

    private fun hideSysUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.decorView.apply {
            systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    // Hide the nav bar and status bar
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    override fun onRestart() {
        super.onRestart()
        hideSysUI()
        if (!skivvy.getTrainingStatus()) {
            startResumeAnimate()
        } else {
            trainingView()
        }
    }

    override fun onStop() {
        super.onStop()
        skivvy.setTrainingStatus(false)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            skivvy.CODE_ALL_PERMISSIONS -> {
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
                    if (temp.getContactReceived() != null && temp.getContactCode() != null) {
                        contactOps(temp.getContactReceived()!!, temp.getContactCode()!!)
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

    private fun setFeedback(text: String?) {
        feedback.startAnimation(fadeOnFast)
        feedback.text = text
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                startSettingAnimate()
                this.finishAffinity()
            }
            KeyEvent.KEYCODE_HEADSETHOOK -> {
                speakOut(nothing)
                normalView()
                startVoiceRecIntent(skivvy.CODE_SPEECH_RECORD)
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                setFeedback(
                    "Volume raised to ${feature.getMediaVolume(audioManager)}%"
                )
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                setFeedback(
                    "Volume lowered to ${feature.getMediaVolume(audioManager)}%"
                )
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleTrainingInput(input: String?) {
        speakOut("Okay")
        Log.d("training", "handled $input")
    }

    //TODO: recursion of command for last command continuation not working, maybe because of sentence containing both agreement and disagreement.
    private fun inGlobalCommands(text: String?): Boolean {
        if (!respondToCommand(text!!)) {
            if (!directActions(text)) {
                if (!computerOps(text)) {
                    if (!appOptions(text)) {
                        if (temp.getRetryCommandCount() < 2) {
                            if (temp.getLastCommand() != null) {
                                temp.setRetryCommandCount(temp.getRetryCommandCount() + 1)
                                inGlobalCommands(text + space + temp.getLastCommand())
                            } else {
                                speakOut(getString(recognize_error))
                                return false
                            }
                        } else {
                            speakOut(getString(recognize_error))
                            return false
                        }
                    } else temp.setLastCommand(text)
                } else temp.setLastCommand(text)
            } else temp.setLastCommand(text)
        } else temp.setLastCommand(text)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var result: ArrayList<String> = ArrayList(1)
        if (!skivvy.getTrainingStatus()) {
            if (!skivvy.nonVocalRequestCodes.contains(requestCode)) {
                if (data == null || data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        ?.get(0)
                        .toString()
                        .toLowerCase(skivvy.locale) == nothing
                ) {
                    normalView()
                    speakOut(getString(no_input))
                    return
                } else {
                    result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)!!
                    inputText.text = result[0]
                }
            }
        }
        when (requestCode) {
            skivvy.CODE_TRAINING_MODE -> {
                if (data != null) {
                    result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)!!
                    inputText.text = result[0]
                    Log.d("training", result[0])
                    handleTrainingInput(result[0])
                } else {
                    Log.d("training", "null input")
                    handleTrainingInput(null)
                }
            }
            skivvy.CODE_SPEECH_RECORD -> {
                val temp = result[0].toLowerCase(skivvy.locale)
                if (txt == null || txt == nothing) {
                    txt = temp
                } else {
                    when (isCooperative(temp)) {
                        false -> {
                            speakOut(getString(okay))
                            normalView()
                        }
                        else -> {
                            if (temp.contains(txt!!)) {
                                txt = temp
                            } else {
                                txt += " $temp"
                            }
                        }
                    }
                }
                if (txt != null) {
                    inputText.text = txt
                    if (!inGlobalCommands(txt)) {
                        errorView()
                        speakOut(getString(recognize_error))
                    } else {
                        this.temp.setRetryCommandCount(0)
                        this.temp.setLastCommand(txt)
                    }
                }
            }
            skivvy.CODE_VOICE_AUTH_CONFIRM -> {
                txt = result[0]
                    .toLowerCase(skivvy.locale)
                if (txt != skivvy.getVoiceKeyPhrase()) {
                    if (skivvy.getBiometricStatus()) {
                        speakOut(getString(vocal_auth_failed) + getString(need_physical_verification))
                        authStateAction(skivvy.CODE_VOICE_AUTH_CONFIRM)
                        biometricPrompt.authenticate(promptInfo)
                    } else {
                        speakOut(getString(vocal_auth_failed))
                    }
                } else {
                    skivvy.setPhraseKeyStatus(false)
                    speakOut(getString(vocal_auth_disabled))
                }
            }
            skivvy.CODE_BIOMETRIC_CONFIRM -> {
                txt = result[0]
                    .toLowerCase(skivvy.locale)
                if (txt != skivvy.getVoiceKeyPhrase()) {
                    if (skivvy.getBiometricStatus()) {
                        speakOut(getString(vocal_auth_failed) + getString(need_physical_verification))
                        authStateAction(skivvy.CODE_BIOMETRIC_CONFIRM)
                        biometricPrompt.authenticate(promptInfo)
                    } else {
                        speakOut(getString(vocal_auth_failed))
                    }
                } else {
                    skivvy.setBiometricsStatus(false)
                    speakOut(getString(biometric_is_off))
                }
            }
            skivvy.CODE_VOLUME_CONFIRM -> {
                txt = result[0].toLowerCase(skivvy.locale)
                if (txt != null) {
                    when (isCooperative(txt!!)) {
                        null -> speakOut(
                            "Are you sure about the harmful ${temp.getVolumePercent()}% volume?",
                            skivvy.CODE_VOLUME_CONFIRM
                        )
                        true -> {
                            feature.setMediaVolume(temp.getVolumePercent(), audioManager)
                            speakOut("Volume at ${temp.getVolumePercent().toInt()}%")
                        }
                        false -> {
                            setOutput(getString(okay))
                            volumeOps(getString(optimal_volume))
                        }
                    }
                }
            }
            skivvy.CODE_APP_CONF -> {
                txt = result[0]
                    .toLowerCase(skivvy.locale)

                when (isCooperative(txt!!)) {
                    true -> {
                        successView(packages.getPackageIcon(temp.getPackageIndex()))
                        speakOut(
                            getString(opening) + packages.getPackageAppName(temp.getPackageIndex())!!
                                .capitalize(skivvy.locale)
                        )
                        startActivity(Intent(packages.getPackageIntent(temp.getPackageIndex())))
                    }
                    false -> {
                        normalView()
                        speakOut(getString(okay))
                    }
                    else -> {
                        speakOut(
                            getString(recognize_error) + "\n" + getString(do_u_want_open) + "${packages.getPackageAppName(
                                temp.getPackageIndex()
                            )!!.capitalize(skivvy.locale)}?",
                            skivvy.CODE_APP_CONF
                        )
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
                    speakOut("Come again?", skivvy.CODE_SPEECH_RECORD)
                }
            }
            skivvy.CODE_CALL_CONF -> {
                //val cdata = skivvy.contactDataManager
                txt = result[0]
                    .toLowerCase(skivvy.locale)
                when (isCooperative(txt!!)) {
                    true -> {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CALL_PHONE
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            speakOut(getString(require_physical_permission))
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.CALL_PHONE),
                                skivvy.CODE_CALL_REQUEST
                            )
                        } else {
                            if (temp.getContactPresence()) {
                                successView(null)
                                callingOps(
                                    contact.phoneList!![temp.getPhoneIndex()],
//                                        cdata.getContactNames()[temp.getContactIndex()]!!
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
                        if (temp.getContactPresence() && temp.getPhoneIndex() < contact.phoneList!!.size && !resources.getStringArray(
                                R.array.disruptions
                            ).contains(txt)
                        ) {
                            speakOut(
                                "At ${contact.phoneList!![temp.getPhoneIndex()]}?",
                                skivvy.CODE_CALL_CONF
                            )
                        } else {
                            normalView()
                            speakOut(getString(okay))
                        }
                    }
                    else -> {
                        if (temp.getContactPresence()) {
                            speakOut(
                                getString(recognize_error) + "\n" + getString(should_i_call) + "${contact.displayName} at ${contact.phoneList!![temp.getPhoneIndex()]}?",
                                skivvy.CODE_CALL_CONF
                            )
                        } else {
                            speakOut(
                                getString(recognize_error) + "\n" + getString(should_i_call) + "${temp.getPhone()}?",
                                skivvy.CODE_CALL_CONF
                            )
                        }
                    }
                }
            }

            skivvy.CODE_EMAIL_CONTENT -> {
                //val cdata = skivvy.contactDataManager
                txt = result[0]
                    .toLowerCase(skivvy.locale)
                when (isCooperative(txt!!)) {
                    false -> {
                        normalView()
                        speakOut(getString(okay))
                    }
                    else -> {
                        if (txt == null) {
                            if (temp.getEmailSubject() == null) {
                                speakOut(
                                    getString(recognize_error) + "\n" + getString(what_is_subject),
                                    skivvy.CODE_EMAIL_CONTENT
                                )
                            } else if (temp.getEmailBody() == null) {
                                speakOut(
                                    getString(recognize_error) + "\n" + getString(what_is_body),
                                    skivvy.CODE_EMAIL_CONTENT
                                )
                            }
                        } else {
                            if (temp.getEmailSubject() == null) {
                                temp.setEmailSubject(txt)
                                speakOut(
                                    getString(subject_added) + "\n" + getString(what_is_body),
                                    skivvy.CODE_EMAIL_CONTENT
                                )
                            } else if (temp.getEmailBody() == null) {
                                temp.setEmailBody(txt)
                                if (temp.getContactPresence()) {
                                    if (contact.emailList!!.size == 1) {
                                        speakOut(
                                            getString(body_added) + "\n" +
//                                                    getString(should_i_email) + "${cdata.getContactNames()[temp.getContactIndex()]} at\n${cdata.getContactEmails()[temp.getContactIndex()]!![temp.getEmailIndex()]}?",
                                                    getString(should_i_email) + "${contact.displayName} at\n${contact.emailList!![temp.getEmailIndex()]}?",
                                            skivvy.CODE_EMAIL_CONF
                                        )
                                    } else {
                                        speakOut(
//                                            getString(body_added) + "I've got ${cdata.getContactEmails()[temp.getContactIndex()]!!.size} addresses of ${cdata.getContactNames()[temp.getContactIndex()]}.\n" +
//                                                    getString(should_i_email) + "them at\n${cdata.getContactEmails()[temp.getContactIndex()]!![temp.getEmailIndex()]}?",
                                            getString(body_added) + "I've got ${contact.emailList!!.size} addresses of ${contact.displayName}.\n" +
                                                    getString(should_i_email) + "them at\n${contact.emailList!![temp.getEmailIndex()]}?",
                                            skivvy.CODE_EMAIL_CONF
                                        )
                                    }
                                } else {
                                    speakOut(
                                        getString(body_added) + "\n" +
                                                getString(should_i_email) + "${temp.getEmail()}?",
                                        skivvy.CODE_EMAIL_CONF
                                    )
                                }
                            }
                        }
                    }
                }
            }

            skivvy.CODE_EMAIL_CONF -> {
                //val cdata = skivvy.contactDataManager
                txt = result[0]
                    .toLowerCase(skivvy.locale)
                when (isCooperative(txt!!)) {
                    true -> {
                        successView(null)
                        speakOut(getString(preparing_email))
                        if (temp.getContactPresence()) {
                            emailingOps(
//                                    cdata.getContactEmails()[temp.getContactIndex()]!![temp.getEmailIndex()],
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
                        if (temp.getContactPresence() && temp.getEmailIndex() < contact.emailList!!.size && !resources.getStringArray(
                                R.array.disruptions
                            ).contains(txt)
                        ) {
                            speakOut(
                                "At ${contact.emailList!![temp.getEmailIndex()]}?",
                                skivvy.CODE_EMAIL_CONF
                            )
                        } else {
                            normalView()
                            speakOut(getString(okay))
                        }
                    }
                    else -> {
                        if (temp.getContactPresence()) {
                            speakOut(
                                getString(recognize_error) + "\n" +
//                                            getString(should_i_email) + "${cdata.getContactNames()[temp.getContactIndex()]} at\n${cdata.getContactEmails()[temp.getContactIndex()]!![temp.getEmailIndex()]}?",
                                        getString(should_i_email) + "${contact.displayName} at\n${contact.emailList!![temp.getEmailIndex()]}?",
                                skivvy.CODE_EMAIL_CONF
                            )
                        } else {
                            speakOut(
                                getString(recognize_error) + "\n" +
                                        getString(should_i_email) + "${temp.getEmail()}?",
                                skivvy.CODE_EMAIL_CONF
                            )
                        }
                    }
                }
            }

            skivvy.CODE_TEXT_MESSAGE_BODY -> {
                //val cdata = skivvy.contactDataManager
                txt = result[0]
                    .toLowerCase(skivvy.locale)
                when (isCooperative(txt!!)) {
                    false -> {
                        normalView()
                        speakOut(getString(okay))
                    }
                    else -> {
                        if (txt != null) {
                            waitingView(null)
                            temp.setTextBody(txt)
                            if (temp.getContactPresence()) {
                                speakOut(
//                                  getString(should_i_text) + "${cdata.getContactNames()[temp.getContactIndex()]} at ${cdata.getContactPhones()[temp.getContactIndex()]!![temp.getPhoneIndex()]}" + getString( via_sms),
                                    getString(should_i_text) + "${contact.displayName} at ${contact.phoneList!![temp.getPhoneIndex()]}" + getString(
                                        via_sms
                                    ), skivvy.CODE_SMS_CONF
                                )
                            } else {
                                speakOut(
                                    getString(should_i_text) + "${temp.getPhone()}" + getString(
                                        via_sms
                                    ),
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
                //val cdata = skivvy.contactDataManager
                txt = result[0]
                    .toLowerCase(skivvy.locale)
                when (isCooperative(txt!!)) {
                    true -> {
                        if (ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.SEND_SMS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            speakOut(getString(require_physical_permission))
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.SEND_SMS),
                                skivvy.CODE_SMS_REQUEST
                            )
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
                        // if (temp.getContactPresence() && temp.getPhoneIndex() < cdata.getContactPhones()[temp.getContactIndex()]!!.size && !resources.getStringArray(
                        if (temp.getContactPresence() && temp.getPhoneIndex() < contact.phoneList!!.size && !resources.getStringArray(
                                R.array.disruptions
                            ).contains(txt)
                        ) {
                            speakOut(
                                //                                 "At ${cdata.getContactPhones()[temp.getContactIndex()]!![temp.getPhoneIndex()]}?",
                                "At ${contact.phoneList!![temp.getPhoneIndex()]}?",
                                skivvy.CODE_SMS_CONF
                            )
                        } else {
                            normalView()
                            speakOut(getString(okay))
                        }
                    }
                    else -> {
                        if (temp.getContactPresence()) {
                            speakOut(
//                                getString(should_i_text) + "${cdata.getContactNames()[temp.getContactIndex()]} at ${cdata.getContactPhones()[temp.getContactIndex()]!![temp.getPhoneIndex()]}" + getString( via_sms),
                                getString(should_i_text) + "${contact.displayName} at ${contact.phoneList!![temp.getPhoneIndex()]}" + getString(
                                    via_sms
                                ),
                                skivvy.CODE_SMS_CONF
                            )
                        } else {
                            speakOut(
                                getString(should_i_text) + "${temp.getPhone()}" + getString(
                                    via_sms
                                ),
                                skivvy.CODE_SMS_CONF
                            )
                        }
                    }
                }
            }
            skivvy.CODE_WHATSAPP_ACTION -> {

                txt = result[0]
                    .toLowerCase(skivvy.locale)
                if (txt != nothing) {
                    try {
                        if (Intent(Intent.ACTION_VIEW).setPackage("com.whatsapp")
                                .resolveActivity(context.packageManager) != null
                        ) {
                            speakOut("Sending '$txt' on whatsapp to ${contact.displayName}")
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW, Uri.parse(
                                        "https://api.whatsapp.com/send?phone="
                                                + contact.phoneList!![temp.getPhoneIndex()] + "&text=" + URLEncoder.encode(
                                            txt,
                                            "UTF-8"
                                        )
                                    )
                                )
                            )
                        }
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    //actions invoking quick commands
    @ExperimentalStdlibApi
    private fun respondToCommand(text: String): Boolean {
        val array = arrayOf(
            R.array.setup_list,
            R.array.bt_list,
            R.array.wifi_list,
            R.array.gps_list,
            R.array.lock_list,
            R.array.snap_list
        )
        when {
            text.contains("training mode") -> {
                if (text == "activate training mode") {
                    if (!skivvy.getTrainingStatus()) {
                        skivvy.setTrainingStatus(true)
                        speakOut("Activating training mode", skivvy.CODE_TRAINING_MODE)
                        //trainingView()
                        //recreate()
                    } else {
                        speakOut("Already learning")
                    }
                } else {
                    if (text.contains("deactivate")) {
                        if (skivvy.getTrainingStatus()) {
                            skivvy.setTrainingStatus(false)
                            speakOut("Deactivating training mode")
                            normalView(true)
                            //startResumeAnimate()
                            //recreate()
                        } else {
                            speakOut("Already inactive")
                        }
                    }
                }
            }

            text.contains("setup") || text.contains("set up") -> {
                startActivity(Intent(context, Setup::class.java))
                overridePendingTransition(R.anim.fade_on, R.anim.fade_off)
            }
//TODO: use this as external function, and return special codes for commands here just like isCooperative(): input.removeBeforeLastStringsIn(text, arrayOf(resources.getStringArray(R.array.bt_list)))
            text.contains("bluetooth") -> {
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
                        errorView(getDrawable(ic_bluetooth))
                        speakOut(getString(bt_already_off))
                    } else {
                        errorView(getDrawable(ic_bluetooth))
                        speakOut(getString(bt_off))
                    }
                } else {
                    val stat = feature.bluetooth(null)
                    if (stat!!) {
                        successView(getDrawable(ic_bluetooth))
                        speakOut(getString(bt_on))
                    } else {
                        errorView(getDrawable(ic_bluetooth))
                        speakOut(getString(bt_off))
                    }
                }
            }
            text.contains("wi-fi") || text.contains("wifi") -> {
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
            resources.getStringArray(array[3]).contains(text) -> {
                waitingView(getDrawable(ic_location_pointer))
                startActivityForResult(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                    skivvy.CODE_LOCATION_SERVICE
                )
            }
            text.contains("lock") || text.contains("loc") -> {
                if (text.contains("screen") || text.contains("device") || text.contains("phone")) {
                    deviceLockOps()
                } else {
                    if (text.replace("lock", nothing).trim() == nothing) {
                        txt = text
                        speakOut("Lock what?", skivvy.CODE_SPEECH_RECORD)
                    } else return false
                }
            }
            text.contains("screenshot") || text.contains("snapshot") || text.contains("take ss") -> {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    speakOut(getString(require_physical_permission))
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        skivvy.CODE_STORAGE_REQUEST
                    )
                } else {
                    takeScreenshot()
                }
            }
            text.contains("airplane") -> {
                if (text.contains("on") || text.contains("enable")) {
                    if (isAirplaneModeEnabled()) {
                        speakOut("Already on airplane mode.")
                    } else {
                        speakOut("Airplane mode on.")
                        setAirplaneMode(true)
                    }
                } else if (text.contains("off") || text.contains("disable")) {
                    if (!isAirplaneModeEnabled()) {
                        speakOut("Airplane mode is already off.")
                    } else {
                        speakOut("Airplane mode off.")
                        setAirplaneMode(false)
                    }
                }
            }
            text.contains("flash") -> {
                if (isFlashAvailable()) {
                    if (text.contains("off")) {
                        speakOut(getString(flash_off))
                        setFlashLight(false)
                    } else {
                        speakOut(getString(flash_on))
                        setFlashLight(true)
                    }
                } else {
                    speakOut("Flashlight not available")
                }
            }
            text.contains("search") -> {
                var query = text.replace("search for", nothing).replace("search", nothing).trim()
                if (query != nothing) {
                    if (query.contains("via")) {
                        query = query.replace("via", nothing).trim()
                        if (query != nothing) {
                            speakOut("Searching for " + getLastCalculationResult() + " via $query")
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://$query.com/search?q=" + getLastCalculationResult())
                                )
                            )
                        } else {
                            txt = "search via"
                            speakOut(
                                "Which search engine should I go for?",
                                skivvy.CODE_SPEECH_RECORD
                            )
                        }
                    } else {
                        txt = "search via"
                        saveCalculationResult(query)
                        speakOut("Which search engine should I go for?", skivvy.CODE_SPEECH_RECORD)
                    }
                } else {
                    if (text.replace("search for", nothing).trim() == nothing || text.replace(
                            "search",
                            nothing
                        )
                            .trim() == nothing
                    ) {
                        txt = text
                        speakOut("Search for what?", skivvy.CODE_SPEECH_RECORD)
                    } else return false
                }
            }
            text.contains("volume") -> {
                volumeOps(text.replace("volume", nothing).trim())
            }
            text.contains("brightness") -> {
                brightnessOps(text.replace("brightness", nothing).trim())
            }
            text == "mute" -> {
                skivvy.saveMuteStatus(true)
                speakOut("Muted")
            }
            text == "speak" || text == "unmute" -> {
                if (skivvy.getMuteStatus()) {
                    skivvy.saveMuteStatus(false)
                    speakOut(getString(okay))
                } else {
                    speakOut(getString(voice_output_on))
                }
            }
            text == getString(exit) -> {
                finish()
            }
            text.contains("voice authentication") -> {
                return when {
                    text.contains("enable") && text.contains("disable") -> {
                        txt = text.replace("enable", nothing)
                        txt = txt!!.replace("disable", nothing)
                        speakOut(getString(enable_or_disable), skivvy.CODE_SPEECH_RECORD)
                        true
                    }
                    text.contains("enable") -> {
                        if (!skivvy.getPhraseKeyStatus()) {
                            if (skivvy.getVoiceKeyPhrase() != null) {
                                skivvy.setPhraseKeyStatus(true)
                                speakOut(getString(vocal_auth_enabled))
                            } else {
                                speakOut(getString(vocal_auth_unset))
                                startActivity(Intent(context, Setup::class.java))
                            }
                        } else {
                            speakOut(getString(vocal_auth_already_on))
                        }
                        true
                    }
                    text.contains("disable") -> {
                        if (!skivvy.getPhraseKeyStatus()) {
                            speakOut(getString(vocal_auth_already_off))
                        } else {
                            speakOut(
                                getString(get_passphrase_text),
                                skivvy.CODE_VOICE_AUTH_CONFIRM
                            )
                        }
                        true
                    }
                    else -> {
                        if (text.replace("voice authentication", nothing).trim() == nothing) {
                            txt = text
                            speakOut("Vocal authentication what?", skivvy.CODE_SPEECH_RECORD)
                            true
                        } else false
                    }
                }
            }
            text.contains("biometric") -> {
                if (!skivvy.checkBioMetrics()) {
                    speakOut(getString(biometric_unsupported))
                } else {
                    return when {
                        text.contains("enable") && text.contains("disable") -> {
                            txt = text.replace("enable", nothing)
                            txt = txt!!.replace("disable", nothing)
                            speakOut(getString(enable_or_disable), skivvy.CODE_SPEECH_RECORD)
                            true
                        }
                        text.contains("enable") -> {
                            if (skivvy.getBiometricStatus()) {
                                speakOut(getString(biometric_already_on))
                            } else {
                                skivvy.setBiometricsStatus(true)
                                if (skivvy.getBiometricStatus()) speakOut(getString(biometric_on))
                                else speakOut(getString(biometric_enable_error))
                            }
                            true
                        }
                        text.contains("disable") -> {
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
                            true
                        }
                        else -> {
                            if (text.replace("biometric", nothing).trim() == nothing) {
                                txt = text
                                speakOut("Biometric what?", skivvy.CODE_SPEECH_RECORD)
                                true
                            } else false
                        }
                    }
                }
            }
            text == "get permission" -> {
                if (!skivvy.hasPermissions(context)) {
                    ActivityCompat.requestPermissions(
                        this, skivvy.permissions,
                        skivvy.CODE_ALL_PERMISSIONS
                    )
                } else {
                    speakOut(getString(have_all_permits))
                }
            }
            else -> {
                return false
            }
        }
        return true
    }

    private fun computerOps(rawExpression: String, reusing: Boolean = false): Boolean {
        val expression = input.expressionize(rawExpression)
        if (expression.length == 3 && !reusing) {
            try {
                val res = calculationManager.operate(
                    expression[0].toString().toFloat(),
                    expression[1],
                    expression[2].toString().toFloat()
                )
                if (res != null) {
                    speakOut(calculationManager.returnValidResult(arrayOf(res.toString())))
                    return true
                } else {
                    computerOps(rawExpression, true)
                }
            } catch (e: Exception) {
                computerOps(rawExpression, true)
            }
        }
        /**
         * Storing availability of all operators and functions in given expression,
         * to arrays of booleans as true.
         */
        val operatorsAndFunctionsArray = arrayOf(skivvy.operators, skivvy.mathFunctions)
        val operatorsAndFunctionsBoolean = arrayOf(
            arrayOfNulls<Boolean>(operatorsAndFunctionsArray[0].size),
            arrayOfNulls(operatorsAndFunctionsArray[1].size)
        )
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
        if (!operatorsAndFunctionsBoolean[0].contains(true)) {     //if no operators
            if (operatorsAndFunctionsBoolean[1].contains(true)) {       //has a mathematical function
                if (expression.contains(skivvy.numberPattern)) {
                    setFeedback(expression)
                    saveCalculationResult(calculationManager.operateFuncWithConstant(expression)!!)
                    if (getLastCalculationResult().let {
                            calculationManager.handleExponentialTerm(
                                it
                            )
                        } == nothing) {
                        errorView()
                        speakOut(getString(invalid_expression))
                    } else
                        speakOut(
                            calculationManager.formatToProperValue(
                                calculationManager.handleExponentialTerm(
                                    getLastCalculationResult()
                                )
                            )
                        )
                    return true
                } else {
                    errorView()
                    speakOut(getString(invalid_expression))
                    return true
                }
            }
            return false
        }

        val totalOps = calculationManager.totalOperatorsInExpression(expression)
        if (totalOps == 0 || !calculationManager.isExpressionOperatable(expression) || calculationManager.segmentizeExpression(
                expression,
                2 * totalOps + 1
            ) == null
        )
            return false

        var arrayOfExpression =
            calculationManager.segmentizeExpression(expression, 2 * totalOps + 1)!!

        var l = 0
        var k = 0
        while (l < arrayOfExpression.size && k < arrayOfExpression.size) {  //operator in place validity check
            if (arrayOfExpression[l] != null && arrayOfExpression[k] != null) {
                if (arrayOfExpression[k]!!.contains(skivvy.nonNumeralPattern)
                    && !arrayOfExpression[k]!!.contains(".")        //if decimal
                    && !operatorsAndFunctionsBoolean[1].contains(true)
                ) {
                    return false
                }
            } else return false
            ++l
            k += 2
        }

        val midOutput = arrayOfExpression.contentToString().replace("[", nothing)
            .replace("]", nothing).replace(",", nothing).replace(space, nothing)
        if (midOutput != rawExpression.replace(space, nothing))
            setFeedback(midOutput)      //segmentized expression to user

        if (operatorsAndFunctionsBoolean[1].contains(true)) {      //If expression has mathematical functions
            val temp =
                calculationManager.evaluateFunctionsInExpressionArray(arrayOfExpression)
            if (temp == null) {
                return false
            } else {
                arrayOfExpression = temp
            }
        }

        if (!calculationManager.isExpressionArrayOnlyNumbersAndOperators(arrayOfExpression))     //if array contains invalid values
            return false
        else {
            saveCalculationResult(calculationManager.expressionCalculation(arrayOfExpression))
            speakOut(getLastCalculationResult())
        }
        return true
    }


    private fun wifiOps(text: String) {
    }

    private fun deviceLockOps() {
        if (skivvy.deviceManager.isAdminActive(skivvy.compName)) {
            successView(getDrawable(ic_locked))
            speakOut(getString(screen_locked))
            skivvy.deviceManager.lockNow()
        } else {
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

    //TODO: This
    private fun brightnessOps(action: String) {
        when {
            action.contains(skivvy.numberPattern) -> {
                val percent = action.replace(skivvy.nonNumeralPattern, nothing).toFloat()
                if (percent > 100F) {
                    speakOut("Invalid volume level.")
                    return
                }
            }
        }
    }

    private fun volumeOps(action: String) {
        when {
            action.contains(skivvy.numberPattern) -> {
                val percent = action.replace(skivvy.nonNumeralPattern, nothing).toFloat()
                if (percent > 100F) {
                    speakOut("Invalid volume level.")
                    return
                }
                if (percent > 75F) {
                    temp.setVolumePercent(percent)
                    speakOut(
                        "Volume at this level might cause harm to you. Are you sure about ${percent.toInt()}%?",
                        skivvy.CODE_VOLUME_CONFIRM,
                        true
                    )
                } else {
                    feature.setMediaVolume(percent, audioManager)
                    speakOut(
                        "Volume at ${percent.toInt()}%", null,
                        isFeedback = true
                    )
                }
            }
            action.contains("up") || action.contains("increase") ||
                    action.contains("raise") -> {
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
                        isFeedback = true
                    )
                }
            }
            action.contains("down") || action.contains("decrease") -> {
                if (
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) ==
                                audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
                    } else {
                        audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
                    }
                ) {
                    speakOut(getString(R.string.volume_lowest))
                } else {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                    speakOut(
                        getString(volume_decreased),
                        null,
                        isFeedback = true
                    )
                }
            }
            action.contains("max") ||
                    action.contains("full") || action.contains("highest") -> {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    AudioManager.FLAG_SHOW_UI
                )
                speakOut(getString(volume_highest))
            }
            action.contains("min") || action.contains("lowest") -> {
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
                speakOut("Volume at ${feature.getMediaVolume(audioManager)}%")
            }
        }
    }

    //TODO: specific app actions
    //actions invoking other applications
    private fun appOptions(text: String?): Boolean {
        var localText: String
        if (text != null) {
            localText = input.removeBeforeLastStringsIn(
                text,
                arrayOf(resources.getStringArray(R.array.initiators))
            )
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
                        speakOut("What?", skivvy.CODE_SPEECH_RECORD, parallelReceiver = true)
                        return true
                    }
                    when {
                        localText == getString(app_name).toLowerCase(skivvy.locale) -> {
                            normalView()
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
                                    getString(do_u_want_open) + "${packages.getPackageAppName(i)!!
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

    //action invoking direct intents
    private fun directActions(text: String): Boolean {
        var localTxt: String
        when {
            //TODO: continued calculation
            text.contains(getString(call)) -> {
                waitingView(getDrawable(ic_phone_dialer))
                localTxt = text.replace(getString(call), nothing, true).trim()
                temp.setPhone(text.replace(skivvy.nonNumeralPattern, nothing))
                if (temp.getPhone() != null) {
                    when {
                        temp.getPhone()!!.contains(skivvy.numberPattern) -> {
                            temp.setPhone(formatPhoneNumber(temp.getPhone()!!))
                            if (ActivityCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CALL_PHONE
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                speakOut(getString(require_physical_permission))
                                ActivityCompat.requestPermissions(
                                    this,
                                    arrayOf(Manifest.permission.CALL_PHONE),
                                    skivvy.CODE_CALL_REQUEST
                                )
                            } else {
                                speakOut(
                                    getString(should_i_call) + "${temp.getPhone()}?",
                                    skivvy.CODE_CALL_CONF
                                )
                            }
                        }
                        else -> {
                            if (localTxt.length > 1) {
                                contactOps(localTxt, skivvy.CODE_CALL_CONF)
                            } else {
                                txt = "call "
                                speakOut("Call who?", skivvy.CODE_SPEECH_RECORD)
                            }
                        }
                    }
                } else {
                    txt = "call "
                    speakOut("Call who?", skivvy.CODE_SPEECH_RECORD)
                }
            }
            text.contains(getString(email)) -> {
                waitingView(getDrawable(ic_envelope_open))
                localTxt = text.replace(getString(email), nothing, true).trim()
                temp.setEmail(localTxt.replace(space, nothing).trim())
                when {
                    temp.getEmail()!!.matches(skivvy.emailPattern) -> {
                        inputText.text = temp.getEmail()
                        speakOut(getString(what_is_subject), skivvy.CODE_EMAIL_CONTENT)
                    }
                    localTxt.length > 1 -> {
                        contactOps(localTxt, skivvy.CODE_EMAIL_CONF)
                    }
                    else -> {
                        txt = "email "
                        speakOut("Email who?", skivvy.CODE_SPEECH_RECORD)
                    }
                }
            }
            text.contains(getString(R.string.text)) -> {
                waitingView(getDrawable(ic_message))
                localTxt = text.replace(getString(R.string.text), nothing, false)
                localTxt = localTxt.trim()
                temp.setPhone(localTxt.replace(skivvy.nonNumeralPattern, nothing))
                when {
                    temp.getPhone()!!.contains(skivvy.numberPattern) -> {
                        speakOut(getString(what_is_message), skivvy.CODE_TEXT_MESSAGE_BODY)
                    }
                    /*
                    localTxt.contains("whatsapp") -> {
                        localTxt = localTxt.replace("whatsapp", nothing)
                        localTxt = localTxt.replace("via", nothing)
                        localTxt = localTxt.replace("on", nothing)
                        localTxt.replace(space, nothing)
                        contactOps(localTxt, skivvy.CODE_WHATSAPP_ACTION)
                    }
                    */
                    localTxt.length > 1 -> {
                        contactOps(localTxt, skivvy.CODE_SMS_CONF)
                    }
                    else -> {
                        txt = text
                        speakOut("Text who?", skivvy.CODE_SPEECH_RECORD)
                    }
                }
            }
            else -> return false
        }
        return true
    }

    private fun textMessageOps(target: String, payLoad: String, code: Int) {
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

    private fun callingOps(number: String?, name: String? = null) {
        if (number != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                name?.let { speakOut(getString(calling) + name) }
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CALL_PHONE),
                    skivvy.CODE_CALL_REQUEST
                )
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

    //TODO: Either create contact list data class (created) and use that here, or do direct lookup faster in background.
    private fun contactOps(name: String, code: Int) {
        temp.setContactCode(code)
        temp.setPhoneIndex(0)
        temp.setEmailIndex(0)
        temp.setContactReceived(name.trim())
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            speakOut(getString(require_physical_permission))
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                skivvy.CODE_CONTACTS_REQUEST
            )
        } else {
            /*
            val cd = skivvy.contactDataManager
            if (cd.getTotalContacts() == 0) {
                errorView()
                speakOut(getString(no_contacts_available))
            } else {
                var contactIndex = 0
                while (contactIndex < cd.getTotalContacts()) {
                    if(cd.getContactIDs()[contactIndex].isNullOrEmpty()){
                        speakOut("Please wait",skivvy.CODE_SPEECH_RECORD)
                        normalView()
                    } else if (temp.getContactReceived() == cd.getContactNames()[contactIndex]!!.toLowerCase(skivvy.locale) ||
                        temp.getContactReceived() == cd.getContactNames()[contactIndex]!!.substringBefore(space).toLowerCase(skivvy.locale)||
                        !cd.getContactNicknames()[contactIndex].isNullOrEmpty()&&cd.getContactNicknames()[contactIndex]!!.contains(temp.getContactReceived())
                    ) {
                        temp.setContactIndex(contactIndex)
                        temp.setContactPresence(true)
                        if (cd.getContactPhotoUris()[contactIndex] != null) {
                            val rb: RoundedBitmapDrawable =
                                RoundedBitmapDrawableFactory.create(resources, MediaStore.Images.Media.getBitmap(
                                    context.contentResolver,
                                    Uri.parse(cd.getContactPhotoUris()[contactIndex])
                                ))
                            rb.isCircular = true
                            rb.setAntiAlias(true)
                            waitingView(rb)
                        } else waitingView(null)
                        if (temp.getContactCode() == skivvy.CODE_EMAIL_CONF) {
                            if (!cd.getContactEmails()[contactIndex].isNullOrEmpty()) {
                                speakOut(getString(what_is_subject), skivvy.CODE_EMAIL_CONTENT)
                            } else {
                                errorView()
                                speakOut(
                                    getString(you_dont_seem_having) + cd.getContactNames()[contactIndex] + getString(
                                        someone_email_address
                                    )
                                )
                            }
                        } else if (!cd.getContactPhones()[contactIndex].isNullOrEmpty()) {
                            when (code) {
                                skivvy.CODE_CALL_CONF -> {
                                    if (cd.getContactPhones()[contactIndex]!!.size == 1) {
                                        speakOut(
                                            getString(should_i_call) + "${cd.getContactNames()[contactIndex]}?",
                                            skivvy.CODE_CALL_CONF
                                        )
                                    } else {
                                        speakOut(
                                            "I've got ${cd.getContactPhones()[contactIndex]!!.size} phone numbers of ${cd.getContactNames()[contactIndex]}.\nShould I call them at " +
                                                    "${cd.getContactPhones()[contactIndex]?.get(
                                                        temp.getPhoneIndex()
                                                    )}?",
                                            skivvy.CODE_CALL_CONF
                                        )
                                    }
                                }
                                skivvy.CODE_SMS_CONF -> {
                                    speakOut(
                                        getString(what_is_message),
                                        skivvy.CODE_TEXT_MESSAGE_BODY
                                    )
                                }
                            }
                        } else if (cd.getContactPhones()[contactIndex].isNullOrEmpty()) {
                            errorView()
                            speakOut(
                                getString(you_dont_seem_having) + cd.getContactNames()[contactIndex] + getString(
                                    someones_phone_number
                                )
                            )
                        }
                    }
                    ++contactIndex
                }
                if (!temp.getContactPresence()) {
                    errorView()
                    speakOut(getString(contact_not_found))
                }
            }
        }
        */
            val cr: ContentResolver = contentResolver
            val cur: Cursor? =
                cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
            if (cur?.count!! > 0) {
                while (cur.moveToNext()) {
                    contact.contactID =
                        cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID))
                    contact.displayName =
                        cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                    val nickCur: Cursor? = cr.query(        //for nicknames
                        ContactsContract.Data.CONTENT_URI,
                        null,
                        ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
                        arrayOf(
                            contact.contactID,
                            ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE
                        ),
                        null
                    )
                    if (nickCur!!.count > 0) {
                        var nc = 0
                        contact.nickName = arrayOfNulls(nickCur.count)
                        while (nickCur.moveToNext()) {
                            contact.nickName!![nc] =
                                nickCur.getString(nickCur.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME))
                                    ?.toLowerCase(skivvy.locale)
                            ++nc
                        }
                    }
                    nickCur.close()
                    val fName = contact.displayName.substringBefore(space)
                    if (temp.getContactReceived() == contact.displayName.toLowerCase(skivvy.locale) || temp.getContactReceived() == fName.toLowerCase(
                            skivvy.locale
                        ) ||
                        !contact.nickName.isNullOrEmpty() && contact.nickName!!.contains(temp.getContactReceived())
                    ) {
                        temp.setContactPresence(true)
                        val dpUri =
                            cur.getString(cur.getColumnIndex(ContactsContract.Contacts.PHOTO_URI))
                        if (dpUri != null) {
                            contact.photoID = dpUri
                            waitingView(cropToCircle(contact.photoID))
                        }
                        val eCur: Cursor? = cr.query(
                            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                            arrayOf(contact.contactID),
                            null
                        )
                        var size = 0
                        while (eCur!!.moveToNext()) {
                            ++size
                        }
                        eCur.moveToFirst()
                        if (size > 0) {
                            contact.emailList = arrayOfNulls(size)
                            var k = 0
                            while (k < size) {
                                contact.emailList!![k] =
                                    eCur.getString(eCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA))
                                eCur.moveToNext()
                                ++k
                            }
                            if (temp.getContactCode() == skivvy.CODE_EMAIL_CONF) {
                                speakOut(getString(what_is_subject), skivvy.CODE_EMAIL_CONTENT)
                            }
                            eCur.close()
                        } else {
                            if (temp.getContactCode() == skivvy.CODE_EMAIL_CONF) {
                                errorView()
                                speakOut(
                                    getString(you_dont_seem_having) + contact.displayName + getString(
                                        someone_email_address
                                    )
                                )
                            }
                        }
                        if (temp.getContactCode() == skivvy.CODE_CALL_CONF || temp.getContactCode() == skivvy.CODE_SMS_CONF || temp.getContactCode() == skivvy.CODE_WHATSAPP_ACTION) {
                            if (cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                                    .toInt() > 0
                            ) {
                                val pCur: Cursor? = cr.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    arrayOf(contact.contactID),
                                    null
                                )
                                pCur!!.moveToFirst()
                                //size = 0
                                //while (pCur.moveToNext()) {
                                //++size
                                //}
                                contact.phoneList = arrayOfNulls(pCur.count)
                                //pCur.moveToFirst()
                                var k = 0
                                while (k < pCur.count) {
                                    contact.phoneList!![k] = formatPhoneNumber(
                                        pCur.getString(
                                            pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                        )
                                    )
                                    pCur.moveToNext()
                                    ++k
                                }

                                k = 0
                                while (k + 1 < contact.phoneList!!.size) {
                                    if (contact.phoneList!![k] == contact.phoneList!![k + 1]) {
                                        contact.phoneList!![k + 1] = nothing
                                    }
                                    ++k
                                }
                                //TODO: set size according to final phone list, which also contains nothing for duplicate phones, as per the above code
                                if (pCur.count == 1) {
                                    when (temp.getContactCode()) {
                                        skivvy.CODE_CALL_CONF -> {
                                            speakOut(
                                                getString(should_i_call) + "${contact.displayName}?",
                                                skivvy.CODE_CALL_CONF
                                            )
                                        }
                                        skivvy.CODE_SMS_CONF -> {
                                            speakOut(
                                                getString(what_is_message),
                                                skivvy.CODE_TEXT_MESSAGE_BODY
                                            )
                                        }
                                        skivvy.CODE_WHATSAPP_ACTION -> {
                                            speakOut(
                                                "What is your message to ${contact.displayName} via whatsapp?",
                                                skivvy.CODE_WHATSAPP_ACTION
                                            )
                                        }
                                    }
                                } else if (pCur.count > 1) {
                                    if (temp.getContactCode() == skivvy.CODE_CALL_CONF) {
                                        speakOut(
                                            "I've got ${pCur.count} phone numbers of ${contact.displayName}.\nShould I call them at " +
                                                    "${contact.phoneList!![temp.getPhoneIndex()]}?",
                                            skivvy.CODE_CALL_CONF
                                        )
                                    } else if (temp.getContactCode() == skivvy.CODE_SMS_CONF) {
                                        speakOut(
                                            getString(what_is_message),
                                            skivvy.CODE_TEXT_MESSAGE_BODY
                                        )
                                    } else if (temp.getContactCode() == skivvy.CODE_WHATSAPP_ACTION) {
                                        speakOut("Not yet supported")
                                        /*speakOut(
                                            "What is your message to ${contact.displayName} via whatsapp?",
                                            skivvy.CODE_WHATSAPP_ACTION
                                        )
                                         */
                                    }
                                } else {
                                    errorView()
                                    speakOut(
                                        getString(you_dont_seem_having) + contact.displayName + getString(
                                            someones_phone_number
                                        )
                                    )
                                }
                                pCur.close()
                            } else {
                                errorView()
                                speakOut(
                                    getString(you_dont_seem_having) + contact.displayName + getString(
                                        someones_phone_number
                                    )
                                )
                            }
                        }
                        eCur.close()
                        break
                    } else temp.setContactPresence(false)
                }
            } else {
                errorView()
                speakOut(getString(no_contacts_available))
            }
            cur.close()
            if (!temp.getContactPresence()) {
                errorView()
                speakOut(getString(contact_not_found))
            }
        }
    }

    private fun cropToCircle(uri: String): RoundedBitmapDrawable {
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
                        null
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
                        null
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

    //TODO:  airplane mode, power off, restart phone,brightness,auto rotation,hotspot, specific settings

    private fun isAirplaneModeEnabled(): Boolean {
        return Settings.System.getInt(contentResolver, Settings.System.AIRPLANE_MODE_ON, 0) == 1
    }

    //TODO: Airplane mode not turning on
    private fun setAirplaneMode(status: Boolean) {
        Settings.System.putInt(
            contentResolver, Settings.System.AIRPLANE_MODE_ON,
            if (status) 1
            else 0
        )
        try {
            sendBroadcast(
                Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).putExtra(
                    "state", status
                )
            )
        } catch (e: SecurityException) {
            if (!Settings.System.canWrite(context)) {
                speakOut(getString(request_settings_write_permit))
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS),
                    skivvy.CODE_SYSTEM_SETTINGS
                )
            }
        }
    }

    private fun setFlashLight(status: Boolean) {
        try {
            val mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val mCameraId = mCameraManager.cameraIdList[0]
            mCameraManager.setTorchMode(mCameraId, status)
        } catch (e: CameraAccessException) {
            speakOut(getString(flash_access_error))
        }
    }

    private fun isFlashAvailable(): Boolean {
        return applicationContext.packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
    }

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

    private fun callStateListener() {
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, number: String) {
                when (state) {
                    lastState -> return
                    TelephonyManager.CALL_STATE_RINGING -> {
                        speakOut("Incoming call from ${formatPhoneNumber(number)}")
                        successView(getDrawable(ic_phone_dialer))
                        feature.setMediaVolume(
                            audioManager.getStreamVolume(AudioManager.STREAM_RING).toFloat(),
                            audioManager
                        )
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                            waitingView(getDrawable(ic_phone_dialer))
                            speakOut("Speaking to ${formatPhoneNumber(number)}")
                        } else {
                            if (temp.getContactPresence()) {
                                speakOut("Calling ${contact.displayName}")
                            } else {
                                speakOut("Calling ${formatPhoneNumber(number)}")
                            }
                        }
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                            speakOut("You missed a call from ${formatPhoneNumber(number)}")
                            errorView()
                        } else if (lastState == TelephonyManager.CALL_STATE_OFFHOOK) {
                            if (temp.getContactPresence()) {
                                speakOut("Call ended with ${contact.displayName}")
                            } else {
                                speakOut("Call ended with\n${formatPhoneNumber(number)}")
                            }
                        }
                    }
                }
                lastState = state
            }
        }
        telephonyManager!!.listen(
            phoneStateListener,
            PhoneStateListener.LISTEN_CALL_STATE
        )
    }

    override fun onDestroy() {
        loading.startAnimation(zoomInRotate)
        speakOut(getString(exit_msg))
        if (skivvy.tts != null) {
            skivvy.tts!!.stop()
            skivvy.tts!!.shutdown()
        }
        super.onDestroy()
    }

    override fun onBackPressed() {
        speakOut(getString(exit_msg))
        loading.startAnimation(zoomInRotate)
        if (skivvy.tts != null) {
            skivvy.tts!!.stop()
            skivvy.tts!!.shutdown()
        }
        super.onBackPressed()
    }

    /**
     * Following function checks whether the given string has last response as acceptance or denial
     * @param response : the given string of response
     * @return : returns boolean value according to content of given response (true for positive, false for negative, null for invalid response)
     * @note: The space is padded in [response] whenever it is singular (i.e., with no spaces),
     * because it should not be a substring of any other string.
     * For example - 'no' in knowledge cannot be treated as a valid response.
     * TODO: extend this to other types of responses too.
     */
    private fun isCooperative(response: String): Boolean? {
        return when {
            input.containsString(
                input.removeBeforeLastStringsIn(
                    response,
                    arrayOf(resources.getStringArray(R.array.acceptances))
                ),
                arrayOf(
                    resources.getStringArray(R.array.disruptions),
                    resources.getStringArray(R.array.denials)
                )
            ) -> false      //if denial was the last response in string
            input.containsString(
                input.removeBeforeLastStringsIn(
                    response,
                    arrayOf(
                        resources.getStringArray(R.array.disruptions),
                        resources.getStringArray(R.array.denials)
                    )
                ), arrayOf(resources.getStringArray(R.array.acceptances))
            ) -> true       //if acceptance was the last response in string
            input.containsString(
                " $response ",
                arrayOf(
                    resources.getStringArray(R.array.disruptions),
                    resources.getStringArray(R.array.denials)
                ),
                isSingle = true
            ) -> false      //if response contains denial
            input.containsString(
                " $response ",
                arrayOf(resources.getStringArray(R.array.acceptances)),
                isSingle = true
            ) -> true       //if response contains acceptance
            else -> null        //if response was invalid
        }
    }

    private fun normalView(fromTraining: Boolean = false) {
        loading.startAnimation(zoomInOutRotate)
        loading.setImageDrawable(getDrawable(dots_in_circle))
        inputText.text = null
        setOutput(null)
        setFeedback(null)
        icon.setImageDrawable(null)
        contact = ContactModel()
        temp = TempDataManager()
        txt = null
        if (fromTraining) {
            startResumeAnimate()
            receiver.isClickable = true
            setting.isClickable = true
            receiver.visibility = View.VISIBLE
            greet.visibility = View.VISIBLE
            setting.visibility = View.VISIBLE
            setOutput(getString(im_ready))
            inputText.text = getString(tap_the_button)
        }
    }

    private fun trainingView() {
        skivvy.saveMuteStatus(false)
        backfall.startAnimation(AnimationUtils.loadAnimation(context, R.anim.extend_back))
        receiver.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_off))
        loading.startAnimation(AnimationUtils.loadAnimation(context, R.anim.rotate_slow))
        loading.setImageDrawable(getDrawable(dots_in_circle_yellow))
        receiver.isClickable = false
        setting.isClickable = false
        greet.text = getString(traning_title)
        receiver.visibility = View.GONE
        setting.visibility = View.GONE
    }

    fun waitingView(image: Drawable?) {
        loading.startAnimation(focusRotate)
        loading.setImageDrawable(getDrawable(dots_in_circle_yellow))
        if (image != null) {
            icon.setImageDrawable(image)
        }
    }

    fun errorView(image: Drawable? = null): Boolean {
        loading.startAnimation(fadeOnFadeOff)
        loading.setImageDrawable(getDrawable(dots_in_circle_red))
        image?.let { icon.setImageDrawable(image) }
        return false
    }

    fun successView(image: Drawable?) {
        loading.startAnimation(focusDeFocusRotate)
        loading.setImageDrawable(getDrawable(dots_in_circle_green))
        if (image != null) {
            icon.setImageDrawable(image)
        }
    }

    private fun setOutput(text: String?) {
        outputText.startAnimation(fadeOnFast)
        outputText.text = text
    }

    private fun speakOut(
        text: String,
        taskCode: Int? = null,
        parallelReceiver: Boolean = skivvy.getParallelResponseStatus(),
        isFeedback: Boolean = false
    ) {
        if (isFeedback) setFeedback(text)
        else setOutput(text)
        skivvy.tts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String) {
                if (!parallelReceiver)
                    taskCode?.let { startVoiceRecIntent(it, text) }
            }

            override fun onError(utteranceId: String) {}
            override fun onStart(utteranceId: String) {
                if (parallelReceiver)
                    taskCode?.let { startVoiceRecIntent(it, text) }
            }
        })
        if (!skivvy.getMuteStatus()) skivvy.tts!!.speak(
            text.replace("\n", nothing),
            TextToSpeech.QUEUE_FLUSH,
            null,
            "$taskCode"
        )
        else
            taskCode?.let { startVoiceRecIntent(it, text) }
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
                            skivvy.setBiometricsStatus(false)
                            speakOut(getString(biometric_is_off))
                        }
                        skivvy.CODE_VOICE_AUTH_CONFIRM -> {
                            skivvy.setPhraseKeyStatus(false)
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

    private fun String.isLandLine(): Boolean {
        return this[0] == '0' && this[1] == '1' && this[2] == '2' && this[3] == '0'
    }

    private fun String.hasCountryCode(country: String): Boolean {
        if (country == "IN")
            return this[0] == '+' && this[1] == '9' && this[2] == '1'
        return false
    }
}
