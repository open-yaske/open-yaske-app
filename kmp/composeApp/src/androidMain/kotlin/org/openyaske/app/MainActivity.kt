package org.openyaske.app

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import org.openyaske.app.notify.ClassReminder
import org.openyaske.app.media.AndroidPdfBridge
import org.openyaske.core.api.CoreApi
import org.openyaske.core.data.AndroidDatabaseDriverFactory
import org.openyaske.core.data.DatabaseRepository
import org.openyaske.core.data.openDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // アイコンだけの起動画面を出し、最初のフレームで Compose に渡す。
        installSplashScreen()
        // targetSdk 36 では edge-to-edge が既定。ステータスバーとナビバーの下まで描く。
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        org.openyaske.app.ui.design.AndroidHaptics.view = window.decorView
        val api = CoreApi(
            DatabaseRepository(
                openDatabase(AndroidDatabaseDriverFactory(applicationContext))
            )
        )
        val picker = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            AndroidPdfBridge.onResult(uris)
        }
        val camera = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            AndroidPdfBridge.onCaptured(bitmap)
        }
        AndroidPdfBridge.attach(this, picker, camera)

        // 時間割の背景画像。Photo Picker で 1 枚選び、縮小して filesDir に保存する。
        org.openyaske.app.ui.timetable.attachTimetableBackground(this)
        var pendingBackground: kotlinx.coroutines.CancellableContinuation<Uri?>? = null
        val backgroundPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            pendingBackground?.resume(uri)
            pendingBackground = null
        }
        org.openyaske.app.ui.timetable.AndroidTimetableBackground.pick = {
            suspendCancellableCoroutine { continuation ->
                pendingBackground = continuation
                backgroundPicker.launch("image/*")
            }
        }

        // ローカル通知の準備。許可は Android 13 以降だけ求める。
        val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
        ClassReminder.ensureScheduled(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ClassReminder.canNotify(this)
        ) {
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            App(api)
        }
    }
}
