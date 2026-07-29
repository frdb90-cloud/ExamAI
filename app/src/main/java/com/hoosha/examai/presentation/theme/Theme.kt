package com.hoosha.examai.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
 primary = Color(0xFF166534),
 onPrimary = Color.White,
 primaryContainer = Color(0xFFDCFCE7),
 onPrimaryContainer = Color(0xFF052E16),
 secondary = Color(0xFF2563EB),
 onSecondary = Color.White,
 secondaryContainer = Color(0xFFDBEAFE),
 onSecondaryContainer = Color(0xFF172554),
 tertiary = Color(0xFF7C3AED),
 background = Color(0xFFF8FAFC),
 onBackground = Color(0xFF0F172A),
 surface = Color(0xFFFFFFFF),
 onSurface = Color(0xFF0F172A),
 surfaceVariant = Color(0xFFE2E8F0),
 onSurfaceVariant = Color(0xFF475569),
 error = Color(0xFFB91C1C),
 errorContainer = Color(0xFFFEE2E2)
)

private val DarkColors = darkColorScheme(
 primary = Color(0xFF86EFAC),
 onPrimary = Color(0xFF052E16),
 primaryContainer = Color(0xFF14532D),
 onPrimaryContainer = Color(0xFFDCFCE7),
 secondary = Color(0xFF93C5FD),
 onSecondary = Color(0xFF172554),
 secondaryContainer = Color(0xFF1E3A8A),
 onSecondaryContainer = Color(0xFFDBEAFE),
 tertiary = Color(0xFFC4B5FD),
 background = Color(0xFF020617),
 onBackground = Color(0xFFF8FAFC),
 surface = Color(0xFF0F172A),
 onSurface = Color(0xFFF8FAFC),
 surfaceVariant = Color(0xFF1E293B),
 onSurfaceVariant = Color(0xFFCBD5E1),
 error = Color(0xFFFCA5A5),
 errorContainer = Color(0xFF7F1D1D)
)

private val ExamAiTypography = Typography(
 displayLarge = TextStyle(
 fontFamily = FontFamily.Default,
 fontWeight = FontWeight.Bold,
 fontSize = 48.sp
 ),
 headlineLarge = TextStyle(
 fontFamily = FontFamily.Default,
 fontWeight = FontWeight.Bold,
 fontSize = 30.sp
 ),
 titleLarge = TextStyle(
 fontFamily = FontFamily.Default,
 fontWeight = FontWeight.SemiBold,
 fontSize = 22.sp
 ),
 titleMedium = TextStyle(
 fontFamily = FontFamily.Default,
 fontWeight = FontWeight.SemiBold,
 fontSize = 18.sp
 ),
 bodyLarge = TextStyle(
 fontFamily = FontFamily.Default,
 fontWeight = FontWeight.Normal,
 fontSize = 17.sp,
 lineHeight = 28.sp
 ),
 bodyMedium = TextStyle(
 fontFamily = FontFamily.Default,
 fontWeight = FontWeight.Normal,
 fontSize = 15.sp,
 lineHeight = 24.sp
 ),
 bodySmall = TextStyle(
 fontFamily = FontFamily.Default,
 fontWeight = FontWeight.Normal,
 fontSize = 13.sp,
 lineHeight = 20.sp
 )
)

@Composable
fun ExamAiTheme(
 darkTheme: Boolean = isSystemInDarkTheme(),
 dynamicColor: Boolean = false,
 content: @Composable () -> Unit
) {
 val context = LocalContext.current

 val colors = when {
 dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
 dynamicDarkColorScheme(context)
 dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
 dynamicLightColorScheme(context)
 darkTheme -> DarkColors
 else -> LightColors
 }

 val activity = context as? Activity
 if (activity!= null) {
 WindowCompat.getInsetsController(
 activity.window,
 activity.window.decorView
 ).isAppearanceLightStatusBars =!darkTheme
 }

 CompositionLocalProvider(
 LocalLayoutDirection provides LayoutDirection.Rtl
 ) {
 MaterialTheme(
 colorScheme = colors,
 typography = ExamAiTypography,
 content = content
 )
 }
}