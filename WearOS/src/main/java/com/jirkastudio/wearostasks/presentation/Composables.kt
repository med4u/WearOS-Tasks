package com.jirkastudio.wearostasks.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.material.Checkbox
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip

@Composable
fun CustomToggleChip(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    labelText: String,
    modifier: Modifier = Modifier
) {
    ToggleChip(
        modifier = modifier,
        checked = checked,
        onCheckedChange = onCheckedChange,
        toggleControl = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.semantics {
                    this.contentDescription = if (checked) "Done" else "Not done"
                },
            )
        },
        label = {
            Text(text = labelText)
        },
    )
}