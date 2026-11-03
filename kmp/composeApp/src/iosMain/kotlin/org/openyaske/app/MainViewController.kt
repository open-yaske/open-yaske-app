package org.openyaske.app

import androidx.compose.ui.window.ComposeUIViewController
import org.openyaske.core.api.CoreApi
import org.openyaske.core.data.DatabaseRepository
import org.openyaske.core.data.IosDatabaseDriverFactory
import org.openyaske.core.data.openDatabase
import platform.UIKit.UIViewController

/**
 * iOS のエントリポイント。Xcode 側からこの ViewController を呼ぶ。
 * Kotlin/Native のリンクには Xcode の iOS SDK が必要。
 */
fun MainViewController(): UIViewController {
    val api = CoreApi(
        DatabaseRepository(openDatabase(IosDatabaseDriverFactory()))
    )
    return ComposeUIViewController {
        App(api)
    }
}
