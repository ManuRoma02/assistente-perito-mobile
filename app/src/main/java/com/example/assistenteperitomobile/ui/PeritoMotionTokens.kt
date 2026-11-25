package com.example.assistenteperitomobile.ui

import android.animation.ValueAnimator
import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntSize

object PeritoMotionTokens {
    val snappySpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val sheetSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow
    )

    val contentSizeSpring: SpringSpec<IntSize> = spring(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow
    )

    val gentleFade: TweenSpec<Float> = tween(durationMillis = 220)

    @Suppress("UNUSED_PARAMETER")
    fun areAnimationsEnabled(context: Context): Boolean =
        runCatching { ValueAnimator.areAnimatorsEnabled() }.getOrDefault(true)
}
