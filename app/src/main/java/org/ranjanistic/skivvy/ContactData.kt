package org.ranjanistic.skivvy

class ContactData {
    private var totalContacts:Int = 0
    private lateinit var contactID:Array<String?>
    private lateinit  var photoID:Array<String?>
    private lateinit  var displayName:Array<String?>
    private lateinit var  nickName:Array<Array<String?>?>
    private lateinit  var phoneList:Array<Array<String?>?>
    private lateinit  var emailList:Array<Array<String?>?>

    fun setTotalContacts(size:Int){
        this.totalContacts = size
    }
    fun getTotalContacts():Int{
        return this.totalContacts
    }
    fun setContactDataInitials(IDs:Array<String?>,pIDs:Array<String?>,displayNames:Array<String?>,
                                   nicknames:Array<Array<String?>?>,phones:Array<Array<String?>?>,emails:Array<Array<String?>?>){
        this.contactID = IDs
        this.photoID = pIDs
        this.displayName = displayNames
        this.nickName = nicknames
        this.phoneList = phones
        this.emailList = emails
    }
    fun setContactSoloData(index:Int, ID:String?,photoUri:String?,contactName:String?){
        this.contactID[index] = ID
        this.photoID[index] = photoUri
        this.displayName[index] = contactName
    }
    fun setContactNicknameInitials(contactIndex: Int,nicknames:Array<String?>){
        this.nickName[contactIndex] =  nicknames
    }
    fun setContactPhonesInitials(contactIndex: Int,phones:Array<String?>){
        this.phoneList[contactIndex] = phones
    }
    fun setContactEmailsInitials(contactIndex: Int,emails:Array<String?>){
        this.emailList[contactIndex] = emails
    }
    fun setContactNicknameData(contactIndex:Int,nicknameIndex:Int,nickName:String?){
        this.nickName[contactIndex]?.set(nicknameIndex, nickName)
    }
    fun setContactPhoneData(contactIndex:Int,phoneIndex:Int,phone:String?){
        this.phoneList[contactIndex]?.set(phoneIndex, phone)
    }
    fun setContactEmailData(contactIndex:Int,emailIndex:Int,email:String?){
        this.emailList[contactIndex]?.set(emailIndex, email)
    }

    fun getContactIDs():Array<String?>{
        return this.contactID
    }
    fun getContactPhotoUris():Array<String?>{
        return this.photoID
    }
    fun getContactNames():Array<String?>{
        return this.displayName
    }
    fun getContactNicknames():Array<Array<String?>?>{
        return this.nickName
    }
    fun getContactEmails():Array<Array<String?>?>{
        return this.emailList
    }
    fun getContactPhones():Array<Array<String?>?>{
        return this.phoneList
    }
}