package com.myhitbodedut

/** Un conseil : son rang, son titre, son texte, et l'illustration qui lui va. */
data class Tip(val number: Int, val titleRes: Int, val bodyRes: Int, val art: Int)

/** Les dix repères de la hitbodedout, dans l'ordre. */
object Tips {

    val ALL: List<Tip> = listOf(
        Tip(1, R.string.tip_1_title, R.string.tip_1_body, R.drawable.char_walking),
        Tip(2, R.string.tip_2_title, R.string.tip_2_body, R.drawable.char_thinking),
        Tip(3, R.string.tip_3_title, R.string.tip_3_body, R.drawable.char_open_arms),
        Tip(4, R.string.tip_4_title, R.string.tip_4_body, R.drawable.char_standing_back),
        Tip(5, R.string.tip_5_title, R.string.tip_5_body, R.drawable.char_tehilim),
        Tip(6, R.string.tip_6_title, R.string.tip_6_body, R.drawable.char_writing),
        Tip(7, R.string.tip_7_title, R.string.tip_7_body, R.drawable.char_praise),
        Tip(8, R.string.tip_8_title, R.string.tip_8_body, R.drawable.char_praying),
        Tip(9, R.string.tip_9_title, R.string.tip_9_body, R.drawable.char_standing_back),
        Tip(10, R.string.tip_10_title, R.string.tip_10_body, R.drawable.char_open_arms)
    )
}
