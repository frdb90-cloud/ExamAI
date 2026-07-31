package com.hoosha.examai

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class ExamAiApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}