package com.example.tracker

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withId

/** Types on the PIN sheet's own keypad, as a parent would (the sheet is a dialog window). */
object PinPad {

    private val digits = listOf(R.id.key0, R.id.key1, R.id.key2, R.id.key3, R.id.key4, R.id.key5, R.id.key6, R.id.key7, R.id.key8, R.id.key9)

    fun type(pin: String, ok: Boolean = true) {
        pin.forEach { press(digits[it - '0']) }
        if (ok) press(R.id.keyOk)
    }

    fun press(id: Int) {
        onView(withId(id)).inRoot(isDialog()).perform(click())
    }
}
