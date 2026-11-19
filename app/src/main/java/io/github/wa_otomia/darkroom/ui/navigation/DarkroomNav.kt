package io.github.wa_otomia.darkroom.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.data.automation.Automation
import io.github.wa_otomia.darkroom.data.catalog.CatalogRepository
import io.github.wa_otomia.darkroom.data.ai.AiImageClient
import io.github.wa_otomia.darkroom.data.jobs.AiJobs
import io.github.wa_otomia.darkroom.data.printer.PrintQueue
import io.github.wa_otomia.darkroom.data.printer.PrinterBluetooth
import io.github.wa_otomia.darkroom.data.settings.ActivityLog
import io.github.wa_otomia.darkroom.data.settings.SettingsRepository
import io.github.wa_otomia.darkroom.data.transfer.TransferRegistry
import io.github.wa_otomia.darkroom.ui.about.AboutScreen
import io.github.wa_otomia.darkroom.ui.gallery.GalleryScreen
import io.github.wa_otomia.darkroom.ui.io.IoScreen
import io.github.wa_otomia.darkroom.ui.io.PrintQueueScreen
import io.github.wa_otomia.darkroom.ui.presets.PresetsScreen
import io.github.wa_otomia.darkroom.ui.settings.SettingsScreen
import io.github.wa_otomia.darkroom.ui.studio.StudioScreen
import io.github.wa_otomia.darkroom.ui.components.pressScale
import io.github.wa_otomia.darkroom.ui.theme.DarkroomMotion
import io.github.wa_otomia.darkroom.ui.theme.Ink
import io.github.wa_otomia.darkroom.ui.theme.Paper
import io.github.wa_otomia.darkroom.ui.theme.PaperDim
import io.github.wa_otomia.darkroom.ui.theme.PaperPressed
import io.github.wa_otomia.darkroom.ui.theme.Room
import io.github.wa_otomia.darkroom.ui.theme.Surface2
import io.github.wa_otomia.darkroom.ui.theme.Surface3
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Space a screen must leave free at its bottom edge so its last item can be
 * scrolled fully clear of the floating navigation bar (SYUI020-R05).
 *
 * On a top-level tab it is the bar's height + 12dp gap + the navigation-bar
 * inset; on a detail screen (no bar) it is just the navigation-bar inset.
 * Add it to the bottom `contentPadding` of the scrolling container (or as a
 * trailing `Spacer`), and lift FABs/snackbars by it. Do not also add
 * `navigationBarsPadding()` — the inset is already included. Outside
 * [DarkroomNav] it is 0.dp.
 */
val LocalBottomBarClearance = compositionLocalOf<Dp> { 0.dp }

