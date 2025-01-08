package on.emission.maps

import android.app.Application
import com.google.android.material.color.DynamicColors

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // apply dynamic color
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}