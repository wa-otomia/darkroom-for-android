package io.github.wa_otomia.darkroom.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wa_otomia.darkroom.BuildConfig
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.ui.components.CardGroup
import io.github.wa_otomia.darkroom.ui.components.GroupDivider
import io.github.wa_otomia.darkroom.ui.components.ListRow
import io.github.wa_otomia.darkroom.ui.components.ListRowPadding
import io.github.wa_otomia.darkroom.ui.components.NoticeCard
import io.github.wa_otomia.darkroom.ui.components.SectionLabel
import io.github.wa_otomia.darkroom.ui.navigation.LocalBottomBarClearance
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.Paper
import io.github.wa_otomia.darkroom.ui.theme.PaperDim
import io.github.wa_otomia.darkroom.ui.theme.Room

private const val APACHE_2 = "Apache License 2.0"
private const val MLKIT_TERMS = "ML Kit Terms of Service"
private const val ANDROID_SDK_LICENSE = "Android Software Development Kit License"

private data class Component(val name: String, val license: String)

/**
 * Every shipped third-party component and its license. Mirrors `gradle/libs.versions.toml` and
 * `THIRD-PARTY-NOTICES.md`; update all three together when a dependency is added or removed, and
 * name each license as it is, not as Apache 2.0 (SYPM040-R03).
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
    Component("AndroidX Browser (Custom Tabs) — Google", APACHE_2),
    Component("Dagger and Hilt — Google", APACHE_2),
    Component("OkHttp — Square", APACHE_2),
    Component("Okio — Square", APACHE_2),
    Component("Coil — Coil Contributors", APACHE_2),
    Component("ML Kit Selfie Segmentation (MediaPipe runtime) — Google", MLKIT_TERMS),
    Component("Google Play services client libraries (base, basement, tasks) — Google", ANDROID_SDK_LICENSE),
    Component("ODML Image — Google", ANDROID_SDK_LICENSE),
    Component("Firebase Components and Encoders — Google", APACHE_2),
    Component("Android Data Transport — Google", APACHE_2),
    Component("AndroidX AppCompat and Fragment (via ML Kit) — Google", APACHE_2),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    // Detail route: this is just the navigation-bar inset, since the NavHost no longer pads the bottom.
    val clearance = LocalBottomBarClearance.current
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
                .padding(horizontal = 20.dp),
        ) {
            // Identity block: the app mark (SYUI020-R06) is decorative, so TalkBack starts at the name.
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo_mark),
                    contentDescription = null,
                    modifier = Modifier.size(92.dp),
                )
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Paper,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
                // A translated sentence around the numbers, so sans (monospace is for bare technical values).
                Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    color = Amber,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    stringResource(R.string.about_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PaperDim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            SectionLabel(stringResource(R.string.about_privacy_title))
            CardGroup {
                Paragraph(stringResource(R.string.about_privacy_ai))
                GroupDivider(startIndent = ListRowPadding)
                Paragraph(stringResource(R.string.about_privacy_local_cutout))
                GroupDivider(startIndent = ListRowPadding)
                Paragraph(stringResource(R.string.about_privacy_ftp))
                GroupDivider(startIndent = ListRowPadding)
                Paragraph(stringResource(R.string.about_privacy_storage))
                GroupDivider(startIndent = ListRowPadding)
                Paragraph(stringResource(R.string.about_privacy_local))
            }

            // Privacy-policy placeholder. The note stays Amber: it is the warning that the
            // policy link is still missing. No actions, so TalkBack reads it as one item.
            NoticeCard(
                title = stringResource(R.string.about_policy_title),
                text = stringResource(R.string.about_policy_placeholder),
                icon = Icons.Outlined.Policy,
                textColor = Amber,
                modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
            )

            SectionLabel(stringResource(R.string.about_licenses_title))
            Text(
                stringResource(R.string.about_licenses_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = PaperDim,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
            )
            // Nothing here is tappable: each row is one merged, read-only ListRow (name over license).
            CardGroup {
                COMPONENTS.forEachIndexed { index, component ->
                    if (index > 0) GroupDivider(startIndent = ListRowPadding)
                    ListRow(title = component.name, supporting = component.license)
                }
            }
            Spacer(Modifier.height(clearance + 32.dp))
        }
    }
}

/** One privacy paragraph as a text row of the privacy [CardGroup]. Paper on Surface1 14.70:1. */
@Composable
private fun Paragraph(text: String) {
    Text(
        text,
        color = Paper,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ListRowPadding, vertical = 14.dp),
    )
}
