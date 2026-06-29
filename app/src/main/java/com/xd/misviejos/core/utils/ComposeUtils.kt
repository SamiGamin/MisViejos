package com.xd.misviejos.core.utils

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modificador transparente para dar un efecto de escala elástica (100% a 95%) al presionar.
 * No consume los eventos de click, por lo que es compatible con clickable { ... } y botones convencionales.
 */
fun Modifier.bounceClick() = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounceClick"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val type = event.type
                    if (type == PointerEventType.Press) {
                        isPressed = true
                    } else if (type == PointerEventType.Release || type == PointerEventType.Exit) {
                        isPressed = false
                    }
                }
            }
        }
}

/**
 * Modificador para animar la entrada escalonada (cascade entrance / staggered)
 * de los elementos de una lista. Desliza verticalmente hacia arriba con resorte y aplica fade-in.
 */
@Composable
fun Modifier.fadeInEntrance(index: Int): Modifier {
    val animatableAlpha = remember { Animatable(0f) }
    val animatableOffset = remember { Animatable(40f) }

    LaunchedEffect(Unit) {
        // Retraso proporcional para crear la cascada
        delay(index * 60L)
        launch {
            animatableAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            animatableOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    return this
        .graphicsLayer {
            alpha = animatableAlpha.value
            translationY = animatableOffset.value
        }
}
