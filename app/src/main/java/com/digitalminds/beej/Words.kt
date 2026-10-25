package com.digitalminds.beej

data class BeeWord(val id: Int, val value: String, val meaning: String) {
    val letters: String get() = value.uppercase()
}

object Words {
    val all = listOf(
        BeeWord(1, "SINGER", "cantante"),
        BeeWord(2, "LAWYER", "abogado o abogada"),
        BeeWord(3, "ALWAYS", "siempre"),
        BeeWord(4, "WAITER", "mesero o mesera"),
        BeeWord(5, "REALLY", "realmente"),
        BeeWord(6, "PEOPLE", "personas"),
        BeeWord(7, "DOCTOR", "doctor o doctora"),
        BeeWord(8, "PILOT", "piloto"),
        BeeWord(9, "SUGAR", "azúcar"),
        BeeWord(10, "COFFEE", "café"),
        BeeWord(11, "MUSICIAN", "músico o música"),
        BeeWord(12, "BEAUTIFUL", "hermoso o hermosa"),
        BeeWord(13, "EXCITED", "emocionado o emocionada"),
        BeeWord(14, "EXPENSIVE", "costoso o costosa"),
        BeeWord(15, "NORMALLY", "normalmente"),
        BeeWord(16, "POLICEMAN", "policía"),
        BeeWord(17, "RECORDING", "grabación"),
        BeeWord(18, "CAREFUL", "cuidadoso o cuidadosa"),
        BeeWord(19, "PINEAPPLE", "piña"),
        BeeWord(20, "DANGEROUS", "peligroso o peligrosa"),
        BeeWord(21, "WATERMELON", "sandía"),
        BeeWord(22, "INTERESTING", "interesante"),
        BeeWord(23, "UNCOUNTABLE", "incontable"),
        BeeWord(24, "PARTICIPATION", "participación"),
        BeeWord(25, "PAPARAZZO", "paparazi"),
        BeeWord(26, "DICTIONARY", "diccionario"),
        BeeWord(27, "PRONUNCIATION", "pronunciación"),
        BeeWord(28, "COMPREHENSION", "comprensión"),
        BeeWord(29, "COMMUNITY", "comunidad"),
        BeeWord(30, "INTELLIGENT", "inteligente")
    )

    fun byId(id: Int): BeeWord = all.first { it.id == id }

    fun normalizeLetters(input: String): String = input.uppercase().filter { it in 'A'..'Z' }

    fun planWords(day: Int): List<BeeWord> {
        val groups = listOf(
            1..3, 4..6, 7..9, 10..12, 13..15, 16..18, 19..21, 22..24, 25..27, 28..30,
            1..10, 11..20, 21..30, 1..30
        )
        val index = (day.coerceIn(1, 14) - 1)
        val range = groups[index]
        return all.filter { it.id in range }
    }
}
