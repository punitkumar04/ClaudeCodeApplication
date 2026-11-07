package com.punitkumar.gruhkharch.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

object CurrencyFormatter {
    // Indian number formatting: 1,00,000 instead of 100,000
    fun formatIndianCurrency(amount: Double): String {
        if (!amount.isFinite()) return "₹0"
        val isNegative = amount < 0
        // Round first so a fraction like .996 can't round up to "1" and be appended as a digit.
        val absAmount = BigDecimal.valueOf(Math.abs(amount)).setScale(2, RoundingMode.HALF_UP).toDouble()

        if (absAmount < 1000) {
            val df = DecimalFormat("#,##0.##")
            val formatted = df.format(absAmount)
            return if (isNegative) "-₹$formatted" else "₹$formatted"
        }

        val intPart = absAmount.toLong()
        val decimalPart = absAmount - intPart

        val lastThree = (intPart % 1000).toString()
        val remaining = intPart / 1000

        val formattedRemaining = if (remaining > 0) {
            val df = DecimalFormat("#,##,##0")
            // Custom Indian grouping
            formatIndianGrouping(remaining) + ","
        } else ""

        val paddedLastThree = if (remaining > 0) lastThree.padStart(3, '0') else lastThree

        val decimalStr = if (decimalPart > 0.005) {
            val df = DecimalFormat(".##")
            df.format(decimalPart)
        } else ""

        val result = "$formattedRemaining$paddedLastThree$decimalStr"
        return if (isNegative) "-₹$result" else "₹$result"
    }

    private fun formatIndianGrouping(number: Long): String {
        if (number < 100) return number.toString()
        val str = number.toString()
        val result = StringBuilder()
        var count = 0
        for (i in str.length - 1 downTo 0) {
            if (count > 0 && count % 2 == 0) {
                result.insert(0, ',')
            }
            result.insert(0, str[i])
            count++
        }
        return result.toString()
    }

    /** Amount without grouping or exponent notation, e.g. 10000000.0 -> "10000000", 12.5 -> "12.5". */
    fun formatPlainAmount(amount: Double): String =
        if (amount.isFinite()) BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString() else "0"

    fun formatCompact(amount: Double): String {
        return when {
            amount >= 10000000 -> String.format("₹%.1f Cr", amount / 10000000)
            amount >= 100000 -> String.format("₹%.1f L", amount / 100000)
            amount >= 1000 -> String.format("₹%.1f K", amount / 1000)
            else -> formatIndianCurrency(amount)
        }
    }
}
