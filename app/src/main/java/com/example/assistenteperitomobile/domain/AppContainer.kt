package com.example.assistenteperitomobile.domain

import android.content.Context
import com.example.assistenteperitomobile.data.ArchiveRepository
import com.example.assistenteperitomobile.data.CalendarRepository
import com.example.assistenteperitomobile.data.ChatHistoryRepository
import com.example.assistenteperitomobile.data.DocumentProcessor
import com.example.assistenteperitomobile.data.FascicoloRepository
import com.example.assistenteperitomobile.data.ModelDownloadManager
import com.example.assistenteperitomobile.data.ota.OtaUpdateManager
import com.example.assistenteperitomobile.domain.agents.AgentPipelineOrchestrator

class AppContainer(val appContext: Context) {
    val aiEngine: PeritoAiEngine by lazy { PeritoAiEngine(appContext) }
    val agentOrchestrator: AgentPipelineOrchestrator by lazy { AgentPipelineOrchestrator(aiEngine) }
    val archiveRepo: ArchiveRepository by lazy { ArchiveRepository(appContext, aiEngine) }
    val calendarRepo: CalendarRepository by lazy { CalendarRepository(appContext) }
    val docProcessor: DocumentProcessor by lazy { DocumentProcessor(appContext) }
    val modelDownloadManager: ModelDownloadManager by lazy { ModelDownloadManager(appContext) }
    val chatHistoryRepo: ChatHistoryRepository by lazy { ChatHistoryRepository(appContext) }
    val fascicoloRepo: FascicoloRepository by lazy { FascicoloRepository(appContext) }
    val otaUpdateManager: OtaUpdateManager by lazy { OtaUpdateManager(appContext) }

    fun close() {
        runCatching { docProcessor.close() }
    }
}
