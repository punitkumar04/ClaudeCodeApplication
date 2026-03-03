package com.punitkumar.gruhkharch.domain.model

enum class PaymentMode(val displayName: String) {
    GPAY("GPay"),
    PHONEPE("PhonePe"),
    BANK_TRANSFER("Bank Transfer (NEFT/IMPS)"),
    CASH("Cash"),
    CHEQUE("Cheque"),
    CREDIT_CARD("Credit Card"),
    UPI_OTHER("UPI (Other)"),
    OTHER("Other")
}
