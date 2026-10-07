package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.DocumentRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MteMergeApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var documentRepository: DocumentRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Initialize PDFBox for Android
        PDFBoxResourceLoader.init(this)

        database = AppDatabase.getDatabase(this)
        documentRepository = DocumentRepository(database.documentHistoryDao())
    }

    companion object {
        lateinit var instance: MteMergeApp
            private set
    }
}
