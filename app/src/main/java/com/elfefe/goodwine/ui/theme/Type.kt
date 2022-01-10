package com.elfefe.goodwine.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.unit.sp
import com.elfefe.goodwine.R



val nunito = FontFamily(
    Font(R.font.nuitosans_regular, FontWeight.Normal),
    Font(R.font.nuitosans_light, FontWeight.Light),
    Font(R.font.nuitosans_extralight, FontWeight.ExtraLight),
    Font(R.font.nuitosans_bold, FontWeight.Bold)
)

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = nunito,
        fontWeight = FontWeight.Light,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)