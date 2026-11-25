package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class FinanceColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val savings: Color,
    val savingsContainer: Color,
    val warning: Color
)

val LocalFinanceColors = staticCompositionLocalOf {
    FinanceColors(
        income = LightIncomeGreen,
        incomeContainer = LightIncomeContainer,
        expense = LightExpenseRose,
        expenseContainer = LightExpenseContainer,
        savings = LightSavingsGold,
        savingsContainer = LightSavingsContainer,
        warning = LightWarningAmber
    )
}

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = OnEmeraldPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = OnEmeraldPrimaryContainer,
    secondary = SageSecondary,
    onSecondary = OnSageSecondary,
    secondaryContainer = SageSecondaryContainer,
    onSecondaryContainer = OnSageSecondaryContainer,
    tertiary = GoldTertiary,
    onTertiary = OnGoldTertiary,
    tertiaryContainer = GoldTertiaryContainer,
    onTertiaryContainer = OnGoldTertiaryContainer,
    background = WarmAlabasterBg,
    onBackground = OnWarmInk,
    surface = CrispPaperSurface,
    onSurface = OnWarmInk,
    surfaceVariant = SoftStoneSurfaceVariant,
    onSurfaceVariant = MutedSlateOnSurfaceVariant,
    surfaceContainerLow = CrispPaperSurface,
    surfaceContainer = Color(0xFFF4F1EA),
    surfaceContainerHigh = SoftStoneSurfaceVariant,
    outline = SubtleBorderOutline,
    outlineVariant = Color(0xFFE7E3DA),
    error = RoseError,
    onError = OnRoseError,
    errorContainer = RoseErrorContainer,
    onErrorContainer = OnRoseErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkMintPrimary,
    onPrimary = DarkOnMintPrimary,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = DarkOnEmeraldContainer,
    secondary = DarkSageSecondary,
    onSecondary = DarkOnSageSecondary,
    secondaryContainer = DarkSageContainer,
    onSecondaryContainer = DarkOnSageContainer,
    tertiary = DarkAmberTertiary,
    onTertiary = DarkOnAmberTertiary,
    tertiaryContainer = DarkAmberContainer,
    onTertiaryContainer = DarkOnAmberContainer,
    background = PineNightBg,
    onBackground = PineNightText,
    surface = PineNightSurface,
    onSurface = PineNightText,
    surfaceVariant = PineNightSurfaceVariant,
    onSurfaceVariant = PineNightMutedText,
    surfaceContainerLow = PineNightSurface,
    surfaceContainer = Color(0xFF192420),
    surfaceContainerHigh = PineNightSurfaceVariant,
    outline = PineNightOutline,
    outlineVariant = Color(0xFF25332E),
    error = DarkRoseError,
    onError = DarkOnRoseError,
    errorContainer = DarkRoseErrorContainer,
    onErrorContainer = DarkOnRoseErrorContainer
)

private val KasKuShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KasKuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val financeColors = if (darkTheme) {
        FinanceColors(
            income = DarkIncomeGreen,
            incomeContainer = DarkIncomeContainer,
            expense = DarkExpenseRose,
            expenseContainer = DarkExpenseContainer,
            savings = DarkSavingsGold,
            savingsContainer = DarkSavingsContainer,
            warning = DarkWarningAmber
        )
    } else {
        FinanceColors(
            income = LightIncomeGreen,
            incomeContainer = LightIncomeContainer,
            expense = LightExpenseRose,
            expenseContainer = LightExpenseContainer,
            savings = LightSavingsGold,
            savingsContainer = LightSavingsContainer,
            warning = LightWarningAmber
        )
    }

    CompositionLocalProvider(LocalFinanceColors provides financeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KasKuTypography,
            shapes = KasKuShapes,
            content = content
        )
    }
}
