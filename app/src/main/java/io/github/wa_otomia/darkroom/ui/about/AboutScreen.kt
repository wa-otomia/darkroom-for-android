package io.github.wa_otomia.darkroom.ui.about

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wa_otomia.darkroom.BuildConfig
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.ui.components.SectionLabel
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.MonoFont
import io.github.wa_otomia.darkroom.ui.theme.Paper
import io.github.wa_otomia.darkroom.ui.theme.PaperDim
import io.github.wa_otomia.darkroom.ui.theme.PaperHairline
import io.github.wa_otomia.darkroom.ui.theme.Room

private const val APACHE_2 = "Apache License 2.0"

private data class Component(val name: String, val license: String)

/**
 * Every shipped third-party component and its license. Mirrors `gradle/libs.versions.toml`;
 * update both together when a dependency is added or removed.
 */
private val COMPONENTS = listOf(
    Component("Kotlin Standard Library — JetBrains", APACHE_2),
    Component("kotlinx.coroutines — JetBrains", APACHE_2),
    Component("kotlinx.serialization — JetBrains", APACHE_2),
    Component("Jetpack Compose (ui, foundation, animation) — Google", APACHE_2),
    Component("Material Components for Compose (material3, material-icons) — Google", APACHE_2),
    Component("AndroidX Core, Activity, Lifecycle — Google", APACHE_2),
    Component("AndroidX Navigation — Google", APACHE_2),
    Component("AndroidX Room — Google", APACHE_2),
    Component("AndroidX DataStore — Google", APACHE_2),
    Component("AndroidX Security Crypto — Google", APACHE_2),
    Component("AndroidX ExifInterface — Google", APACHE_2),
    Component("Dagger and Hilt — Google", APACHE_2),
    Component("OkHttp — Square", APACHE_2),
    Component("Okio — Square", APACHE_2),
    Component("Coil — Coil Contributors", APACHE_2),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = Room,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title), color = Paper) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = Paper,
                        )
                    }
                },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Room,
                    titleContentColor = Paper,
                    navigationIconContentColor = Paper,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
            Text(
                stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                color = Amber,
                fontFamily = MonoFont,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                stringResource(R.string.about_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = PaperDim,
                modifier = Modifier.padding(top = 10.dp, bottom = 24.dp),
            )

            SectionLabel(stringResource(R.string.about_privacy_title))
            Paragraph(stringResource(R.string.about_privacy_ai))
            Paragraph(stringResource(R.string.about_privacy_ftp))
            Paragraph(stringResource(R.string.about_privacy_storage))
            Paragraph(stringResource(R.string.about_privacy_local))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 24.dp)
                    .border(1.dp, Amber.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Outlined.Policy, contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp))
                Column {
                    Text(stringResource(R.string.about_policy_title), color = Paper)
                    Text(
                        stringResource(R.string.about_policy_placeholder),
                        color = Amber,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            SectionLabel(stringResource(R.string.about_licenses_title))
            Text(
                stringResource(R.string.about_licenses_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = PaperDim,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            // Nothing on this page is tappable, so every frame here is decoration and uses the
            // hairline. At PaperFaint's 40% the five stacked panels read as a stack of controls.
            Column(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, PaperHairline, RoundedCornerShape(2.dp)),
            ) {
                COMPONENTS.forEachIndexed { index, component ->
                    if (index > 0) HorizontalDivider(thickness = 1.dp, color = PaperHairline)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(component.name, color = Paper, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
                        Text(
                            component.license,
                            color = PaperDim,
                            fontFamily = MonoFont,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(
        text,
        color = Paper,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .border(1.dp, PaperHairline, RoundedCornerShape(2.dp))
            .padding(12.dp),
    )
}
