package org.openyaske.app.ui.design

import androidx.compose.ui.text.TextStyle

/**
 * 数字を等幅で並べる。時刻・単位数・GPA・件数に使う。
 *
 * `MaterialTheme.typography.bodySmall.tabular()` のようにロールへ重ねる。
 */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")
