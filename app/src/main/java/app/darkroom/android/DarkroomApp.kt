package app.darkroom.android

import android.app.Application
import app.darkroom.android.data.jobs.JobsServiceController
import app.darkroom.android.data.printer.PrintQueue
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DarkroomApp : Application() {
    @Inject lateinit var jobsController: JobsServiceController
    @Inject lateinit var printQueue: PrintQueue

    override fun onCreate() {
        super.onCreate()
        jobsController.start()
        printQueue.restore()
    }
}
