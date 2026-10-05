package com.fsck.k9.preferences

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettings
import net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettingsPreferenceManager
import net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettings
import net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingsPreferenceManager
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfig
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDrawerConfigManagerTest {
    private val inboxSettings = FakeInboxSettingsPreferenceManager(DisplayInboxSettings(isShowUnifiedInbox = true))
    private val visualSettings = FakeVisualSettingsPreferenceManager(DisplayVisualSettings())

    @Test
    fun `getConfigFlow() follows the Unified Inbox setting`() = runTest {
        val manager = DefaultDrawerConfigManager(backgroundScope, inboxSettings, visualSettings)
        val configs = collect(manager)

        assertThat(configs.last()).isEqualTo(config(showUnifiedFolders = true))

        inboxSettings.save(DisplayInboxSettings(isShowUnifiedInbox = false))
        runCurrent()

        assertThat(configs.last()).isEqualTo(config(showUnifiedFolders = false))
    }

    @Test
    fun `getConfigFlow() follows the other drawer settings`() = runTest {
        val manager = DefaultDrawerConfigManager(backgroundScope, inboxSettings, visualSettings)
        val configs = collect(manager)

        inboxSettings.save(DisplayInboxSettings(isShowUnifiedInbox = true, isShowStarredCount = true))
        runCurrent()
        assertThat(configs.last()).isEqualTo(config(showUnifiedFolders = true, showStarredCount = true))

        visualSettings.save(DisplayVisualSettings(drawerExpandAllFolder = true))
        runCurrent()
        assertThat(configs.last()).isEqualTo(
            config(showUnifiedFolders = true, showStarredCount = true, expandAllFolder = true),
        )
    }

    @Test
    fun `getConfig() reads the current settings without a subscriber`() = runTest {
        val manager = DefaultDrawerConfigManager(backgroundScope, inboxSettings, visualSettings)

        inboxSettings.save(DisplayInboxSettings(isShowUnifiedInbox = false))

        assertThat(manager.getConfig()).isEqualTo(config(showUnifiedFolders = false))
    }

    @Test
    fun `save() writes the settings it holds`() = runTest {
        val manager = DefaultDrawerConfigManager(backgroundScope, inboxSettings, visualSettings)

        manager.save(config(showUnifiedFolders = false, showStarredCount = true, expandAllFolder = true))

        assertThat(inboxSettings.getConfig())
            .isEqualTo(DisplayInboxSettings(isShowUnifiedInbox = false, isShowStarredCount = true))
        assertThat(visualSettings.getConfig()).isEqualTo(DisplayVisualSettings(drawerExpandAllFolder = true))
    }

    private fun TestScope.collect(manager: DefaultDrawerConfigManager): List<DrawerConfig> {
        val configs = mutableListOf<DrawerConfig>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.getConfigFlow().toList(configs)
        }
        runCurrent()
        return configs
    }

    private fun config(
        showUnifiedFolders: Boolean,
        showStarredCount: Boolean = false,
        expandAllFolder: Boolean = false,
    ) = DrawerConfig(
        showUnifiedFolders = showUnifiedFolders,
        showStarredCount = showStarredCount,
        expandAllFolder = expandAllFolder,
    )
}

private class FakeInboxSettingsPreferenceManager(
    initial: DisplayInboxSettings,
) : DisplayInboxSettingsPreferenceManager {
    private val state = MutableStateFlow(initial)
    override fun save(config: DisplayInboxSettings) = state.update { config }
    override fun getConfig(): DisplayInboxSettings = state.value
    override fun getConfigFlow(): Flow<DisplayInboxSettings> = state
}

private class FakeVisualSettingsPreferenceManager(
    initial: DisplayVisualSettings,
) : DisplayVisualSettingsPreferenceManager {
    private val state = MutableStateFlow(initial)
    override fun save(config: DisplayVisualSettings) = state.update { config }
    override fun getConfig(): DisplayVisualSettings = state.value
    override fun getConfigFlow(): Flow<DisplayVisualSettings> = state
}
