@file:Suppress("NOTHING_TO_INLINE")

package on.emission.maps.util.view

import android.view.ViewGroup
import androidx.core.view.children

inline fun <reified T> ViewGroup.findChild(): T? {
    return children.find { it is T } as? T
}
