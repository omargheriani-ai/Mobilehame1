package com.mobilehame.solitaire.viewmodel

import androidx.lifecycle.ViewModel
import com.mobilehame.solitaire.model.Card
import com.mobilehame.solitaire.model.CardLocation
import com.mobilehame.solitaire.model.GameState
import com.mobilehame.solitaire.model.Suit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SolitaireViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(GameState.new())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    fun newGame() {
        _gameState.value = GameState.new()
    }

    fun dismissWin() {
        _gameState.value = _gameState.value.copy(isWon = false)
    }

    /**
     * Primary interaction handler. Handles:
     * 1. Tapping stock -> draw one card to waste
     * 2. Tapping waste when empty stock -> recycle waste to stock
     * 3. Tapping a face-up card to select it
     * 4. Tapping a destination when a card is selected -> attempt move
     */
    fun onCardTapped(location: CardLocation) {
        val state = _gameState.value

        when (location) {
            is CardLocation.Waste -> {
                if (state.selectedCardLocation != null) {
                    // Try to move selected card to... waste? Not a valid destination. Deselect.
                    _gameState.value = state.copy(selectedCardLocation = null)
                    return
                }
                // No selection — nothing to do for waste tap (draw happens via stock tap)
            }

            is CardLocation.Foundation -> {
                val sel = state.selectedCardLocation
                if (sel != null) {
                    // Attempt to move selected card(s) to foundation
                    val movedState = tryMoveToFoundation(state, sel, location.suitIndex)
                    if (movedState != null) {
                        val won = checkWin(movedState)
                        _gameState.value = movedState.copy(
                            selectedCardLocation = null,
                            isWon = won
                        )
                    } else {
                        // Invalid move — deselect
                        _gameState.value = state.copy(selectedCardLocation = null)
                    }
                } else {
                    // Select top card of foundation? (needed to move back to tableau)
                    val pile = state.foundations[location.suitIndex]
                    if (pile.isNotEmpty()) {
                        _gameState.value = state.copy(selectedCardLocation = location)
                    }
                }
            }

            is CardLocation.Tableau -> {
                val col = location.col
                val row = location.row
                val column = state.tableau[col]

                val sel = state.selectedCardLocation
                if (sel != null) {
                    if (sel is CardLocation.Tableau && sel.col == col && sel.row == row) {
                        // Tapping the same card deselects
                        _gameState.value = state.copy(selectedCardLocation = null)
                        return
                    }
                    // Try to move to this column
                    val movedState = tryMoveToTableau(state, sel, col)
                    if (movedState != null) {
                        val won = checkWin(movedState)
                        _gameState.value = movedState.copy(
                            selectedCardLocation = null,
                            isWon = won
                        )
                    } else {
                        // Invalid move — try selecting the tapped card instead
                        selectAt(state, location)
                    }
                } else {
                    // No selection — try to select
                    if (row < column.size && column[row].isFaceUp) {
                        _gameState.value = state.copy(selectedCardLocation = location)
                    } else if (row < column.size && !column[row].isFaceUp && row == column.size - 1) {
                        // Tap face-down top card to flip it
                        val newCol = column.toMutableList()
                        newCol[row] = newCol[row].faceUp()
                        val newTableau = state.tableau.toMutableList()
                        newTableau[col] = newCol
                        _gameState.value = state.copy(tableau = newTableau)
                    }
                }
            }
        }
    }

    fun onStockTapped() {
        val state = _gameState.value
        // Deselect any selection first
        val baseState = state.copy(selectedCardLocation = null)

        if (baseState.stock.isEmpty()) {
            // Recycle waste back to stock (face down, reversed)
            if (baseState.waste.isEmpty()) return
            val newStock = baseState.waste.reversed().map { it.faceDown() }
            _gameState.value = baseState.copy(stock = newStock, waste = emptyList())
        } else {
            // Draw one card from stock to waste
            val card = baseState.stock.last().faceUp()
            val newStock = baseState.stock.dropLast(1)
            val newWaste = baseState.waste + card
            _gameState.value = baseState.copy(stock = newStock, waste = newWaste)
        }
    }

    fun onWasteTapped() {
        val state = _gameState.value
        if (state.waste.isEmpty()) return

        val sel = state.selectedCardLocation
        if (sel != null) {
            // If waste is selected and user taps waste again, deselect
            if (sel is CardLocation.Waste) {
                _gameState.value = state.copy(selectedCardLocation = null)
            } else {
                // Deselect other selection
                _gameState.value = state.copy(selectedCardLocation = null)
            }
            return
        }
        // Select top waste card
        _gameState.value = state.copy(selectedCardLocation = CardLocation.Waste)
    }

    private fun selectAt(state: GameState, location: CardLocation.Tableau) {
        val col = location.col
        val row = location.row
        val column = state.tableau[col]
        if (row < column.size && column[row].isFaceUp) {
            _gameState.value = state.copy(selectedCardLocation = location)
        } else {
            _gameState.value = state.copy(selectedCardLocation = null)
        }
    }

    // Returns new state after moving, or null if move is invalid
    private fun tryMoveToFoundation(
        state: GameState,
        from: CardLocation,
        toSuitIndex: Int
    ): GameState? {
        val (card, newStateWithRemoval) = extractTopCard(state, from) ?: return null
        val foundation = state.foundations[toSuitIndex]

        // Card suit must match foundation index
        if (card.suit.ordinal != toSuitIndex) return null

        // Card rank must be exactly one more than top of foundation (or Ace if empty)
        val expectedRank = if (foundation.isEmpty()) 1 else foundation.last().rank + 1
        if (card.rank != expectedRank) return null

        val newFoundation = foundation + card
        val newFoundations = newStateWithRemoval.foundations.toMutableList()
        newFoundations[toSuitIndex] = newFoundation
        return newStateWithRemoval.copy(foundations = newFoundations)
    }

    // Returns new state after moving a card/stack to tableau column, or null if invalid
    private fun tryMoveToTableau(
        state: GameState,
        from: CardLocation,
        toCol: Int
    ): GameState? {
        val cards = extractCards(state, from) ?: return null
        val topCard = cards.first()
        val targetColumn = state.tableau[toCol]

        if (targetColumn.isEmpty()) {
            // Only Kings can go on empty columns
            if (topCard.rank != 13) return null
        } else {
            val targetTop = targetColumn.last()
            if (!targetTop.isFaceUp) return null
            // Must be alternating color and descending rank
            if (targetTop.isRed == topCard.isRed) return null
            if (targetTop.rank != topCard.rank + 1) return null
        }

        // Remove cards from source
        val stateAfterRemoval = removeCards(state, from, cards.size) ?: return null

        // Add cards to target column
        val newCol = stateAfterRemoval.tableau[toCol] + cards
        val newTableau = stateAfterRemoval.tableau.toMutableList()
        newTableau[toCol] = newCol
        return stateAfterRemoval.copy(tableau = newTableau)
    }

    // Extract the top single card from a location along with new state after removal
    private fun extractTopCard(state: GameState, from: CardLocation): Pair<Card, GameState>? {
        return when (from) {
            is CardLocation.Waste -> {
                if (state.waste.isEmpty()) return null
                val card = state.waste.last()
                val newWaste = state.waste.dropLast(1)
                card to state.copy(waste = newWaste)
            }
            is CardLocation.Foundation -> {
                val pile = state.foundations[from.suitIndex]
                if (pile.isEmpty()) return null
                val card = pile.last()
                val newPile = pile.dropLast(1)
                val newFoundations = state.foundations.toMutableList()
                newFoundations[from.suitIndex] = newPile
                card to state.copy(foundations = newFoundations)
            }
            is CardLocation.Tableau -> {
                val col = state.tableau[from.col]
                if (col.isEmpty() || from.row >= col.size) return null
                val card = col[from.row]
                if (!card.isFaceUp) return null
                // Only top card can go to foundation from tableau
                if (from.row != col.size - 1) return null
                val newCol = col.dropLast(1).let { remaining ->
                    if (remaining.isNotEmpty() && !remaining.last().isFaceUp) {
                        remaining.dropLast(1) + remaining.last().faceUp()
                    } else remaining
                }
                val newTableau = state.tableau.toMutableList()
                newTableau[from.col] = newCol
                card to state.copy(tableau = newTableau)
            }
        }
    }

    // Extract a sequence of cards starting from the given location (for tableau moves)
    private fun extractCards(state: GameState, from: CardLocation): List<Card>? {
        return when (from) {
            is CardLocation.Waste -> {
                if (state.waste.isEmpty()) null
                else listOf(state.waste.last())
            }
            is CardLocation.Foundation -> {
                val pile = state.foundations[from.suitIndex]
                if (pile.isEmpty()) null else listOf(pile.last())
            }
            is CardLocation.Tableau -> {
                val col = state.tableau[from.col]
                if (from.row >= col.size) return null
                val card = col[from.row]
                if (!card.isFaceUp) return null
                col.drop(from.row)
            }
        }
    }

    // Remove `count` cards from source location, flipping new top card if needed
    private fun removeCards(state: GameState, from: CardLocation, count: Int): GameState? {
        return when (from) {
            is CardLocation.Waste -> {
                if (state.waste.isEmpty()) return null
                state.copy(waste = state.waste.dropLast(count))
            }
            is CardLocation.Foundation -> {
                val pile = state.foundations[from.suitIndex]
                if (pile.isEmpty()) return null
                val newPile = pile.dropLast(count)
                val newFoundations = state.foundations.toMutableList()
                newFoundations[from.suitIndex] = newPile
                state.copy(foundations = newFoundations)
            }
            is CardLocation.Tableau -> {
                val col = state.tableau[from.col]
                if (from.row + count > col.size) return null
                var newCol = col.take(from.row)
                // Flip new top card if face down
                if (newCol.isNotEmpty() && !newCol.last().isFaceUp) {
                    newCol = newCol.dropLast(1) + newCol.last().faceUp()
                }
                val newTableau = state.tableau.toMutableList()
                newTableau[from.col] = newCol
                state.copy(tableau = newTableau)
            }
        }
    }

    private fun checkWin(state: GameState): Boolean {
        return state.foundations.all { it.size == 13 }
    }

    fun onFoundationTapped(suitIndex: Int) {
        val state = _gameState.value
        val sel = state.selectedCardLocation

        if (sel != null) {
            val movedState = tryMoveToFoundation(state, sel, suitIndex)
            if (movedState != null) {
                val won = checkWin(movedState)
                _gameState.value = movedState.copy(selectedCardLocation = null, isWon = won)
            } else {
                _gameState.value = state.copy(selectedCardLocation = null)
            }
        } else {
            val pile = state.foundations[suitIndex]
            if (pile.isNotEmpty()) {
                _gameState.value = state.copy(
                    selectedCardLocation = CardLocation.Foundation(suitIndex)
                )
            }
        }
    }

    fun onTableauColumnTapped(col: Int) {
        val state = _gameState.value
        val column = state.tableau[col]

        val sel = state.selectedCardLocation
        if (sel != null) {
            // Try to move to empty column
            if (column.isEmpty()) {
                val movedState = tryMoveToTableau(state, sel, col)
                if (movedState != null) {
                    val won = checkWin(movedState)
                    _gameState.value = movedState.copy(selectedCardLocation = null, isWon = won)
                    return
                }
            }
            _gameState.value = state.copy(selectedCardLocation = null)
        }
    }

    // Auto-move top waste card to foundation if possible
    fun tryAutoMoveWasteToFoundation() {
        val state = _gameState.value
        if (state.waste.isEmpty()) return
        val card = state.waste.last()
        val suitIndex = card.suit.ordinal
        val movedState = tryMoveToFoundation(state, CardLocation.Waste, suitIndex)
        if (movedState != null) {
            val won = checkWin(movedState)
            _gameState.value = movedState.copy(selectedCardLocation = null, isWon = won)
        }
    }

    // Auto-move top card of a tableau column to foundation if possible
    fun tryAutoMoveTableauToFoundation(col: Int) {
        val state = _gameState.value
        val column = state.tableau[col]
        if (column.isEmpty()) return
        val topRow = column.size - 1
        if (!column[topRow].isFaceUp) return
        val card = column[topRow]
        val suitIndex = card.suit.ordinal
        val from = CardLocation.Tableau(col, topRow)
        val movedState = tryMoveToFoundation(state, from, suitIndex)
        if (movedState != null) {
            val won = checkWin(movedState)
            _gameState.value = movedState.copy(selectedCardLocation = null, isWon = won)
        }
    }
}
