package com.mobilehame.solitaire.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilehame.solitaire.model.Card
import com.mobilehame.solitaire.model.CardLocation
import com.mobilehame.solitaire.model.GameState
import com.mobilehame.solitaire.model.Suit
import com.mobilehame.solitaire.ui.theme.*
import com.mobilehame.solitaire.viewmodel.SolitaireViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolitaireScreen(viewModel: SolitaireViewModel = viewModel()) {
    val gameState by viewModel.gameState.collectAsState()

    SolitaireTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Solitaire",
                            color = CardWhite,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        TextButton(onClick = { viewModel.newGame() }) {
                            Text("New Game", color = HighlightColor, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = FeltGreenDark
                    )
                )
            },
            containerColor = FeltGreen
        ) { paddingValues ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(4.dp)
            ) {
                val screenWidth = maxWidth
                // Each card gets 1/7 of the width minus some padding
                val cardWidth = (screenWidth - 8.dp) / 7f - 2.dp
                val cardHeight = cardWidth * 1.4f

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Top row: stock, waste, spacer, foundations
                    TopRow(
                        gameState = gameState,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        onStockTapped = { viewModel.onStockTapped() },
                        onWasteTapped = { viewModel.onWasteTapped() },
                        onFoundationTapped = { suitIndex -> viewModel.onFoundationTapped(suitIndex) }
                    )

                    // Tableau
                    TableauSection(
                        gameState = gameState,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        onCardTapped = { location -> viewModel.onCardTapped(location) },
                        onColumnTapped = { col -> viewModel.onTableauColumnTapped(col) }
                    )
                }

                // Win dialog
                if (gameState.isWon) {
                    WinDialog(
                        onNewGame = {
                            viewModel.dismissWin()
                            viewModel.newGame()
                        },
                        onDismiss = { viewModel.dismissWin() }
                    )
                }
            }
        }
    }
}

@Composable
fun TopRow(
    gameState: GameState,
    cardWidth: Dp,
    cardHeight: Dp,
    onStockTapped: () -> Unit,
    onWasteTapped: () -> Unit,
    onFoundationTapped: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stock pile
        StockPile(
            stock = gameState.stock,
            cardWidth = cardWidth,
            cardHeight = cardHeight,
            onClick = onStockTapped
        )

        // Waste pile
        WastePile(
            waste = gameState.waste,
            cardWidth = cardWidth,
            cardHeight = cardHeight,
            isSelected = gameState.selectedCardLocation is CardLocation.Waste,
            onClick = onWasteTapped
        )

        // Spacer
        Spacer(modifier = Modifier.weight(1f))

        // Foundation piles (4)
        for (suitIndex in 0 until 4) {
            val suit = Suit.values()[suitIndex]
            val pile = gameState.foundations[suitIndex]
            val isSelected = gameState.selectedCardLocation is CardLocation.Foundation &&
                    (gameState.selectedCardLocation as CardLocation.Foundation).suitIndex == suitIndex
            FoundationPile(
                suit = suit,
                pile = pile,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                isSelected = isSelected,
                onClick = { onFoundationTapped(suitIndex) }
            )
        }
    }
}

@Composable
fun StockPile(
    stock: List<Card>,
    cardWidth: Dp,
    cardHeight: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (stock.isNotEmpty()) {
            CardBack(cardWidth = cardWidth, cardHeight = cardHeight)
        } else {
            // Empty stock — show recycle symbol
            EmptyPilePlaceholder(
                label = "↺",
                cardWidth = cardWidth,
                cardHeight = cardHeight
            )
        }
    }
}

@Composable
fun WastePile(
    waste: List<Card>,
    cardWidth: Dp,
    cardHeight: Dp,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (waste.isNotEmpty()) {
            CardFace(
                card = waste.last(),
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                isSelected = isSelected,
                onClick = onClick
            )
        } else {
            EmptyPilePlaceholder(
                label = "",
                cardWidth = cardWidth,
                cardHeight = cardHeight
            )
        }
    }
}

@Composable
fun FoundationPile(
    suit: Suit,
    pile: List<Card>,
    cardWidth: Dp,
    cardHeight: Dp,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (pile.isNotEmpty()) {
            CardFace(
                card = pile.last(),
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                isSelected = isSelected,
                onClick = onClick
            )
        } else {
            EmptyPilePlaceholder(
                label = suit.symbol,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                labelColor = if (suit.isRed) CardRed else Color(0xFF666666)
            )
        }
    }
}

@Composable
fun TableauSection(
    gameState: GameState,
    cardWidth: Dp,
    cardHeight: Dp,
    onCardTapped: (CardLocation) -> Unit,
    onColumnTapped: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (col in 0 until 7) {
            val column = gameState.tableau[col]
            TableauColumn(
                col = col,
                column = column,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                selectedLocation = gameState.selectedCardLocation,
                onCardTapped = onCardTapped,
                onColumnTapped = onColumnTapped,
                modifier = Modifier.width(cardWidth)
            )
        }
    }
}

