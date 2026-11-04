package org.openyaske.core

import kotlin.test.Test
import kotlin.test.assertEquals
import org.openyaske.core.net.urlEncodeQuery

class UrlEncodeTest {
    @Test
    fun encodesJapaneseAndSpaces() {
        assertEquals(
            "%E8%AA%BF%E5%B8%83%20%E9%A7%85",
            urlEncodeQuery("調布 駅")
        )
    }

    @Test
    fun keepsUnreservedCharacters() {
        assertEquals("Chofu-1_2.3~", urlEncodeQuery("Chofu-1_2.3~"))
    }
}
