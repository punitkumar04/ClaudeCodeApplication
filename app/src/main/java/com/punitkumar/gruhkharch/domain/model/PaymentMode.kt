package com.punitkumar.gruhkharch.domain.model

enum class PaymentMode(val displayName: String) {
    UPI("UPI (GPay/PhonePe/PayTM)"),
    BANK_TRANSFER("Bank Transfer (NEFT/IMPS)"),
    CASH("Cash"),
    CHEQUE("Cheque"),
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    OTHER("Other")
}
