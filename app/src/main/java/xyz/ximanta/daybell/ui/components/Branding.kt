package xyz.ximanta.daybell.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.ximanta.daybell.R

/**
 * The Daybell mark.
 * @param tint set to MaterialTheme.colorScheme.primary to make it follow the theme,
 *             or Color.Unspecified to keep the brand teal.
 */
@Composable
fun DaybellLogo(
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    contentDescription: String? = stringResource(R.string.app_name)
) {
    Image(
        painter = painterResource(id = R.drawable.ic_daybell_logo),
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = if (tint == Color.Unspecified) null else ColorFilter.tint(tint)
    )
}

/** Logo + wordmark + tagline, for the drawer header / onboarding / About screen. */
@Composable
fun DaybellLockup(
    modifier: Modifier = Modifier,
    showTagline: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DaybellLogo(
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (showTagline) {
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
