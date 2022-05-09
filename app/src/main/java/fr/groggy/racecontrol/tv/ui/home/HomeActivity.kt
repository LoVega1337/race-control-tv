package fr.groggy.racecontrol.tv.ui.home

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import fr.groggy.racecontrol.tv.BuildConfig
import fr.groggy.racecontrol.tv.R
import fr.groggy.racecontrol.tv.core.season.SeasonService
import fr.groggy.racecontrol.tv.f1tv.Archive
import fr.groggy.racecontrol.tv.ui.season.browse.SeasonBrowseActivity
import fr.groggy.racecontrol.tv.ui.settings.SettingsActivity
import fr.groggy.racecontrol.tv.upd.DownloadApk
import fr.groggy.racecontrol.tv.utils.coroutines.schedule
import org.threeten.bp.Duration
import org.threeten.bp.Year
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection


@AndroidEntryPoint
class HomeActivity : FragmentActivity(R.layout.activity_home) {
    companion object {
        private val TAG = HomeActivity::class.simpleName

        fun intent(context: Context) = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }

    @Inject internal lateinit var seasonService: SeasonService
    private var teaserImage: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentYear = Year.now().value
        teaserImage = findViewById(R.id.teaserImage)
        teaserImage?.setOnClickListener {
            val activity = SeasonBrowseActivity.intent(this, Archive(currentYear))
            startActivity(activity)
        }

        val teaserImageText = findViewById<TextView>(R.id.teaserImageText)
        teaserImageText.text = resources.getString(R.string.teaser_image_text, currentYear)

        findViewById<View>(R.id.settings).setOnClickListener {
            startActivity(SettingsActivity.intent(this))
        }

        val currentVersion = BuildConfig.VERSION_NAME // Getting current version (eg. comparing with Version)
        // val currentVersion = "2.6.2" // Debug
        val currentVersionText: TextView = findViewById<TextView>(R.id.currentVersionText)
        currentVersionText.text = currentVersion // Showing Version number top left

        AsyncTask.execute {

            val httpsURL =
                "https://raw.githubusercontent.com/leonardoxh/race-control-tv/master/app/build.gradle.kts"
            val myUrl = URL(httpsURL)
            val conn: HttpsURLConnection = myUrl.openConnection() as HttpsURLConnection
            val `is`: InputStream = conn.getInputStream()
            val isr = InputStreamReader(`is`)
            val br = BufferedReader(isr)
            var inputLine: String?
            var completeResponse = ""

            while (br.readLine().also { inputLine = it } != null) {
                Log.d("inputResponse", inputLine.toString())
                completeResponse += inputLine
            }

            br.close()

            completeResponse =
                completeResponse.substring(completeResponse.indexOf("versionName = \"") + 15)
            completeResponse = completeResponse.substring(
                0,
                completeResponse.indexOf("\"        buildConfigField")
            )

            Log.d("CompleteResponse", completeResponse)

            if (completeResponse != currentVersion) {
                Log.d("@@@@@", "Different Version")
                runOnUiThread {
                    var newVersionText: TextView? = null
                    newVersionText = findViewById(R.id.newVersionText)
                    newVersionText.visibility = View.VISIBLE
                }
            }

        }

        findViewById<View>(R.id.updateApp).setOnClickListener {

            AsyncTask.execute {

                val httpsURL = "https://raw.githubusercontent.com/leonardoxh/race-control-tv/master/app/build.gradle.kts"
                val myUrl = URL(httpsURL)
                val conn: HttpsURLConnection = myUrl.openConnection() as HttpsURLConnection
                val `is`: InputStream = conn.getInputStream()
                val isr = InputStreamReader(`is`)
                val br = BufferedReader(isr)
                var inputLine: String?
                var completeResponse = ""

                while (br.readLine().also { inputLine = it } != null) {
                    Log.d("inputResponse", inputLine.toString())
                    completeResponse += inputLine
                }

                br.close()

                completeResponse = completeResponse.substring(completeResponse.indexOf("versionName = \"") + 15)
                completeResponse = completeResponse.substring(0, completeResponse.indexOf("\"        buildConfigField"))

                Log.d("CompleteResponse", completeResponse)

                if (completeResponse == currentVersion) {
                    Log.d("@@@@@", "Same Version")
                    runOnUiThread {
                        Toast.makeText(this, R.string.no_update_available, Toast.LENGTH_SHORT).show()
                    }
                }
                else {
                    Log.d("@@@@@", "Different Version")

                    runOnUiThread {
                        val url = "https://github.com/leonardoxh/race-control-tv/releases/latest/download/app-release.apk"
                        val downloadApk = DownloadApk(this@HomeActivity)
                        downloadApk.startDownloadingApk(url)
                    }
                }
            }
        }
    }

    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()

        teaserImage?.requestFocus()

        lifecycleScope.launchWhenStarted {
            schedule(Duration.ofMinutes(1)) {
                Log.d("Fetching new data", "Lifecycle state is ${lifecycle.currentState}")
                try {
                    seasonService.loadSeason(Archive(Year.now().value))
                } catch (_: Exception) {
                    /*
                     * If for whatever reason this doesn't load, just give up
                     * user can retry at next screen
                     */
                }

                if (supportFragmentManager.findFragmentByTag(TAG) !is HomeFragment) {
                    supportFragmentManager.commit {
                        replace(R.id.fragment_container, HomeFragment())
                    }
                }
            }
        }
    }
}