@Composable
fun TableauColumn(
    col: Int,
    column: List<Card>,
    cardWidth: Dp,
    cardHeight: Dp,
    selectedLocation: CardLocation?,
    onCardTapped: (CardLocation) -> Unit,
    onColumnTapped: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Vertical overlap offset
    val faceDownOffset = cardHeight * 0.18f
    val faceUpOffset = cardHeight * 0.28f

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onColumnTapped(col) }
    ) {
        if (column.isEmpty()) {
            EmptyPilePlaceholder(
                label = "",
                cardWidth = cardWidth,
                cardHeight = cardHeight
            )
        } else {
            // Calculate total height needed
            var currentOffset = 0.dp
            column.forEachIndexed { row, card ->
                val isSelected = when (selectedLocation) {
                    is CardLocation.Tableau -> selectedLocation.col == col && selectedLocation.row <= row && card.isFaceUp
                    else -> false
                }

                Box(modifier = Modifier.offset(y = currentOffset)) {
                    if (card.isFaceUp) {
                        CardFace(
                            card = card,
                            cardWidth = cardWidth,
                            cardHeight = cardHeight,
                            isSelected = isSelected,
                            onClick = { onCardTapped(CardLocation.Tableau(col, row)) }
                        )
                    } else {
                        CardBack(
                            cardWidth = cardWidth,
                            cardHeight = cardHeight,
                            onClick = { onCardTapped(CardLocation.Tableau(col, row)) }
                        )
                    }
                }

                currentOffset += if (card.isFaceUp) faceUpOffset else faceDownOffset
            }
        }
    }
}

@Composable
fun CardFace(
    card: Card,
    cardWidth: Dp,
    cardHeight: Dp,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (card.isRed) CardRed else CardBlack
    val borderColor = if (isSelected) HighlightColor else Color.Transparent
    val borderWidth = if (isSelected) 2.dp else 0.dp

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .clip(RoundedCornerShape(4.dp))
            .background(CardWhite)
            .border(borderWidth, borderColor, RoundedCornerShape(4.dp))
            .clickable { onClick() }
    ) {
        // Top-left rank and suit
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 2.dp, top = 1.dp)
        ) {
            Text(
                text = card.rankLabel,
                color = textColor,
                fontSize = (cardWidth.value * 0.22f).sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (cardWidth.value * 0.22f).sp
            )
            Text(
                text = card.suit.symbol,
                color = textColor,
                fontSize = (cardWidth.value * 0.2f).sp,
                lineHeight = (cardWidth.value * 0.2f).sp
            )
        }

        // Center suit symbol
        Text(
            text = card.suit.symbol,
            color = textColor,
            fontSize = (cardWidth.value * 0.38f).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
            textAlign = TextAlign.Center
        )

        // Bottom-right rank and suit (rotated 180)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 2.dp, bottom = 1.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = card.suit.symbol,
                color = textColor,
                fontSize = (cardWidth.value * 0.2f).sp,
                lineHeight = (cardWidth.value * 0.2f).sp
            )
            Text(
                text = card.rankLabel,
                color = textColor,
                fontSize = (cardWidth.value * 0.22f).sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (cardWidth.value * 0.22f).sp
            )
        }
    }
}

@Composable
fun CardBack(
    cardWidth: Dp,
    cardHeight: Dp,
    onClick: (() -> Unit)? = null
) {
    val mod = Modifier
        .width(cardWidth)
        .height(cardHeight)
        .clip(RoundedCornerShape(4.dp))
        .background(CardBack)
        .border(1.dp, CardBackPattern, RoundedCornerShape(4.dp))
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)

    Box(modifier = mod) {
        // Simple crosshatch pattern using nested boxes
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
                .border(1.dp, CardBackPattern.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
                .border(1.dp, CardBackPattern.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun EmptyPilePlaceholder(
    label: String,
    cardWidth: Dp,
    cardHeight: Dp,
    labelColor: Color = Color(0xFF4A9A4A)
) {
    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .clip(RoundedCornerShape(4.dp))
            .background(FoundationEmpty.copy(alpha = 0.4f))
            .border(1.dp, Color(0xFF4A9A4A), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = labelColor,
                fontSize = (cardWidth.value * 0.38f).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WinDialog(
    onNewGame: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "You Won!",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        },
        text = {
            Text(
                text = "Congratulations! You've completed the game!\n\nAll 52 cards are in the foundations.",
                fontSize = 16.sp
            )
        },
        confirmButton = {
            Button(onClick = onNewGame) {
                Text("New Game")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
