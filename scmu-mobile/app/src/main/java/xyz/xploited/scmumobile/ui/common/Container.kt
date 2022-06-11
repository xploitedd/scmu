package xyz.xploited.scmumobile.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Container(
    modifier: Modifier = Modifier,
    composable: @Composable () -> Unit
) {
    Box(
        modifier = modifier.padding(16.dp)
    ) {
        composable()
    }
}