package com.digitalminds.beej

data class BeeWord(val id: Int, val value: String, val meaning: String) {
    val letters: String get() = value.uppercase()
}

object Words {
    // Exactly the 30 entries for 3rd, 4th and 5th grades. CARIFUL corrected to CAREFUL.
    val all = listOf(
        BeeWord(1,"SINGER","cantante"), BeeWord(2,"LAWYER","abogado/a"),
        BeeWord(3,"ALWAYS","siempre"), BeeWord(4,"WAITER","mesero/a"),
        BeeWord(5,"REALLY","realmente"), BeeWord(6,"PEOPLE","personas"),
        BeeWord(7,"DOCTOR","médico/a"), BeeWord(8,"PILOT","piloto"),
        BeeWord(9,"SUGAR","azúcar"), BeeWord(10,"COFFEE","café"),
        BeeWord(11,"MUSICIAN","músico/a"), BeeWord(12,"BEAUTIFUL","hermoso/a"),
        BeeWord(13,"EXCITED","emocionado/a"), BeeWord(14,"EXPENSIVE","costoso/a"),
        BeeWord(15,"NORMALLY","normalmente"), BeeWord(16,"POLICEMAN","policía"),
        BeeWord(17,"RECORDING","grabación"), BeeWord(18,"CAREFUL","cuidadoso/a"),
        BeeWord(19,"PINEAPPLE","piña"), BeeWord(20,"DANGEROUS","peligroso/a"),
        BeeWord(21,"WATERMELON","sandía"), BeeWord(22,"INTERESTING","interesante"),
        BeeWord(23,"UNCOUNTABLE","incontable"), BeeWord(24,"PARTICIPATION","participación"),
        BeeWord(25,"PAPARAZZO","paparazi"), BeeWord(26,"DICTIONARY","diccionario"),
        BeeWord(27,"PRONUNCIATION","pronunciación"), BeeWord(28,"COMPREHENSION","comprensión"),
        BeeWord(29,"COMMUNITY","comunidad"), BeeWord(30,"INTELLIGENT","inteligente")
    )
    fun normalize(input: String): String = input.trim().uppercase().filter { it in 'A'..'Z' }
    fun isCorrect(word: BeeWord, attempt: String) = normalize(attempt) == word.letters
    fun dayGroup(day: Int): List<BeeWord> = when(day.coerceIn(1,14)) {
        in 1..6 -> all.drop((day-1)*5).take(5)
        in 7..10 -> all.drop((day-7)*8).take(8)
        else -> all
    }
}
