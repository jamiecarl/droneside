package com.jamiecarl.droneside.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.jamiecarl.droneside.R

@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: Int) = Font(
    resId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val Inter = FontFamily(
    variable(R.font.inter_variable, 400),
    variable(R.font.inter_variable, 500),
    variable(R.font.inter_variable, 600),
    variable(R.font.inter_variable, 700)
)

/** Timing, labels and stats. */
val MonoDs = FontFamily(
    variable(R.font.jetbrains_mono_variable, 400),
    variable(R.font.jetbrains_mono_variable, 500),
    variable(R.font.jetbrains_mono_variable, 600)
)
