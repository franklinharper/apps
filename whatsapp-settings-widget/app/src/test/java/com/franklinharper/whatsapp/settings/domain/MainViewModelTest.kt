package com.franklinharper.whatsapp.settings.domain

import com.franklinharper.whatsapp.settings.MainUiState
import com.franklinharper.whatsapp.settings.MainViewModel
import com.franklinharper.whatsapp.settings.monitor.WhatsAppStatusMonitor
import com.franklinharper.whatsapp.settings.presentation.UnrestrictedSessionFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class MainViewModelTest {

    private class FakeStatusRepository(private var status: WhatsAppStatus) : WhatsAppStatusRepository {
        override fun getStatus(): WhatsAppStatus = status
        fun setStatus(s: WhatsAppStatus) { status = s }
    }

    private class FakeTrackingRepository : StatusTrackingRepository {
        val recordedStatuses = mutableListOf<WhatsAppStatus>()
        var sessions = emptyList<UnrestrictedSession>()

        override fun recordIfChanged(
            status: WhatsAppStatus,
            timestampMillis: Long,
            source: DetectionSource,
        ) {
            recordedStatuses += status
        }

        override fun getUnrestrictedSessionsNewestFirst(): List<UnrestrictedSession> = sessions
    }

    @Test
    fun `initial state reflects BackgroundUsageUnrestricted from repository`() {
        val fixture = fixture(WhatsAppStatus.BackgroundUsageUnrestricted)
        val vm = fixture.viewModel
        assertEquals(MainUiState(WhatsAppStatus.BackgroundUsageUnrestricted), vm.uiState.value)
    }

    @Test
    fun `initial state reflects BackgroundUsageRestrictedOrOptimized from repository`() {
        val fixture = fixture(WhatsAppStatus.BackgroundUsageRestrictedOrOptimized)
        val vm = fixture.viewModel
        assertEquals(MainUiState(WhatsAppStatus.BackgroundUsageRestrictedOrOptimized), vm.uiState.value)
    }

    @Test
    fun `initial state reflects NotInstalled from repository`() {
        val fixture = fixture(WhatsAppStatus.NotInstalled)
        val vm = fixture.viewModel
        assertEquals(MainUiState(WhatsAppStatus.NotInstalled), vm.uiState.value)
    }

    @Test
    fun `refresh re-queries repository and updates uiState`() {
        val fixture = fixture(WhatsAppStatus.BackgroundUsageRestrictedOrOptimized)
        fixture.statusRepository.setStatus(WhatsAppStatus.BackgroundUsageUnrestricted)
        fixture.viewModel.refresh()
        assertEquals(MainUiState(WhatsAppStatus.BackgroundUsageUnrestricted), fixture.viewModel.uiState.value)
    }

    @Test
    fun `initial state includes formatted unrestricted sessions`() {
        val fixture = fixture(WhatsAppStatus.BackgroundUsageRestrictedOrOptimized)
        fixture.trackingRepository.sessions = listOf(
            UnrestrictedSession(
                id = 42,
                startTimestampMillis = 1_000,
                endTimestampMillis = 61_000,
            )
        )
        fixture.viewModel.refresh()

        assertEquals(1, fixture.viewModel.uiState.value.unrestrictedSessions.size)
        assertEquals(42, fixture.viewModel.uiState.value.unrestrictedSessions.first().id)
    }

    private fun fixture(initialStatus: WhatsAppStatus): Fixture {
        val statusRepository = FakeStatusRepository(initialStatus)
        val trackingRepository = FakeTrackingRepository()
        val monitor = WhatsAppStatusMonitor(
            statusRepository = statusRepository,
            trackingRepository = trackingRepository,
            clock = { 123L },
        )
        return Fixture(
            statusRepository = statusRepository,
            trackingRepository = trackingRepository,
            viewModel = MainViewModel(
                monitor = monitor,
                trackingRepository = trackingRepository,
                sessionFormatter = UnrestrictedSessionFormatter(),
            ),
        )
    }

    private data class Fixture(
        val statusRepository: FakeStatusRepository,
        val trackingRepository: FakeTrackingRepository,
        val viewModel: MainViewModel,
    )
}
