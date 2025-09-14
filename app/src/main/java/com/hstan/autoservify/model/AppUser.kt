package com.hstan.autoservify.model

data class AppUser(
    var uid: String = "",
    var email: String = "",
    var name: String = "",
    var userType: String = "",
    var shopId: String? = null
) {
    // Empty constructor required for Firestore
    constructor() : this("", "", "", "", null)
}
