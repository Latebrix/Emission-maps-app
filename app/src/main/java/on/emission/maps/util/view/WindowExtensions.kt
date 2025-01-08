package on.emission.maps.util.view

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.Window
import com.google.android.material.elevation.ElevationOverlayProvider
import on.emission.maps.util.system.InternalResourceHelper
import on.emission.maps.util.system.getResourceColor

//not used function
fun Window.setNavigationBarTransparentCompat(context: Context, elevation: Float = 0F) {
    navigationBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        !InternalResourceHelper.getBoolean(context, "config_navBarNeedsScrim", true)
    ) {
        Color.TRANSPARENT
    } else {
        // Set navbar scrim 70% of navigationBarColor
        ElevationOverlayProvider(context).compositeOverlayIfNeeded(
            context.getResourceColor(android.R.attr.navigationBarColor, 0.7F),
            elevation,
        )
    }
}