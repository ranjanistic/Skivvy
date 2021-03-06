package org.ranjanistic.skivvy.manager

class TempDataManager {

    private var packageIndex: Int = 0
    private var phoneIndex: Int = 0
    private var phone: String? = null
    private var emailIndex: Int = 0
    private var email: String? = null
    private var emailSubject: String? = null
    private var emailBody: String? = null
    private var textBody: String? = null
    private var contactPresence: Boolean = false
    private var contactReceived: String? = null
    private var contactIndex: Int = 0
    private var contactCode: Int? = null
    private var volumePercent: Float = 40F
    private var authAttemptCount: Int = 4

    //TODO: Retry command by adding to previous one
    private var retryCommandCount = 0
    private var lastCommand: String? = null
    private var inConversation: Boolean = false
    private var inConversationCode: Int? = null

    fun getPackageIndex(): Int = this.packageIndex
    fun getPhoneIndex(): Int = this.phoneIndex
    fun getPhone(): String? {
        return this.phone
    }

    fun getEmailIndex(): Int = this.emailIndex
    fun getEmail(): String? = this.email
    fun getEmailSubject(): String? = this.emailSubject
    fun getEmailBody(): String? = this.emailBody
    fun getTextBody(): String? = this.textBody
    fun getContactPresence(): Boolean = this.contactPresence
    fun getContactReceived(): String? = this.contactReceived
    fun getContactIndex(): Int = this.contactIndex
    fun getContactCode(): Int? = this.contactCode
    fun getVolumePercent(): Float = this.volumePercent
    fun getAuthAttemptCount(): Int = this.authAttemptCount
    fun getRetryCommandCount(): Int = this.retryCommandCount
    fun getLastCommand(): String? = this.lastCommand
    fun isInConversation(): Boolean = this.inConversation
    fun inConversationCode(): Int? = this.inConversationCode
    fun setPackageIndex(packageIndex: Int) {
        this.packageIndex = packageIndex
    }

    fun setPhoneIndex(phoneIndex: Int) {
        this.phoneIndex = phoneIndex
    }

    fun setPhone(phone: String?) {
        this.phone = phone
    }

    fun setEmailIndex(emailIndex: Int) {
        this.emailIndex = emailIndex
    }

    fun setEmail(email: String?) {
        this.email = email
    }

    fun setEmailSubject(emailSubject: String?) {
        this.emailSubject = emailSubject
    }

    fun setEmailBody(emailBody: String?) {
        this.emailBody = emailBody
    }

    fun setTextBody(textBody: String?) {
        this.textBody = textBody
    }

    fun setContactPresence(isPresent: Boolean) {
        this.contactPresence = isPresent
    }

    fun setContactReceived(contactReceived: String?) {
        this.contactReceived = contactReceived
    }

    fun setContactIndex(contactIndex: Int) {
        this.contactIndex = contactIndex
    }

    fun setContactCode(contactCode: Int?) {
        this.contactCode = contactCode
    }

    fun setVolumePercent(volumePercent: Float) {
        this.volumePercent = volumePercent
    }

    fun setAuthAttemptCount(count: Int) {
        this.authAttemptCount = count
    }

    fun setRetryCommandCount(count: Int) {
        this.retryCommandCount = count
    }

    fun setLastCommand(cmd: String?) {
        this.lastCommand = cmd
    }

    fun setInConversation(status: Boolean) {
        this.inConversation = status
    }

    fun setInConversationCode(code: Int) {
        this.inConversationCode = code
    }
}