@Composable
fun DarkroomNav(
    catalog: CatalogRepository,
    settings: SettingsRepository,
    printQueue: PrintQueue,
    grok: AiImageClient,
    bluetooth: PrinterBluetooth,
    activityLog: ActivityLog,
    transferRegistry: TransferRegistry,
    aiJobs: AiJobs,
    automation: Automation,
    openPhotoId: String? = null,
) {
    val nav = rememberNavController()
    val photos by catalog.photos.collectAsState(initial = emptyList())
    val appSettings by settings.settings.collectAsState()
    val grokPub by settings.ai.collectAsState()
    val transfers by transferRegistry.transfers.collectAsState()
    val sessions by transferRegistry.sessions.collectAsState()
    val aiJobList by aiJobs.jobs.collectAsState()
    val back by nav.currentBackStackEntryAsState()
    val destRoute = back?.destination?.route.orEmpty().substringBefore("?")
    val tabs = listOf("gallery", "io", "presets", "settings")
    val showBar = destRoute in tabs
    // Kept across recompositions: GalleryScreen keys its collector on this instance.
    val galleryReselect = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    val galleryFocus = remember { MutableStateFlow<String?>(null) }
    val focusPhotoId by galleryFocus.collectAsState()
    val layoutDirection = LocalLayoutDirection.current

    fun openGallery(photoId: String) {
        galleryFocus.value = photoId
        if (!nav.popBackStack("gallery", false)) {
            nav.navigateTab("gallery")
        }
    }

    LaunchedEffect(openPhotoId) {
        if (!openPhotoId.isNullOrBlank()) {
            nav.navigate("studio/$openPhotoId") { launchSingleTop = true }
        }
    }

    val tabItems = remember {
        listOf(
            NavTab("gallery", R.string.nav_gallery, Icons.Outlined.PhotoLibrary),
            NavTab("io", R.string.nav_io, Icons.Outlined.WifiTethering),
            NavTab("presets", R.string.nav_presets, Icons.Outlined.AutoFixHigh),
            NavTab("settings", R.string.nav_settings, Icons.Outlined.Settings),
        )
    }
    val density = LocalDensity.current
    var barHeight by remember { mutableStateOf(FloatingBarMinHeight) }
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val clearance = if (showBar) barHeight + FloatingBarGap + navInset else navInset

    // No bottomBar: the floating bar is drawn over the content, which runs to
    // the bottom edge on every route. Top-level screens keep their last item
    // clear of the bar through LocalBottomBarClearance.
    Scaffold(containerColor = Room) { padding ->
        // Every route but the Studio starts below the status bar. The Studio takes the top inset
        // itself, so its black crop backdrop runs up under the status bar (design D4) and moves
        // with the screen during the slide and fade transitions.
        val top = padding.calculateTopPadding()
        Box(Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalBottomBarClearance provides clearance) {
                NavHost(
                    nav,
                    startDestination = "gallery",
                    modifier = Modifier.padding(
                        start = padding.calculateStartPadding(layoutDirection),
                        end = padding.calculateEndPadding(layoutDirection),
                    ),
                    // Tab switches cross-fade; drilling into a detail slides toward the layout end and
                    // reverses on the way back. SlideDirection.Start/End already respect RTL.
                    enterTransition = {
                        if (targetState.isDetail()) {
                            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(DETAIL_MS)) +
                                fadeIn(tween(DETAIL_MS))
                        } else {
                            fadeIn(tween(TAB_MS))
                        }
                    },
                    exitTransition = {
                        if (targetState.isDetail()) {
                            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(DETAIL_MS)) +
                                fadeOut(tween(DETAIL_MS))
                        } else {
                            fadeOut(tween(TAB_MS))
                        }
                    },
                    popEnterTransition = {
                        if (initialState.isDetail()) {
                            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(DETAIL_MS)) +
                                fadeIn(tween(DETAIL_MS))
                        } else {
                            fadeIn(tween(TAB_MS))
                        }
                    },
                    popExitTransition = {
                        if (initialState.isDetail()) {
                            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(DETAIL_MS)) +
                                fadeOut(tween(DETAIL_MS))
                        } else {
                            fadeOut(tween(TAB_MS))
                        }
                    },
                ) {
                    composable("gallery") {
                        BelowStatusBar(top) {
                            GalleryScreen(
                                photos = photos,
                                transfers = transfers,
                                aiJobs = aiJobList,
                                grokCredential = grokPub.credential,
                                printerReady = appSettings.printerMac.isNotBlank(),
                                autoEdit = appSettings.autoEdit,
                                autoWatermark = appSettings.autoWatermark,
                                autoPrint = appSettings.autoPrint,
                                catalog = catalog,
                                automation = automation,
                                printQueue = printQueue,
                                reselect = galleryReselect,
                                focusPhotoId = focusPhotoId,
                                onFocusConsumed = { galleryFocus.value = null },
                                onOpen = { nav.navigate("studio/$it") },
                                onDismissTransfer = { transferRegistry.dismiss(it) },
                                onCancelAi = { aiJobs.cancel(it) },
                                onSettings = { section ->
                                    if (section.isBlank()) nav.navigateTab("settings")
                                    else nav.navigateSettingsSection(section)
                                },
                            )
                        }
                    }
                    composable("io") {
                        BelowStatusBar(top) {
                            IoScreen(
                                settings = appSettings,
                                sessions = sessions,
                                transfers = transfers,
                                printQueue = printQueue,
                                onDismissTransfer = { transferRegistry.dismiss(it) },
                                onOpenPrintQueue = { nav.navigate("io/print-queue") },
                            )
                        }
                    }
                    composable("io/print-queue") {
                        BelowStatusBar(top) {
                            PrintQueueScreen(
                                catalog = catalog,
                                printQueue = printQueue,
                                photos = photos,
                                onBack = { nav.popBackStack() },
                            )
                        }
                    }
                    composable("presets") { BelowStatusBar(top) { PresetsScreen(settings) } }
                    composable(
                        route = "settings?section={section}",
                        arguments = listOf(
                            navArgument("section") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                        ),
                    ) { entry ->
                        BelowStatusBar(top) {
                            SettingsScreen(
                                settings = settings,
                                bluetooth = bluetooth,
                                activityLog = activityLog,
                                grokClient = grok,
                                catalog = catalog,
                                section = entry.arguments?.getString("section").orEmpty(),
                                onAbout = { nav.navigate("about") },
                            )
                        }
                    }
                    composable("about") { BelowStatusBar(top) { AboutScreen(onBack = { nav.popBackStack() }) } }
                    // No BelowStatusBar: StudioScreen pads for the status bar itself.
                    composable("studio/{id}") { entry ->
                        val id = entry.arguments?.getString("id").orEmpty()
                        StudioScreen(
                            photoId = id,
                            photos = photos,
                            catalog = catalog,
                            settings = settings,
                            printQueue = printQueue,
                            aiJobs = aiJobs,
                            onBack = { nav.popBackStack() },
                            // No launchSingleTop: sibling versions are a different photo, so the back stack
                            // has to keep the one we came from.
                            onOpen = { nav.navigate("studio/$it") },
                            onOpenGallery = { openGallery(it) },
                            onSettings = { section ->
                                nav.navigate(if (section.isBlank()) "settings" else "settings?section=$section") {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
            }
            if (showBar) {
                FloatingNavBar(
                    tabs = tabItems,
                    selectedId = destRoute,
                    onSelect = { tab ->
                        // Tapping the tab you are already on scrolls that tab back to the
                        // top instead of re-navigating to it.
                        if (destRoute == tab.id) {
                            if (tab.id == "gallery") galleryReselect.tryEmit(Unit)
                        } else {
                            nav.navigateTab(tab.id)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = FloatingBarGap)
                        .onSizeChanged {
                            val measured = with(density) { it.height.toDp() }
                            if (measured != barHeight) barHeight = measured
                        },
                )
            }
        }
    }
}

private data class NavTab(val id: String, val label: Int, val icon: ImageVector)

/**
 * The top system-bar padding the navigation host used to apply to every route. Applied per
 * destination so that the Studio route alone can draw under the status bar; it wraps the screen
 * exactly as the old host padding did (same constraints, same size).
 */
@Composable
private fun BelowStatusBar(top: Dp, content: @Composable () -> Unit) {
    Box(Modifier.padding(top = top)) { content() }
}

/** Gap between the floating bar and the navigation-bar inset. */
private val FloatingBarGap = 12.dp

/** 4dp inset + 48dp item + 4dp inset; the measured height wins once laid out. */
private val FloatingBarMinHeight = 56.dp
private val FloatingBarInset = 4.dp
private val FloatingBarShape = RoundedCornerShape(28.dp)
private val FloatingItemShape = RoundedCornerShape(24.dp)

/** Surface2 at 92%; no backdrop blur on any API level (SYUI020-R05). */
private val FloatingBarFill = Surface2.copy(alpha = 0.92f)

/**
 * Floating pill navigation (design D3). The selected tab is a Paper pill with
 * icon and label in Ink (14.85:1); the others are 48dp icon-only cells in
 * PaperDim (5.00:1 on the bar over Room, still 4.36:1 if a white photo shows
 * through the 8% transparency — icons need 3:1). Each cell is a `Role.Tab`
 * with selected state inside a `selectableGroup`, and its content
 * description is the tab label. Compose gives tabs no "not selected" state
 * text, so unselected cells set it themselves and TalkBack reads "Settings,
 * not selected, tab"; the selected cell gets TalkBack's own "Selected". The
 * selected cell's width change animates over
 * [DarkroomMotion.NavBarResizeMillis]. Row order follows layout direction,
 * so the bar mirrors in RTL.
 *
 * The selected cell is weighted (without fill), so the icon-only cells are
 * measured first and always keep their 48dp; at large font sizes on narrow
 * screens the label gives way and ends in an ellipsis instead. The whole pill
 * takes touches (as the old Material bar did), so a tap on its rim or between
 * cells never reaches the content scrolled underneath.
 */
@Composable
private fun FloatingNavBar(
    tabs: List<NavTab>,
    selectedId: String,
    onSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .shadow(elevation = 8.dp, shape = FloatingBarShape, clip = false)
            .clip(FloatingBarShape)
            .background(FloatingBarFill)
            // Swallow taps that miss a cell; the content runs under the bar.
            .pointerInput(Unit) {}
            .selectableGroup()
            .padding(FloatingBarInset),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val selected = selectedId == tab.id
            FloatingNavItem(
                tab = tab,
                selected = selected,
                onClick = { onSelect(tab) },
                modifier = if (selected) Modifier.weight(1f, fill = false) else Modifier,
            )
        }
    }
}

