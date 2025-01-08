package on.emission.maps.util.theme

import android.app.Activity
import android.content.SharedPreferences
import android.content.Context
import on.emission.maps.R

public class StorageThemeManager {
    
    private lateinit var getTheme:SharedPreferences
    private lateinit var setTheme:SharedPreferences.Editor
    
    public fun getMyTheme(context: Context) : Int {
        getTheme = context.getSharedPreferences("myTheme" , Activity.MODE_PRIVATE)
        var myTheme: Int = getTheme.getInt("MainTheme", R.style.ThemeTuketion)
        return myTheme
    }
    
    public fun setMyTheme(context: Context , newTheme:Int) {
        setTheme = context.getSharedPreferences("myTheme" , Activity.MODE_PRIVATE).edit()
        setTheme.putInt("MainTheme", newTheme).commit()
    }
    
}
