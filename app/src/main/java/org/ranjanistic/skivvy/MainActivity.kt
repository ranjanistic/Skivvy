package org.ranjanistic.skivvy

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.format.DateFormat
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ranjanistic.skivvy.R.drawable.*
import org.ranjanistic.skivvy.R.string.*
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.function.BinaryOperator

@ExperimentalStdlibApi
class MainActivity : AppCompatActivity(){
    private var tts: TextToSpeech? = null

    private var outPut: TextView? = null
    private var input: TextView? = null
    private var focusRotate: Animation? = null
    private var normalRotate: Animation? = null
    private var rotateSlow: Animation? = null
    private var exitAnimation: Animation? = null
    private var fadeAnimation: Animation? = null
    private var bubbleAnimation: Animation? = null
    private var fallAnimation: Animation? = null
    private var riseAnimation:Animation? = null
    private var extendAnimation:Animation? = null
    private var fadeOffAnimation:Animation? = null
    private var translateAnimation:Animation? = null
    private var receiver: TextView? = null
    private lateinit var setting:ImageButton
    private var greet: TextView? = null
    private var tempPackageIndex:Int? = null
    private var tempPhoneNumberIndex:Int? = 0
    private var tempEmailIndex:Int? = 0
    private var loading: ImageView? = null
    private var outputStat:ImageView? = null
    private var icon: ImageView? = null
    private var txt: String? = null
    private var tempPhone:String? = null
    private var tempMail:String? = null
    private var tempMailSubject:String? = null
    private var tempMailBody:String? = null
    private var tempContact:String? = null
    private var tempContactCode:Int? = null
    private lateinit var backfall: ImageView
    private lateinit var packagesAppName:Array<String?>
    private lateinit var packagesName:Array<String?>
    private lateinit var packagesMain:Array<Intent?>
    private lateinit var packagesIcon:Array<Drawable?>
    lateinit var skivvy:Skivvy
    private lateinit var context:Context
    private var packagesTotal:Int = 0
    private var deviceManger: DevicePolicyManager? = null
    private var compName: ComponentName? = null
    private var contact:ContactModel = ContactModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this
        skivvy = this.application as Skivvy
        setViewAndDefaults()
        loadDefaultAnimations()
        normalView()
        tts = TextToSpeech(context, TextToSpeech.OnInitListener {
            when(it){
                TextToSpeech.SUCCESS ->{
                    when(tts!!.setLanguage(skivvy.locale)){
                        TextToSpeech.LANG_MISSING_DATA,
                        TextToSpeech.LANG_NOT_SUPPORTED -> outPut!!.text =  getString(language_not_supported)
                    }
                } else -> outPut!!.text =  getString(output_error)
            }
        })
        setListeners()
        //Long running task
        GlobalScope.launch {
            getLocalPackages()
        }
    }
    private fun setViewAndDefaults(){
        //skivvy.locale = Locale.US
        setting = findViewById(R.id.setting)
        outPut = findViewById(R.id.textOutput)
        input = findViewById(R.id.textInput)
        loading = findViewById(R.id.loader)
        icon = findViewById(R.id.actionIcon)
        receiver = findViewById(R.id.receiverBtn)
        greet = findViewById(R.id.greeting)
        backfall = findViewById(R.id.backdrop)
        outputStat = findViewById(R.id.outputStatusView)
        outputStat!!.visibility = View.INVISIBLE
    }

    private fun loadDefaultAnimations(){
        fallAnimation = AnimationUtils.loadAnimation(context,R.anim.fall_back)
        riseAnimation = AnimationUtils.loadAnimation(context,R.anim.rise_back)
        bubbleAnimation = AnimationUtils.loadAnimation(context,R.anim.bubble_wave)
        normalRotate = AnimationUtils.loadAnimation(context, R.anim.rotate_emerge_demerge)
        focusRotate = AnimationUtils.loadAnimation(context, R.anim.rotate_focus)
        rotateSlow = AnimationUtils.loadAnimation(context, R.anim.rotate_slow)
        fadeAnimation = AnimationUtils.loadAnimation(context, R.anim.fade)
        exitAnimation = AnimationUtils.loadAnimation(context, R.anim.rotate_exit)
        /*fadeOffAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_off)
        translateAnimation = AnimationUtils.loadAnimation(context, R.anim.scale_translate_setting)
        extendAnimation = AnimationUtils.loadAnimation(context, R.anim.extend_back)
         */
        backfall.startAnimation(fallAnimation)
        setting.startAnimation(bubbleAnimation)
        receiver!!.startAnimation(bubbleAnimation)
        greet!!.startAnimation(bubbleAnimation)
    }
    private fun startSettingAnimate(){
        setting.startAnimation(translateAnimation)
        backfall.startAnimation(extendAnimation)
        loading!!.startAnimation(exitAnimation)
    }
    private fun setListeners(){
/*        translateAnimation!!.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(p0: Animation?) {
            }
            override fun onAnimationRepeat(p0: Animation?) {}
            override fun onAnimationStart(p0: Animation?) {}
        })

 */
        setting.setOnClickListener {
            setButtonsClickable(false)
            startActivity(Intent(context,Setup::class.java))
            //overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out)
        }
        receiver?.setOnClickListener {
            setButtonsClickable(false)
            normalView()
            startVoiceRecIntent(skivvy.CODE_SPEECH_RECORD)
        }
    }

    override fun onStart() {
        super.onStart()
        setButtonsClickable(true)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            skivvy.CODE_ALL_PERMISSIONS->{
                //TODO: if(grantResults[])
            }
            skivvy.CODE_CALL_REQUEST -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    when {
                        contact.phoneList.isNotEmpty() -> {
                            speakOut(getString(should_i_call) + "${contact.displayName} at ${contact.phoneList[tempPhoneNumberIndex!!]}?", skivvy.CODE_CONTACT_CALL_CONF)
                        }
                        tempPhone!=null -> {
                            speakOut(getString(should_i_call) + "$tempPhone?", skivvy.CODE_CALL_CONF)
                        }
                        else -> {
                            speakOut(getString(null_variable_error))
                        }
                    }
                } else {
                    tempPhone = null
                    errorView()
                    speakOut(getString(call_permit_denied))
                }
            }
            skivvy.CODE_STORAGE_REQUEST ->{
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    takeScreenshot()
                } else {
                    errorView()
                    speakOut(getString(storage_permission_denied))
                }
            }
            skivvy.CODE_CONTACTS_REQUEST->{
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(tempContact!=null && tempContactCode!=null) {
                        contactOps(tempContact!!,tempContactCode!!)
                    } else {
                        speakOut(getString(null_variable_error))
                    }
                }  else {
                    errorView()
                    speakOut(getString(contact_permission_denied))
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode == KeyEvent.KEYCODE_HEADSETHOOK){
            if(outputStat!!.visibility != View.VISIBLE) {
                setButtonsClickable(false)
                normalView()
                startVoiceRecIntent(skivvy.CODE_SPEECH_RECORD)
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        setButtonsClickable(true)
        tts!!.language = skivvy.locale
        when (requestCode) {
            skivvy.CODE_SPEECH_RECORD -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    txt = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)[0].toString()
                        .toLowerCase(skivvy.locale)
                    if (txt != null) {
                        input?.text = txt
                        if (!respondToCommand(txt!!)) {
                            if (!appOptions(txt)) {
                                if (!directActions(txt!!)) {
                                    errorView()
                                    speakOut(getString(recognize_error))
                                }
                            }
                        }
                    }
                } else {
                    speakOut(getString(no_input))
                }
            }
            skivvy.CODE_OTHER_APP_CONF -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    txt = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)[0].toString()
                        .toLowerCase(skivvy.locale)
                    if (txt != null) {
                        if (resources.getStringArray(R.array.acceptances).contains(txt)) {
                            if (tempPackageIndex != null) {
                                successView(packagesIcon[tempPackageIndex!!])
                                speakOut(getString(opening) + packagesAppName[tempPackageIndex!!])
                                startActivityForResult(
                                    Intent(packagesMain[tempPackageIndex!!]),
                                    skivvy.CODE_OTHER_APP
                                )
                                tempPackageIndex = null
                            } else {
                                errorView()
                                speakOut(getString(null_variable_error))
                            }
                        } else if (resources.getStringArray(R.array.denials).contains(txt)) {
                            tempPackageIndex = null
                            normalView()
                            speakOut(getString(okay))
                        } else {
                            waitingView(packagesIcon[tempPackageIndex!!])
                            speakOut(
                                getString(recognize_error) + getString(do_u_want_open) + packagesAppName[tempPackageIndex!!] + "?",
                                skivvy.CODE_OTHER_APP_CONF
                            )
                        }
                    }
                } else {
                    normalView()
                    speakOut(getString(no_input))
                }
            }
            skivvy.CODE_OTHER_APP -> {
                txt = null
                tempPackageIndex = null
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
            skivvy.CODE_LOCK_SCREEN -> {
                if (resultCode == Activity.RESULT_OK ) {
                    deviceLockOps()
                } else {
                    errorView()
                    speakOut(getString(device_admin_failure))
                }
            }
            skivvy.CODE_CALL_CONF -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    txt = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)[0].toString()
                        .toLowerCase(skivvy.locale)
                    when {
                        resources.getStringArray(R.array.acceptances).contains(txt) -> {
                            successView(getDrawable(ic_glossyphone))
                            callingOps(tempPhone)
                            tempPhone = null
                        }
                        resources.getStringArray(R.array.denials).contains(txt) -> {
                            tempPhone = null
                            normalView()
                            speakOut(getString(okay))
                        }
                        else -> {
                            waitingView(getDrawable(ic_glossyphone))
                            speakOut(
                                getString(recognize_error) + getString(should_i_call) + "$tempPhone?",
                                skivvy.CODE_CALL_CONF
                            )
                        }
                    }
                } else {
                    normalView()
                    speakOut(getString(no_input))
                }
            }
            skivvy.CODE_CONTACT_CALL_CONF -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    txt = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)[0].toString().toLowerCase(skivvy.locale)
                    when {
                        resources.getStringArray(R.array.acceptances).contains(txt) -> {
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                speakOut(getString(require_physical_permission))
                                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), skivvy.CODE_CALL_REQUEST)
                            } else {
                                successView(null)
                                callingOps(contact.phoneList[tempPhoneNumberIndex!!], contact.displayName)
                            }
                        }
                        resources.getStringArray(R.array.denials).contains(txt) -> {
                            tempPhoneNumberIndex = tempPhoneNumberIndex!!+1
                            if(tempPhoneNumberIndex!=0 && tempPhoneNumberIndex!! < contact.phoneList.size && !resources.getStringArray(R.array.disruptions).contains(txt)){
                                speakOut("At ${contact.phoneList[tempPhoneNumberIndex!!]}?",skivvy.CODE_CONTACT_CALL_CONF)
                            } else {
                                normalView()
                                speakOut(getString(okay))
                            }
                        }
                        else -> {
                            speakOut(getString(recognize_error) + getString(should_i_call) + "${contact.displayName} at ${contact.phoneList[tempPhoneNumberIndex!!]}?", skivvy.CODE_CONTACT_CALL_CONF)
                        }
                    }
                } else {
                    normalView()
                    speakOut(getString(no_input))
                }
            }
            skivvy.CODE_EMAIL_CONF -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    txt = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)[0].toString()
                        .toLowerCase(skivvy.locale)
                    when {
                        resources.getStringArray(R.array.acceptances).contains(txt) -> {
                            //successView(null)
                            speakOut(getString(what_is_subject),skivvy.CODE_EMAIL_SUBJECT)
                            //emailingOps(tempMail!!)
                        }
                        resources.getStringArray(R.array.denials).contains(txt) -> {
                            tempMail = null
                            normalView()
                            speakOut(getString(okay))
                        }
                        else -> {
                            speakOut(
                                getString(recognize_error) + getString(should_i_email)+"$tempMail?",
                                skivvy.CODE_EMAIL_CONF
                            )
                        }
                    }
                } else {
                    normalView()
                    speakOut(getString(no_input))
                }
            }
            skivvy.CODE_CONTACT_EMAIL_CONF->{
                if (resultCode == Activity.RESULT_OK && data != null) {
                    txt = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)[0].toString()
                        .toLowerCase(skivvy.locale)
                    when {
                        resources.getStringArray(R.array.acceptances).contains(txt) -> {
                            tempMail = contact.emailList[tempEmailIndex!!]
                            speakOut(getString(what_is_subject),skivvy.CODE_EMAIL_SUBJECT)
                        }
                        resources.getStringArray(R.array.denials).contains(txt) -> {
                            tempEmailIndex = tempEmailIndex!!+1
                            if(tempEmailIndex!=0 && tempEmailIndex!! < contact.emailList.size && !resources.getStringArray(R.array.disruptions).contains(txt)){
                                speakOut("At ${contact.emailList[tempEmailIndex!!]}?",skivvy.CODE_CONTACT_EMAIL_CONF)
                            } else {
                                normalView()
                                speakOut(getString(okay))
                            }
                        }
                        else -> {
                            speakOut(getString(recognize_error) + getString(should_i_email)+"${contact.emailList[tempEmailIndex!!]}?",
                                skivvy.CODE_CONTACT_EMAIL_CONF
                            )
                        }
                    }
                } else {
                    normalView()
                    speakOut(getString(no_input))
                }
            }
            skivvy.CODE_EMAIL_SUBJECT->{
                if (resultCode == Activity.RESULT_OK && data != null) {
                    txt = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)[0].toString()
                        .toLowerCase(skivvy.locale)
                    when {
                        resources.getStringArray(R.array.disruptions).contains(txt)->{
                            normalView()
                            speakOut(getString(okay))
                        }
                        txt!=null -> {
                            tempMailSubject = txt
                            speakOut(getString(subject_added)+getString(what_is_body),skivvy.CODE_EMAIL_BODY)
                        }
                        else -> {
                            speakOut(getString(recognize_error) + getString(what_is_subject),
                                skivvy.CODE_EMAIL_SUBJECT
                            )
                        }
                    }
                } else {
                    normalView()
                    speakOut(getString(no_input))
                }
            }
            skivvy.CODE_EMAIL_BODY->{
                if (resultCode == Activity.RESULT_OK && data != null) {
                    txt = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)[0].toString()
                        .toLowerCase(skivvy.locale)
                    when {
                        resources.getStringArray(R.array.disruptions).contains(txt)->{
                            normalView()
                            speakOut(getString(okay))
                        }
                        txt!=null -> {
                            successView(null)
                            tempMailBody = txt
                            speakOut(getString(body_added) + getString(preparing_email))
                            emailingOps(tempMail,tempMailSubject,tempMailBody)
                        }
                        else -> {
                            speakOut(getString(recognize_error) + getString(what_is_body),
                                skivvy.CODE_EMAIL_BODY
                            )
                        }
                    }
                } else {
                    normalView()
                    speakOut(getString(no_input))
                }
            }
        }
    }

    //TODO:  Mathematical calculations command
    //actions invoking quick commands
    @ExperimentalStdlibApi
    private fun respondToCommand(text:String):Boolean{
        val array = arrayOf(R.array.setup_list,R.array.bt_list,R.array.wifi_list,R.array.gps_list,R.array.lock_list,R.array.snap_list)
        when {
            resources.getStringArray(array[0]).contains(text) -> {
              startActivity(Intent(context,Setup::class.java))
            }
            resources.getStringArray(array[1]).contains(text) -> {
                bluetoothOps()
            }
            resources.getStringArray(array[2]).contains(text) -> {
                waitingView(getDrawable(ic_wifi_connected))
                wifiOps()
            }
            resources.getStringArray(array[3]).contains(text) -> {
                locationOps()
            }
            resources.getStringArray(array[4]).contains(text) -> {
                deviceLockOps()
            }
            resources.getStringArray(array[5]).contains(text) -> {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    speakOut(getString(require_physical_permission))
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),skivvy.CODE_STORAGE_REQUEST)
                } else {
                    takeScreenshot()
                }
            }
            text.contains("volume")->{
                when {
                    text.contains("up") -> volumeOps(true)
                    text.contains("down") -> volumeOps(false)
                    else -> volumeOps(null)
                }
            }
            text == getString(exit) -> {
                finish()
            }
            text.contains("calculate") || text.contains("compute") ->{
                var expression = text.replace("calculate","")
                Log.d(TAG,expression)
                expression = expression.replace(" ","")
                expression = expression.replace("x","*")
                expression = expression.replace("dividedby","/")
                expression = expression.replace("over","/")
                expression = expression.replace("multipliedby","*")
                expression = expression.replace("into","*")
                expression = expression.replace("plus","+")
                expression = expression.replace("minus","-")
                speakOut(expression)
                if(!computerOps(expression)){
                    speakOut("Invalid expression")
                } else speakOut("Done")
            }
            text == "grant permissions"->{
                if(!hasPermissions(this, *skivvy.permissions)) {
                    ActivityCompat.requestPermissions(
                        this, skivvy.permissions,
                        skivvy.CODE_ALL_PERMISSIONS
                    )
                } else {
                    speakOut("I have all the permissions that I need from you.")
                }
            }
            else -> {
                return false
            }
        }
        return true
    }

    //TODO: Calculator function
    private var TAG = "COPS"
    @ExperimentalStdlibApi
    private fun computerOps(expression:String):Boolean {
        Log.d(TAG, expression)
        val operatorBool:Array<Boolean> = arrayOf(false,false,false,false)
        val operators: Array<Char> = arrayOf('/', '*', '+', '-')
        var opIndex = 0
        while(opIndex<operators.size){
            operatorBool[opIndex] = expression.contains(operators[opIndex])
            ++opIndex
        }
        if(!operatorBool.contains(true)){
            return false
        }

        /**
         *  The following block stores the position of operators in the given expression in  a new array (of Integers),
         *  which will help the further block of code to contain and create a distinction between operands (numbers) and operators.
         */

        var expIndex = 0
        var totalOps = 0
        while(expIndex<expression.length){
            opIndex = 0
            while(opIndex < operators.size) {
                if (expression[expIndex] == operators[opIndex]){
                    ++totalOps         //saving operator positions
                }
                ++opIndex
            }
            ++expIndex
        }
        if(totalOps == 0){
            return false
        }
        expIndex = 0
        val expOperatorPos = arrayOfNulls<Int>(totalOps)
         var expOpIndex = 0
        while(expIndex<expression.length){
            opIndex = 0
            while(opIndex < operators.size) {
                if (expression[expIndex] == operators[opIndex]){
                    expOperatorPos[expOpIndex] = expIndex         //saving operator positions
                    ++expOpIndex
                }
                ++opIndex
            }
            ++expIndex
        }

        /**
         * The following block extracts values from given expression, char by char, and stores them
         * in an array of Strings, by grouping digits in form of numbers at the same index as string, and operators in
         * the expression at a separate index if array of Strings.
         *  For ex - Let the given expression be :   1234/556*89+4-23
         *  Starting from index = 0, the following block will store digits till '/'  at index =0 of empty array of Strings, then
         *  will store '/' itself at index =  1 of empty array of Strings. Then proceeds to store 5, 5  and 6 at the same index = 2 of e.a. of strings.
         *  And stores the next operator '*' at index = 3, and so on. Thus a distinction between operands and operators is created and stored in a new array (of strings).
         */

        var arrayOfExpression = arrayOfNulls<String>(2*totalOps +1)
        var expArrayIndex = 0
        var positionInExpression = 0
        var positionInOperatorPos = 0
        while(positionInOperatorPos<expOperatorPos.size && positionInExpression<expression.length) {
            while (positionInExpression < expOperatorPos[positionInOperatorPos]!!) {
                if(arrayOfExpression[expArrayIndex] == null){
                    arrayOfExpression[expArrayIndex] = expression[positionInExpression].toString()
                } else {
                    arrayOfExpression[expArrayIndex] += expression[positionInExpression].toString()
                }
                ++positionInExpression
            }
            ++expArrayIndex
            if (positionInExpression == expOperatorPos[positionInOperatorPos]) {
                if(arrayOfExpression[expArrayIndex] == null){
                    arrayOfExpression[expArrayIndex] = expression[positionInExpression].toString()
                } else {
                    arrayOfExpression[expArrayIndex] += expression[positionInExpression].toString()
                }
                ++expArrayIndex
            }
            ++positionInExpression
            ++positionInOperatorPos
            if(positionInOperatorPos>=expOperatorPos.size){
                while(positionInExpression<expression.length){
                    if(arrayOfExpression[expArrayIndex] == null){
                        arrayOfExpression[expArrayIndex] = expression[positionInExpression].toString()
                    } else {
                        arrayOfExpression[expArrayIndex] += expression[positionInExpression].toString()
                    }
                    ++positionInExpression
                }
            }
        }
        /**
         * Now, as we have the new array of strings, having the proper expression, with operators at every even position of
         * the array (at odd indices), the following block of code will evaluate the expression according to the BODMAS rule.
         */


        var nullPosCount = 0
        opIndex = 0
        while(opIndex<operators.size){
            var opPos=1
            while(opPos<arrayOfExpression.size-nullPosCount) {
                if (arrayOfExpression[opPos] == operators[opIndex].toString()) {
                    arrayOfExpression[opPos-1] = operate(arrayOfExpression[opPos-1]!!.toInt(),
                        operators[opIndex],
                        arrayOfExpression[opPos+1]!!.toInt()
                    ).toString()
                    var j = opPos
                    while(j+2<arrayOfExpression.size){
                        arrayOfExpression[j] = arrayOfExpression[j+2]
                        ++j
                    }
                    if(arrayOfExpression.size>3&&arrayOfExpression[opPos] == operators[opIndex].toString()){
                        Log.d("isSameOperator", "${operators[opIndex]}")
                        opPos-=2
                        nullPosCount+=2
                    }
                }
                opPos+=2
            }
            ++opIndex
        }

        var m = 0
        while(m<arrayOfExpression.size) {
            if(arrayOfExpression[m]!=null){
                Log.d("arrayAfterDiv", arrayOfExpression[m]!!)
            }else{
                Log.d(TAG, "Nill Div  $m")
            }
            ++m
        }
        return true
        /*
        opIndex = 0
        while(opIndex < operators.size){
            var opPos = 1
            while(opPos < arrayOfExpression.size) {
                if (arrayOfExpression[opPos] == operators[opIndex].toString()) {
                    val n1 = arrayOfExpression[opPos-1]!!.toInt()
                    val n2 = arrayOfExpression[opPos+1]!!.toInt()
                    val n = operate(n1,operators[opIndex],n2)
                    arrayOfExpression[opPos-1]  = n.toString()
                    var j = opPos
                    while(j+2<=arrayOfExpression.size){
                        arrayOfExpression[j] = arrayOfExpression[j+2]
                        arrayOfExpression[j+2] = ""
                        ++j
                    }
                    if(arrayOfExpression[opPos] == operators[opIndex].toString()){
                        val n1 = arrayOfExpression[opPos-1]!!.toInt()
                        val n2 = arrayOfExpression[opPos+1]!!.toInt()
                        val n = operate(n1,operators[opIndex],n2)
                        arrayOfExpression[opPos-1]  = n.toString()
                    }
                }
                opPos+=2
            }
            ++opIndex
        }

        //TODO: Check Nill
        var k = 0
        while(k<arrayOfExpression.size) {
            if(arrayOfExpression[k]!=null){
                Log.d(TAG, arrayOfExpression[k]!!)
            }else{
                Log.d(TAG, "Nill $k")
            }
            ++k
        }
        return true

        while (c < expression.length) {
            if (!operators.contains(expression[c]))
                return false
            else {
                var k = i
                while(k<c)
                arrayOfExpression[i] = expression[c-1].toString()
                ++i
            }
            ++c
        }
        var divExpression: String? = expression
        while(divExpression!!.contains('/')){
            var d1: Float
            var d2: Float
            var i = 1
            while(i<expression.length){
                if(expression[i]=='/'){
                    divExpression = expression.toCharArray(0,i-2).toString()
                    d1 = expression[i-1].toFloat()
                    d2 = expression[i+1].toFloat()
                    val d = d1/d2
                    divExpression += d.toString()
                    divExpression += expression.toCharArray(i+2,expression.length).toString()
                }
                i += 2
            }
        }
        var mulExpression:String? = divExpression
        while(mulExpression!!.contains('*')){
            var m1:Int = 0
            var m2:Int = 0
            var i = 1
            while(i<divExpression.length){
                if(divExpression[i]=='/'){
                    mulExpression = divExpression.toCharArray(0,i-2).toString()
                    m1 = expression[i-1].toInt()
                    m2 = expression[i+1].toInt()
                    val m = m1*m2
                    mulExpression += m.toString()
                    mulExpression+=divExpression.toCharArray(i+2,divExpression.length).toString()
                }
                i += 2
            }
        }
        var sumExpression:String? = mulExpression
        while(sumExpression!!.contains('+')){
            var s1:Int = 0
            var s2:Int = 0
            var i = 1
            while(i<mulExpression.length){
                if(mulExpression[i]=='/'){
                    sumExpression = mulExpression.toCharArray(0,i-2).toString()
                    s1 = expression[i-1].toInt()
                    s2 = expression[i+1].toInt()
                    val s = s1+s2
                    sumExpression+=s.toString()
                    sumExpression+=mulExpression.toCharArray(i+2,mulExpression.length).toString()
                }
                i += 2
            }
        }

        var subExpression:String? = sumExpression
        while(subExpression!!.contains('-')){
            var s1:Int = 0
            var s2:Int = 0
            var i = 1
            while(i<sumExpression.length){
                if(sumExpression[i]=='/'){
                    subExpression.replace(subExpression,sumExpression.toCharArray(0,i-2).toString())
                    s1 = expression[i-1].toInt()
                    s2 = expression[i+1].toInt()
                    val s = s1-s2
                    subExpression.plus(s.toString())
                    subExpression.plus(sumExpression.toCharArray(i+2,sumExpression.length).toString())
                }
                i += 2
            }
        }
        speakOut(subExpression)
        return true

 */
    }

    private fun operate(a:Int, operator:Char, b:Int):Int?{
        return when(operator){
            '/'-> a/b
            '*'-> a*b
            '+'-> a+b
            '-'-> a-b
            else-> null
        }
    }

    private fun hasPermissions(context: Context, vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    private fun bluetoothOps(){
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter.isEnabled) {
            mBluetoothAdapter.disable()
            speakOut(getString(bt_off))
        } else {
            successView(getDrawable(ic_bluetooth))
            mBluetoothAdapter.enable()
            speakOut(getString(bt_on))
        }
    }
    private fun wifiOps(){
        val wifiManager: WifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        if(wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = false
            speakOut(getString(wifi_off))
        } else {
            successView(getDrawable(ic_wifi_connected))
            wifiManager.isWifiEnabled = true
            speakOut(getString(wifi_on))
        }
    }
    private fun locationOps(){
        waitingView(getDrawable(ic_location_pointer))
        startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),skivvy.CODE_LOCATION_SERVICE)
    }
    private fun deviceLockOps(){
        deviceManger = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(context, Administrator::class.java)
        if (deviceManger!!.isAdminActive(compName!!)) {
            successView(getDrawable(ic_glossylock))
            speakOut(getString(screen_locked))
            deviceManger!!.lockNow()
        } else {
            waitingView(getDrawable(ic_glossylock))
            speakOut(getString(device_admin_request))
            startActivityForResult(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
                .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(device_admin_persuation)), skivvy.CODE_LOCK_SCREEN)
        }
    }

    //TODO: More volume customizations
    private fun volumeOps(action:Boolean?){
        val audioManager: AudioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when(action){
            true -> audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            false ->audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
            else ->audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_PLAY_SOUND)
        }
    }

    //actions invoking other applications
    private fun appOptions(text: String?):Boolean{
        var flag = false
        if(text!=null) {
            if (packagesTotal > 0) {
                var i=0
                while (i < packagesTotal) {
                    if(text == getString(app_name).toLowerCase(skivvy.locale)){
                        flag = true
                        speakOut(getString(i_am) +  getString(app_name))
                        break
                    } else if (text == packagesAppName[i]) {
                        flag = successView(packagesIcon[i])
                        speakOut(getString(opening) + packagesAppName[i])
                        startActivityForResult(Intent(packagesMain[i]), skivvy.CODE_OTHER_APP)
                        break
                    } else if (text.let { packagesName[i]!!.indexOf(it) } != -1) {
                        flag = true
                        tempPackageIndex = i
                        waitingView(packagesIcon[i])
                        speakOut(getString(do_u_want_open) + packagesAppName[i]+ "?",skivvy.CODE_OTHER_APP_CONF)
                        break
                    } else {
                        flag = false
                    }
                    ++i
                }
            } else {
                speakOut(getString(internal_error))
            }
        } else {
            flag = errorView()
        }
        return flag
    }

    //action invoking direct intents
    private fun directActions(text: String):Boolean{
        val localTxt:String
        if(text.contains(getString(call))) {
            waitingView(getDrawable(ic_glossyphone))
            localTxt = text.replace(getString(call), "",true)
            tempPhone = text.replace("[^0-9]".toRegex(), "")
            if(tempPhone!=null) {
                when {
                    tempPhone!!.contains(skivvy.phonePattern) ->{
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            speakOut(getString(require_physical_permission))
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), skivvy.CODE_CALL_REQUEST)
                        } else {
                            speakOut(
                                getString(should_i_call) + "$tempPhone?",
                                skivvy.CODE_CALL_CONF
                            )
                        }
                    }
                    else -> {
                        if(localTxt.length>1) {
                            contactOps(localTxt, skivvy.CODE_CONTACT_CALL_CONF)
                        } else {
                            errorView()
                            speakOut(getString(invalid_call_request))
                        }
                    }
                }
            } else return false
            return true
        } else if(text.contains(getString(email))){
            waitingView(getDrawable(ic_email_envelope))
            localTxt = text.replace(getString(email),"",true)
            tempMail = localTxt.replace(" ","")
            tempMail = tempMail!!.trim()
            when {
                tempMail!!.matches(skivvy.emailPattern) -> {
                    speakOut(getString(should_i_email) + "$tempMail?",skivvy.CODE_EMAIL_CONF)
                }
                localTxt.length>1 -> {
                    contactOps(localTxt,skivvy.CODE_CONTACT_EMAIL_CONF)
                }
                else -> {
                    errorView()
                    speakOut(getString(invalid_email_request))
                }
            }
            return true
        }
        return false
    }
    @SuppressLint("MissingPermission")
    private fun callingOps(number:String?){
        if(number!=null) {
            speakOut(getString(calling)+"$number")
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$number")
            startActivity(intent)
        } else {
            errorView()
            speakOut(getString(null_variable_error))
        }
    }
    @SuppressLint("MissingPermission")
    private fun callingOps(number:String?,name: String){
        if(number!=null) {
            speakOut(getString(calling)+name)
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$number")
            startActivity(intent)
        } else {
            errorView()
            speakOut(getString(null_variable_error))
        }
    }

    private fun emailingOps(email:String?,subject:String?,body:String?){
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("mailto:$email?subject=$subject&body=$body")))
    }

    private fun contactOps(name:String,code: Int){
        waitingView(getDrawable(ic_glossyphone))
        var isContactPresent = false
        val isEmailPresent: Boolean
        tempContactCode = code
        tempPhoneNumberIndex = 0
        tempEmailIndex = 0
        tempContact = name.trim()
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            speakOut(getString(require_physical_permission))
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS),skivvy.CODE_CONTACTS_REQUEST)
        } else {
            val cr: ContentResolver = contentResolver
            val cur: Cursor? = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
            if (cur?.count !! > 0) {
                while (cur.moveToNext()) {
                    //TODO: Additional Nickname support
                    val dName =
                        cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                            .toLowerCase(skivvy.locale)
                    val fName = dName.substringBefore(" ")
                    if (tempContact == dName || tempContact == fName) {
                        isContactPresent = true
                        contact.contactID =
                            cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID))
                        contact.displayName =
                            cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                        val dpUri =
                            cur.getString(cur.getColumnIndex(ContactsContract.Contacts.PHOTO_URI))
                        if (dpUri != null) {
                            contact.photoID = dpUri
                            waitingView(BitmapDrawable(resources, MediaStore.Images.Media.getBitmap(this.contentResolver, Uri.parse(contact.photoID))))
                        }
                        val pCur: Cursor? = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(contact.contactID),
                            null
                        )
                        val eCur: Cursor? = cr.query(
                            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                            arrayOf(contact.contactID),
                            null
                        )
                        var o = 0
                        while (eCur!!.moveToNext()) {
                            ++o
                        }
                        if(o>0){
                            isEmailPresent = true
                            eCur.moveToFirst()
                            contact.emailList = arrayOfNulls(o)
                            var k = 0
                            //TODO: Multiple email addresses handling
                            while (k < o) {
                                contact.emailList[k] = eCur.getString(eCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA))
                                ++k
                            }
                            if (tempContactCode == skivvy.CODE_CONTACT_EMAIL_CONF) {
                                if(contact.emailList.size>1){
                                    speakOut("I've got ${contact.emailList.size} email addresses of ${contact.displayName}.\nShould I email them at " +
                                            "${contact.emailList[tempEmailIndex!!]}?", skivvy.CODE_CONTACT_EMAIL_CONF)
                                } else {
                                    speakOut(
                                        getString(should_i_email) + "${contact.displayName} at ${contact.emailList[tempEmailIndex!!]}?",
                                        skivvy.CODE_CONTACT_EMAIL_CONF
                                    )
                                }
                            }
                            eCur.close()
                        } else {
                            isEmailPresent = false
                            if (tempContactCode == skivvy.CODE_CONTACT_EMAIL_CONF){
                                speakOut(getString(you_dont_seem_having)+contact.displayName+getString(R.string.someone_email_address))
                            }
                        }
                        if (tempContactCode == skivvy.CODE_CONTACT_CALL_CONF) {
                            if (cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)).toInt() > 0) {
                                pCur!!.moveToFirst()
                                var size = 0
                                while (pCur.moveToNext()) { ++size }
                                contact.phoneList = arrayOfNulls(size)
                                pCur.moveToFirst()
                                var k = 0
                                //TODO: Multiple phone numbers handling
                                while (pCur.moveToNext()) {
                                    contact.phoneList[k] = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                                    ++k
                                }
                                if(size==1){
                                    speakOut(
                                        getString(should_i_call) + "${contact.displayName}?",
                                        skivvy.CODE_CONTACT_CALL_CONF
                                    )
                                } else {
                                    speakOut("I've got $size phone numbers of ${contact.displayName}.\nShould I call them at " +
                                            "${contact.phoneList[tempPhoneNumberIndex!!]}?", skivvy.CODE_CONTACT_CALL_CONF)
                                }
                            } else {
                                if (isEmailPresent) {
                                    speakOut(
                                        getString(you_dont_seem_having)+ contact.displayName +getString(someones_phone_number)+"\n" +
                                                "Want to contact them at "+ "${contact.emailList[tempEmailIndex!!]}"+ " instead?", skivvy.CODE_CONTACT_EMAIL_CONF)
                                } else {
                                    errorView()
                                    speakOut(getString(you_dont_seem_having)+ contact.displayName +getString(someones_phone_number))
                                }
                            }
                        }
                        pCur!!.close()
                        break
                    } else isContactPresent = false
                }
            } else {
                errorView()
                speakOut(getString(no_contacts_available))
            }
            cur.close()
            if(!isContactPresent){
                errorView()
                speakOut(getString(contact_not_found))
            }
        }
    }

    //intent voice recognition, code according to action command, serving activity result
    private fun startVoiceRecIntent(code:Int){
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, skivvy.locale)
            //.putExtra(RecognizerIntent.EXTRA_PROMPT, "Reply");
        if (intent.resolveActivity(packageManager) != null)
            startActivityForResult(intent, code)
        else{
            errorView()
            speakOut(getString(internal_error))
        }
    }

    //gets all packages and respective details available on device
    private fun getLocalPackages(){
        var counter = 0
        val pm: PackageManager = packageManager
        val packages: List<ApplicationInfo> = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        packagesTotal = packages.size
        packagesAppName = arrayOfNulls(packagesTotal)
        packagesName = arrayOfNulls(packagesTotal)
        packagesIcon = arrayOfNulls(packagesTotal)
        packagesMain = arrayOfNulls(packagesTotal)
        for (packageInfo in packages) {
            if(pm.getLaunchIntentForPackage(packageInfo.packageName) != null) {
                packagesAppName[counter] =
                    pm.getApplicationLabel(packageInfo).toString().toLowerCase(skivvy.locale)
                packagesName[counter] = packageInfo.packageName.toLowerCase(skivvy.locale)
                packagesIcon[counter] = pm.getApplicationIcon(packageInfo)
                packagesMain[counter] = pm.getLaunchIntentForPackage(packageInfo.packageName)
                ++counter
            } else {
                --packagesTotal    //removing un-initiable packages
            }
        }
    }
    private fun takeScreenshot() {
        val now = Date()
        DateFormat.format("yyyy-MM-dd_hh:mm:ss", now)
        try {
            val mPath: String = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg"
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

     public override fun onDestroy() {
         loading?.startAnimation(exitAnimation)
         speakOut(getString(exit_msg))
         if (tts != null) {
             tts!!.stop()
             tts!!.shutdown()
         }
         super.onDestroy()
     }

    override fun onBackPressed() {
        speakOut(getString(exit_msg))
        loading?.startAnimation(exitAnimation)
        super.onBackPressed()
    }

    private fun normalView(){
        contact = ContactModel()
        txt = null
        tempMail = null
        tempMailSubject = null
        tempMailBody = null
        tempPackageIndex = null
        tempPhoneNumberIndex = 0
        tempEmailIndex = 0
        tempPhone = null
        tempContactCode = null
        tempContact = null
        loading?.setImageDrawable(getDrawable(ic_dotsincircle))
        loading?.startAnimation(normalRotate)
        input?.text = null
        outPut?.text = null
        icon?.setImageDrawable(null)
    }
    private fun waitingView(image:Drawable?){
        loading?.startAnimation(rotateSlow)
        loading?.setImageDrawable(getDrawable(ic_yellow_dotsincircle))
        if(image!=null){
            icon?.setImageDrawable(image)
        }
    }
    private fun errorView():Boolean{
        loading?.startAnimation(fadeAnimation)
        loading?.setImageDrawable(getDrawable(ic_red_dotsincircle))
        return false
    }
    private fun successView(image:Drawable?):Boolean{
        loading?.startAnimation(focusRotate)
        loading?.setImageDrawable(getDrawable(ic_green_dotsincircle))
        if(image!=null) {
            icon?.setImageDrawable(image)
        }
        return true
    }

    private fun setButtonsClickable(state:Boolean){
        receiver?.isClickable = state
        setting.isClickable = state
    }
     private fun speakOut(text:String) {
         outPut?.text = text
         tts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
             override fun onDone(utteranceId: String) {
//                 outputStat!!.visibility = View.INVISIBLE
             }
             override fun onError(utteranceId: String) {}
             override fun onStart(utteranceId: String) {
     //            outputStat!!.visibility = View.VISIBLE
             }
         })
         if(!getMuteStatus()) tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null,"")
     }

    private fun speakOut(text:String,taskCode:Int?){
        outPut?.text = text
        tts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String) {
          //      outputStat!!.visibility = View.INVISIBLE
                if(taskCode!=null){ startVoiceRecIntent(taskCode) }
                setButtonsClickable(true)
            }
            override fun onError(utteranceId: String) {}
            override fun onStart(utteranceId: String) {
                setButtonsClickable(false)
//                outputStat!!.visibility = View.VISIBLE
            }
        })
        if(!getMuteStatus()) tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null,"")
        else{
                if(taskCode!=null)  startVoiceRecIntent(taskCode)
        }
    }

    private fun getBiometricStatus():Boolean{
        return getSharedPreferences(skivvy.PREF_HEAD_SECURITY, MODE_PRIVATE)
            .getBoolean(skivvy.PREF_KEY_BIOMETRIC, false)
    }

    private fun getTrainingStatus():Boolean{
        return getSharedPreferences(skivvy.PREF_HEAD_APP_MODE, MODE_PRIVATE)
            .getBoolean(skivvy.PREF_KEY_TRAINING, false)
    }
    private fun getMuteStatus():Boolean{
        return getSharedPreferences(skivvy.PREF_HEAD_VOICE, MODE_PRIVATE)
            .getBoolean(skivvy.PREF_KEY_MUTE_UNMUTE, false)
    }
 }
