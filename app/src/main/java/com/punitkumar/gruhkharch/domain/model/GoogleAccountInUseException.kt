package com.punitkumar.gruhkharch.domain.model

/** Linking failed because this Google account already belongs to another user of the app. */
class GoogleAccountInUseException(val idToken: String) :
    Exception("This Google account is already used by another Construction Khata account.")
