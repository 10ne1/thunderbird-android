package com.fsck.k9.preferences

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettings
import net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettingsPreferenceManager
import net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettings
import net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingsPreferenceManager
import net.thunderbird.core.preference.update
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfig

internal class DefaultDrawerConfigManager(
    coroutineScope: CoroutineScope,
    private val displayInboxSettingsPreferenceManager: DisplayInboxSettingsPreferenceManager,
    private val displayVisualSettingsPreferenceManager: DisplayVisualSettingsPreferenceManager,
) : DrawerConfigManager {
    // Follow the settings rather than read them once: the drawer kept showing the Unified Inbox after it was
    // turned off, until the app restarted.
    private val drawerConfig: StateFlow<DrawerConfig> = combine(
        displayInboxSettingsPreferenceManager.getConfigFlow(),
        displayVisualSettingsPreferenceManager.getConfigFlow(),
    ) { inboxSettings, visualSettings ->
        toDrawerConfig(inboxSettings, visualSettings)
    }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = DrawerConfig(
                showStarredCount = false,
                showUnifiedFolders = false,
                expandAllFolder = false,
            ),
        )

    override fun save(config: DrawerConfig) {
        displayInboxSettingsPreferenceManager.update {
            it.copy(
                isShowStarredCount = config.showStarredCount,
                isShowUnifiedInbox = config.showUnifiedFolders,
            )
        }
        displayVisualSettingsPreferenceManager.update {
            it.copy(drawerExpandAllFolder = config.expandAllFolder)
        }
    }

    @Synchronized
    override fun getConfig(): DrawerConfig {
        return toDrawerConfig(
            displayInboxSettingsPreferenceManager.getConfig(),
            displayVisualSettingsPreferenceManager.getConfig(),
        )
    }

    override fun getConfigFlow(): Flow<DrawerConfig> {
        return drawerConfig
    }

    private fun toDrawerConfig(inboxSettings: DisplayInboxSettings, visualSettings: DisplayVisualSettings) =
        DrawerConfig(
            showStarredCount = inboxSettings.isShowStarredCount,
            showUnifiedFolders = inboxSettings.isShowUnifiedInbox,
            expandAllFolder = visualSettings.drawerExpandAllFolder,
        )
}
