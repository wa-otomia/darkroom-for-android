package app.darkroom.android.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.darkroom.android.R
import app.darkroom.android.data.automation.Automation
import app.darkroom.android.data.catalog.CatalogRepository
import app.darkroom.android.data.ai.AiImageClient
import app.darkroom.android.data.jobs.AiJobs
import app.darkroom.android.data.printer.PrintQueue
import app.darkroom.android.data.printer.PrinterBluetooth
import app.darkroom.android.data.settings.ActivityLog
import app.darkroom.android.data.settings.SettingsRepository
import app.darkroom.android.data.transfer.TransferRegistry
import app.darkroom.android.ui.about.AboutScreen
import app.darkroom.android.ui.gallery.GalleryScreen
import app.darkroom.android.ui.io.IoScreen
import app.darkroom.android.ui.io.PrintQueueScreen
import app.darkroom.android.ui.presets.PresetsScreen
import app.darkroom.android.ui.settings.SettingsScreen
import app.darkroom.android.ui.studio.StudioScreen
import app.darkroom.android.ui.theme.Amber
import app.darkroom.android.ui.theme.Ink
import app.darkroom.android.ui.theme.Paper
import app.darkroom.android.ui.theme.PaperDim
import app.darkroom.android.ui.theme.Room
import kotlinx.coroutines.flow.MutableSharedFlow

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

    LaunchedEffect(openPhotoId) {
        if (!openPhotoId.isNullOrBlank()) {
            nav.navigate("studio/$openPhotoId") { launchSingleTop = true }
        }
    }

    Scaffold(
        containerColor = Room,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = Room, contentColor = Paper) {
                    data class Tab(val id: String, val label: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)
                    val items = listOf(
                        Tab("gallery", R.string.nav_gallery, Icons.Outlined.PhotoLibrary),
                        Tab("io", R.string.nav_io, Icons.Outlined.WifiTethering),
                        Tab("presets", R.string.nav_presets, Icons.Outlined.AutoFixHigh),
                        Tab("settings", R.string.nav_settings, Icons.Outlined.Settings),
                    )
                    items.forEach { tab ->
                        NavigationBarItem(
                            selected = destRoute == tab.id,
                            onClick = {
                                // Tapping the tab you are already on scrolls that tab back to the
                                // top instead of re-navigating to it.
                                if (destRoute == tab.id) {
                                    if (tab.id == "gallery") galleryReselect.tryEmit(Unit)
                                } else {
                                    nav.navigateTab(tab.id)
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Ink,
                                selectedTextColor = Amber,
                                indicatorColor = Paper,
                                unselectedIconColor = PaperDim,
                                unselectedTextColor = PaperDim,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            nav,
            startDestination = "gallery",
            modifier = Modifier.padding(padding),
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
                GalleryScreen(
                    photos = photos,
                    transfers = transfers,
                    aiJobs = aiJobList,
                    grokReady = grokPub.keySet,
                    printerReady = appSettings.printerMac.isNotBlank(),
                    autoEdit = appSettings.autoEdit,
                    autoWatermark = appSettings.autoWatermark,
                    autoPrint = appSettings.autoPrint,
                    catalog = catalog,
                    automation = automation,
                    reselect = galleryReselect,
                    onOpen = { nav.navigate("studio/$it") },
                    onDismissTransfer = { transferRegistry.dismiss(it) },
                    onCancelAi = { aiJobs.cancel(it) },
                    onSettings = { section ->
                        if (section.isBlank()) nav.navigateTab("settings")
                        else nav.navigateSettingsSection(section)
                    },
                )
            }
            composable("io") {
                IoScreen(
                    settings = appSettings,
                    sessions = sessions,
                    transfers = transfers,
                    printQueue = printQueue,
                    onDismissTransfer = { transferRegistry.dismiss(it) },
                    onOpenPrintQueue = { nav.navigate("io/print-queue") },
                )
            }
            composable("io/print-queue") {
                PrintQueueScreen(
                    catalog = catalog,
                    printQueue = printQueue,
                    photos = photos,
                    onBack = { nav.popBackStack() },
                )
            }
            composable("presets") { PresetsScreen(settings) }
            composable(
                route = "settings?section={section}",
                arguments = listOf(
                    navArgument("section") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
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
            composable("about") { AboutScreen(onBack = { nav.popBackStack() }) }
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
                    onSettings = { section ->
                        nav.navigate(if (section.isBlank()) "settings" else "settings?section=$section") {
                            launchSingleTop = true
                        }
                    },
                )
            }
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
