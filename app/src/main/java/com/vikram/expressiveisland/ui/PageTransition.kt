package com.vikram.expressiveisland.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import com.vikram.expressiveisland.data.PageTransitionStyle

/**
 * Builds the configured page transition. Every style uses the same duration budget to keep
 * navigation responsive while offering distinct motion personalities.
 */
internal fun pageTransition(
    style: PageTransitionStyle,
    direction: Int = 1,
): ContentTransform {
    val enter: EnterTransition
    val exit: ExitTransition

    when (style) {
        PageTransitionStyle.FADE -> {
            enter = fadeIn(tween(PAGE_TRANSITION_DURATION_MS))
            exit = fadeOut(tween(PAGE_TRANSITION_DURATION_MS))
        }

        PageTransitionStyle.SLIDE -> {
            enter = slideInHorizontally(tween(PAGE_TRANSITION_DURATION_MS)) { width ->
                direction * width
            }
            exit = slideOutHorizontally(tween(PAGE_TRANSITION_DURATION_MS)) { width ->
                -direction * width
            }
        }

        PageTransitionStyle.SHARED_AXIS -> {
            enter = slideInHorizontally(tween(PAGE_TRANSITION_DURATION_MS)) { width ->
                direction * width / 4
            } + fadeIn(tween(PAGE_TRANSITION_DURATION_MS))
            exit = slideOutHorizontally(tween(PAGE_TRANSITION_DURATION_MS)) { width ->
                -direction * width / 4
            } + fadeOut(tween(PAGE_TRANSITION_DURATION_MS))
        }

        PageTransitionStyle.FADE_THROUGH -> {
            enter = fadeIn(tween(PAGE_TRANSITION_DURATION_MS, delayMillis = 90))
            exit = fadeOut(tween(90))
        }

        PageTransitionStyle.SCALE_FADE -> {
            enter = scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(PAGE_TRANSITION_DURATION_MS)
            ) + fadeIn(tween(PAGE_TRANSITION_DURATION_MS))
            exit = scaleOut(
                targetScale = 0.96f,
                animationSpec = tween(PAGE_TRANSITION_DURATION_MS)
            ) + fadeOut(tween(PAGE_TRANSITION_DURATION_MS))
        }

        PageTransitionStyle.SLIDE_FADE -> {
            enter = slideInHorizontally(tween(PAGE_TRANSITION_DURATION_MS)) { width ->
                direction * width
            } + fadeIn(tween(PAGE_TRANSITION_DURATION_MS))
            exit = slideOutHorizontally(tween(PAGE_TRANSITION_DURATION_MS)) { width ->
                -direction * width
            } + fadeOut(tween(PAGE_TRANSITION_DURATION_MS))
        }
    }

    return enter togetherWith exit
}

private const val PAGE_TRANSITION_DURATION_MS = 300
