package com.example.tracker.ui

import com.example.tracker.R
import com.example.tracker.databinding.ViewGenderBinding
import com.example.tracker.domain.Gender

/** Reads and sets the shared gender picker (view_gender.xml). Null = nothing chosen. */
fun genderOf(picker: ViewGenderBinding): Gender? = when (picker.genderGroup.checkedButtonId) {
    R.id.maleButton -> Gender.MALE
    R.id.femaleButton -> Gender.FEMALE
    else -> null
}

fun setGender(picker: ViewGenderBinding, gender: Gender?) {
    when (gender) {
        Gender.MALE -> picker.genderGroup.check(R.id.maleButton)
        Gender.FEMALE -> picker.genderGroup.check(R.id.femaleButton)
        null -> picker.genderGroup.clearChecked()
    }
}
