package com.mobilehame.solitaire.model

data class GameState(
    val tableau: List<List<Card>>,       // 7 columns
    val foundations: List<List<Card>>,   // 4 piles, indexed by Suit.ordinal
    val stock: List<Card>,               // face-down draw pile
    val waste: List<Card>,               // face-up played cards
    val selectedCardLocation: CardLocation? = null,
    val isWon: Boolean = false
) {
    companion object {
        fun new(): GameState {
            val deck = buildDeck().shuffled()
            val tableau = mutableListOf<List<Card>>()
            var index = 0
            for (col in 0 until 7) {
                val column = mutableListOf<Card>()
                for (row in 0..col) {
                    val card = if (row == col) deck[index].faceUp() else deck[index].faceDown()
                    column.add(card)
                    index++
                }
                tableau.add(column)
            }
            val stock = deck.drop(index).map { it.faceDown() }
            return GameState(
                tableau = tableau,
                foundations = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
                stock = stock,
                waste = emptyList()
            )
        }

        private fun buildDeck(): List<Card> {
            val cards = mutableListOf<Card>()
            for (suit in Suit.values()) {
                for (rank in 1..13) {
                    cards.add(Card(suit, rank))
                }
            }
            return cards
        }
    }
}

sealed class CardLocation {
    data class Tableau(val col: Int, val row: Int) : CardLocation()
    data class Foundation(val suitIndex: Int) : CardLocation()
    object Waste : CardLocation()
}
