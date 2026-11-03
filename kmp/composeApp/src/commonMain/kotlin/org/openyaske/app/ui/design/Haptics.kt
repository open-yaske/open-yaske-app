package org.openyaske.app.ui.design

/**
 * 手触り。押した・切り替えたの 2 種類だけを出す。
 * スクロールやアニメーションには付けない。
 */

/** 軽いクリック。ボタン・FAB・セルの押下。 */
expect fun tapFeedback()

/** 選択の切り替え。タブ・チェックの完了。 */
expect fun selectFeedback()
