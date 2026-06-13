package com.mobilehame.solitaire.model

data class Card(
    val suit: Suit,
    val rank: Int, // 1=Ace, 2-10, 11=Jack, 12=Queen, 13=King
    val isFaceUp: Boolean = false
) {
    val rankLabel: String
        get() = when (rank) {
            1 -> "A"
            11 -> "J"
            12 -> "Q"
            13 -> "K"
            else -> rank.toString()
        }

    val isRed: Boolean get() = suit.isRed

    fun faceUp() = copy(isFaceUp = true)
    fun faceDown() = copy(isFaceUp = false)
}
