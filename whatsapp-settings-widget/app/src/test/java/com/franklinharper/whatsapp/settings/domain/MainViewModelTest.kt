package com.franklinharper.whatsapp.settings.domain

import com.franklinharper.whatsapp.settings.MainUiState
import com.franklinharper.whatsapp.settings.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class MainViewModelTest {

    private class FakeRepository(private var status: WhatsAppStatus) : WhatsAppStatusRepository {
        override fun getStatus(): WhatsAppStatus = status
        fun setStatus(s: WhatsAppStatus) { status = s }
    }

    @Test
    fun `initial state reflects Unrestricted from repository`() {
        val repo = FakeRepository(WhatsAppStatus.Unrestricted)
        val vm = MainViewModel(repo)
        assertEquals(MainUiState(WhatsAppStatus.Unrestricted), vm.uiState.value)
    }

    @Test
    fun `initial state reflects Optimized from repository`() {
        val repo = FakeRepository(WhatsAppStatus.Optimized)
        val vm = MainViewModel(repo)
        assertEquals(MainUiState(WhatsAppStatus.Optimized), vm.uiState.value)
    }

    @Test
    fun `initial state reflects NotInstalled from repository`() {
        val repo = FakeRepository(WhatsAppStatus.NotInstalled)
        val vm = MainViewModel(repo)
        assertEquals(MainUiState(WhatsAppStatus.NotInstalled), vm.uiState.value)
    }

    @Test
    fun `refresh re-queries repository and updates uiState`() {
        val repo = FakeRepository(WhatsAppStatus.Optimized)
        val vm = MainViewModel(repo)
        repo.setStatus(WhatsAppStatus.Unrestricted)
        vm.refresh()
        assertEquals(MainUiState(WhatsAppStatus.Unrestricted), vm.uiState.value)
    }
}
