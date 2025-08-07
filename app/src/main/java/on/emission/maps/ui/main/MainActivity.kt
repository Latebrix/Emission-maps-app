package on.emission.maps.ui.main

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.pandora.bottomnavigator.BottomNavigator
import com.pandora.bottomnavigator.FragmentInfo
import on.emission.maps.R
import on.emission.maps.databinding.ActivityMainBinding
import on.emission.maps.ui.home.MainFragment
import on.emission.maps.ui.more.MoreFragment
import on.emission.maps.ui.resources.ResourcesFragment
import on.emission.maps.util.system.getThemeColor
import on.emission.maps.util.theme.StorageThemeManager
import java.util.Timer
import kotlin.concurrent.schedule


class MainActivity : AppCompatActivity() {

    lateinit var navigator: BottomNavigator
    private lateinit var binding: ActivityMainBinding
    var contentHasLoaded = false

    fun setTheme(myTheme: Int, apply: Boolean) {
        theme.applyStyle(myTheme, apply)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        setTheme(StorageThemeManager().getMyTheme(this), true)

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        myNavigator(false)

        //splash screen
        startLoadingContent()
        setupSplashScreen(splashScreen)

        window.statusBarColor = getThemeColor(R.attr.LinearBackGroundColor)
        window.navigationBarColor = getThemeColor(R.attr.NavBackGroundColor)
        WindowCompat.setDecorFitsSystemWindows(window, false)

    }

    override fun onBackPressed() {
        if (!navigator.pop()) {
            super.onBackPressed()
        }
    }

    private fun startLoadingContent() {
        Timer().schedule(2000) {
            contentHasLoaded = true
        }
    }

    private fun setupSplashScreen(splashScreen: SplashScreen) {
        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    return if (contentHasLoaded) {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else false
                }
            }
        )
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val slideBack = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.TRANSLATION_X,
                0f,
                -splashScreenView.view.width.toFloat()
            ).apply {
                interpolator = DecelerateInterpolator()
                duration = 800L
                doOnEnd { splashScreenView.remove() }
            }

            slideBack.start()
        }
    }

    fun myNavigator(restart: Boolean) {

        if (restart) {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
        }

        //navigator
        navigator = BottomNavigator.onCreateWithDetachability(
            fragmentContainer = R.id.container,
            bottomNavigationView = findViewById(R.id.navView),
            rootFragmentsFactory = mapOf(
                R.id.home to { FragmentInfo(MainFragment(), true) },
                R.id.resources to { FragmentInfo(ResourcesFragment(), true) },
                R.id.more to { FragmentInfo(MoreFragment(), true) }
            ),
            defaultTab = R.id.home,
            activity = this
        )

        if (restart) {
            navigator.pop()
        }

        window.navigationBarColor = getThemeColor(R.attr.NavBackGroundColor)
        window.statusBarColor = getThemeColor(R.attr.NavBackGroundColor)

    }

}
