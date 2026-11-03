package org.openyaske.app.ui.design

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow

/**
 * シートを開いている間だけ戻るを 1 枚積む。
 *
 * 画面遷移の戻る（`App` の `BackHandler`）より内側で登録されるため、
 * シートが開いていればまずシートが閉じる。ダイアログは各プラットフォームの
 * Dialog が戻るを持つので、ここではボトムシートに使う。
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun ModalBackHandler(onDismiss: () -> Unit) {
    androidx.compose.ui.backhandler.BackHandler(enabled = true, onBack = onDismiss)
}

/**
 * シートを閉じたときの手触り。
 *
 * 下に払って閉じても、戻るや外側タップで閉じても 1 回だけ出す。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetDismissHaptic(state: SheetState) {
    val wasVisible = remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        snapshotFlow { state.isVisible }.collect { visible ->
            if (visible) {
                wasVisible.value = true
            } else if (wasVisible.value) {
                wasVisible.value = false
                tapFeedback()
            }
        }
    }
}
