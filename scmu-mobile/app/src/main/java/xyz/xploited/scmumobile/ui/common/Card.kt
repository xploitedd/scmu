package xyz.xploited.scmumobile.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Card(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontSize = 20.sp
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 12.sp
            )
        }
    }
}