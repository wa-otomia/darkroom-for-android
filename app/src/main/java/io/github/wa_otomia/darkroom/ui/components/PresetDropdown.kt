package io.github.wa_otomia.darkroom.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.core.AI_PRESET_NONE
import io.github.wa_otomia.darkroom.core.AiPreset
import io.github.wa_otomia.darkroom.ui.theme.Paper
import io.github.wa_otomia.darkroom.ui.theme.PaperDim

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetDropdown(
    value: String,
    presets: List<AiPreset>,
    enabled: Boolean = true,
    label: String? = null,
    onChange: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val selected = presets.find { it.id == value }?.title ?: stringResource(R.string.preset_none)
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { if (enabled) open = it }) {
        TextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = label?.let { { Text(it, color = PaperDim) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
            colors = darkroomTextFieldColors(),
        )
        val menuMaxHeight = with(LocalDensity.current) {
            (LocalWindowInfo.current.containerSize.height / 2).toDp()
        }
        ExposedDropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.heightIn(max = menuMaxHeight),
        ) {
            DropdownMenuItem(text = { Text(stringResource(R.string.preset_none), color = Paper) }, onClick = {
                onChange(AI_PRESET_NONE)
                open = false
            })
            presets.groupBy { it.category }.forEach { (_, items) ->
                items.forEach { p ->
                    DropdownMenuItem(text = { Text(p.title, color = Paper) }, onClick = {
                        onChange(p.id)
                        open = false
                    })
                }
            }
        }
    }
}