@Composable
private fun FloatingNavItem(
    tab: NavTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(tab.label)
    val notSelected = stringResource(R.string.a11y_state_not_selected)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Every press changes the fill, so it shows with animations off too:
    // PaperPressed keeps Ink at 11.92:1, Surface3 keeps the PaperDim icon at 4.65:1.
    val fill by animateColorAsState(
        targetValue = when {
            selected && pressed -> PaperPressed
            selected -> Paper
            pressed -> Surface3
            else -> Surface3.copy(alpha = 0f)
        },
        animationSpec = DarkroomMotion.stateChange(),
        label = "navItemFill",
    )
    val content by animateColorAsState(
        targetValue = if (selected) Ink else PaperDim,
        animationSpec = DarkroomMotion.stateChange(),
        label = "navItemContent",
    )
    Row(
        modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                if (!selected) stateDescription = notSelected
            }
            .pressScale(interaction)
            .clip(FloatingItemShape)
            .background(fill)
            .animateContentSize(tween(DarkroomMotion.NavBarResizeMillis, easing = DarkroomMotion.Standard))
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(tab.icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                color = content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // The cell already carries the full label as its content description.
                modifier = Modifier.weight(1f, fill = false).clearAndSetSemantics {},
            )
        }
    }
}

private const val TAB_MS = 200
private const val DETAIL_MS = 300

private val TOP_LEVEL = setOf("gallery", "io", "presets", "settings")

/**
 * Settings is both a tab and a drill-down target, so it counts as a detail only when a status chip
 * asked for a specific section.
 */
private fun NavBackStackEntry.isDetail(): Boolean {
    val route = destination.route.orEmpty().substringBefore("?")
    val base = route.substringBefore("/")
    if (base !in TOP_LEVEL) return true
    if (route != base) return true
    if (base == "settings") return arguments?.getString("section").orEmpty().isNotBlank()
    return false
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateSettingsSection(section: String) {
    navigate("settings?section=$section") {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
    }
}
