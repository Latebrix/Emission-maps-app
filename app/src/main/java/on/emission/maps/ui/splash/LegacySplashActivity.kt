package on.emission.maps.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import java.util.Timer
import kotlin.concurrent.schedule
import on.emission.maps.ui.main.MainActivity
import on.emission.maps.R
import com.github.megatronking.stringfog.annotation.StringFogIgnore

@StringFogIgnore
class LegacySplashActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen()
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_legacy_splash)
    
    splashScreen.setKeepOnScreenCondition { true }

    // For this example, the Timer delay represents awaiting the data to determine where to navigate
    
    Timer().schedule(1500){
      routeToNextActivity()
    }
  }

  private fun routeToNextActivity() {
    val intent = Intent(this@LegacySplashActivity, MainActivity::class.java)
    startActivity(intent)
    finish()
  }
}