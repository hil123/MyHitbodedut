package com.myhitbodedut

/**
 * « Je ne sais pas quoi dire » (propositions 3 et 15 de la feuille de route,
 * 19/10) : au lieu d'un seul texte, des questions, une à la fois.
 *
 * Elles suivent « מה אומרים? » du guide du propriétaire (textes/
 * guide_hitbodedut.md, חלק יד) : remercier, raconter, dire ce qu'on ressent,
 * demander le matériel puis le spirituel, faire son 'heshbon nefesh,
 * demander pardon, une petite décision, l'avenir, et finir en remerciant.
 */
object QuestionsBlocage {

    val QUESTIONS = listOf(
        R.string.question_merci,
        R.string.question_raconter,
        R.string.question_ressenti,
        R.string.question_besoins,
        R.string.question_force,
        R.string.question_bilan,
        R.string.question_pardon,
        R.string.question_decision,
        R.string.question_demain,
        R.string.question_fin,
    )

    /** La suivante ; après la dernière, on recommence. */
    fun suivante(i: Int): Int = if (i < 0) 0 else (i + 1) % QUESTIONS.size
}
