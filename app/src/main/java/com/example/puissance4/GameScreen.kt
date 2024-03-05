package com.example.puissance4

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.puissance4.ui.theme.Puissance4Theme
import kotlin.math.max
import kotlin.math.min


class GameScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Puissance4Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("mainMenu") }
                    var difficulty by remember { mutableStateOf(2) } // Pour "Joueur vs IA"

                    when (currentScreen) {
                        "mainMenu" -> MainMenu(onNavigate = { screen, selectedDifficulty ->
                            if (screen == "playerVsAI" && selectedDifficulty != null) {
                                difficulty = selectedDifficulty
                                currentScreen = screen
                            } else {
                                currentScreen = screen
                            }
                        }, onStartPlayerVsAI = { selectedDifficulty ->
                            difficulty = selectedDifficulty
                            currentScreen = "playerVsAI"
                        })

                        "playerVsPlayer" -> PlayerVsPlayerGameScreen(
                            context = this
                        ) { currentScreen = "mainMenu" }

                        "playerVsAI" -> PlayerVsAIGameScreen(
                            context = this,
                            difficulty = difficulty
                        ) { currentScreen = "mainMenu" }

                        "gameHistory" -> GameHistoryScreen(onNavigateBack = { currentScreen = "mainMenu" })
                    }
                }
            }
        }
    }
}

/**
 * Composable for the game screen between a human player and an AI.
 *
 * @param context The context of the activity.
 * @param difficulty The difficulty level of the AI.
 * @param onGameEnd The callback to be invoked when the game ends.
 */
@Composable
fun PlayerVsAIGameScreen(
    context: Context,
    difficulty: Int,
    onGameEnd: () -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }
    var grid by remember { mutableStateOf(List(6) { MutableList(7) { 0 } }) }
    var currentPlayer by remember { mutableStateOf(1) }
    var winner by remember { mutableStateOf(0) }
    var playerName by remember { mutableStateOf("") }

        if (showDialog) {
            Dialog(onDismissRequest = { /* Ne fait rien pour empêcher la fermeture */ }) {
                Surface(shape = MaterialTheme.shapes.medium) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Entrez le nom du joueur", style = MaterialTheme.typography.headlineLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = playerName,
                            onValueChange = { playerName = it },
                            label = { Text("Joueur Humain") })
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (playerName.isNotEmpty()) {
                                    showDialog = false
                                }
                            },
                            enabled = playerName.isNotEmpty() // Le bouton est activé uniquement si le nom n'est pas vide
                        ) {
                            Text("Commencer la partie")
                        }
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            if (winner != 0) {
                Text(text = if (winner == 1) "$playerName a gagné !" else "L'IA a gagné !", style = MaterialTheme.typography.titleLarge)
            } else {
                Text(text = if (currentPlayer == 1) "Tour de $playerName" else "Tour de l'IA", style = MaterialTheme.typography.titleLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))

            GameGrid(grid, currentPlayer) { updatedGrid, win ->
                grid = updatedGrid
                if (win != 0) {
                    winner = win
                } else {
                    currentPlayer = if (currentPlayer == 1) 2 else 1
                }
            }

            Button(onClick = onGameEnd, modifier = Modifier.padding(top = 16.dp)) {
                Text("Retour au menu")
            }
        }

        // Gestion des tours de l'IA
                LaunchedEffect(currentPlayer, grid) {
                    if (currentPlayer == 2 && winner == 0) {
                        val (_, bestColumn) = minimaxWithAlphaBeta(grid, difficulty, true, Int.MIN_VALUE, Int.MAX_VALUE)
                        bestColumn?.let { columnIndex ->
                            kotlinx.coroutines.delay(500)
                            val (updatedGrid, win) = onColumnClick(grid, columnIndex, currentPlayer)
                            grid = updatedGrid
                            winner = win
                            currentPlayer = 1

                            if (winner != 0) {
                                val resultString = when(winner) {
                                    1 -> "$playerName (Joueur) a gagné"
                                    else -> "L'IA a gagné"
                                }
                                val difficultyString = when(difficulty) {
                                    2 -> "Facile"
                                    3 -> "Moyen"
                                    4 -> "Difficile"
                                    else -> "Hardcore"
                                }
                                saveGameResultHumanVsAI(context, resultString, "IA", difficultyString)
                                onGameEnd()
                            }
                        }
                    }
                }
        }
        }
}

/**
 * Composable for the game screen between two human players.
 *
 * @param context The context of the activity.
 * @param onGameEnd The callback to be invoked when the game ends.
 */
@Composable
fun PlayerVsPlayerGameScreen(
    context: Context,
    onGameEnd: () -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }
    var playerName1 by remember { mutableStateOf("") }
    var playerName2 by remember { mutableStateOf("") }
    var grid by remember { mutableStateOf(List(6) { MutableList(7) { 0 } }) }
    var currentPlayer by remember { mutableStateOf(1) }
    var winner by remember { mutableStateOf(0) }
    var winnerName by remember { mutableStateOf("") }

    // Dialogue pour saisir les noms des joueurs
    if (showDialog) {
        Dialog(onDismissRequest = { /* Ne fait rien pour empêcher la fermeture */ }) {
            Surface(shape = MaterialTheme.shapes.medium,) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Entrez les noms des joueurs", style = MaterialTheme.typography.headlineLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = playerName1,
                        onValueChange = { playerName1 = it },
                        label = { Text("Joueur 1") })
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = playerName2,
                        onValueChange = { playerName2 = it },
                        label = { Text("Joueur 2") })
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        if (playerName1.isNotEmpty() && playerName2.isNotEmpty()) {
                            showDialog = false
                        }
                    }) {
                        Text("Commencer la partie")
                    }
                }
            }
        }

    } else {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                if (winner != 0) {
                    val winningPlayerName = if (winner == 1) playerName1 else playerName2
                    Text("Le joueur $winningPlayerName a gagné !", style = MaterialTheme.typography.titleLarge)
                } else {
                    val currentPlayerName = if (currentPlayer == 1) playerName1 else playerName2
                    Text("Tour de $currentPlayerName", style = MaterialTheme.typography.titleLarge)
                }

                Spacer(modifier = Modifier.height(16.dp))

                GameGrid(grid, currentPlayer) { updatedGrid, win ->
                    grid = updatedGrid
                    if (win != 0) {
                        winner = win
                        winnerName = if (winner == 1) playerName1 else playerName2
                    } else {
                        currentPlayer = if (currentPlayer == 1) 2 else 1
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    if (winner != 0) {
                        Button(onClick = {
                            saveGameResultHumanVsHuman(
                                context,
                                winnerName,
                                if (winnerName == playerName1) playerName2 else playerName1
                            )
                            onGameEnd()
                        }) {
                            Text("Retour au menu")
                        }
                    }
                }
        }}
    }
}

/**
 * Minimax algorithm with alpha-beta pruning.
 *
 * @param grid The current game grid.
 * @param depth The maximum depth to search.
 * @param isMaximizing Whether it's the AI's turn to play (maximizing).
 * @param alpha The alpha value for alpha-beta pruning.
 * @param beta The beta value for alpha-beta pruning.
 * @return A pair containing the best score and the best column to play.
 */
fun minimaxWithAlphaBeta(grid: List<List<Int>>, depth: Int, isMaximizing: Boolean, alpha: Int, beta: Int): Pair<Int, Int?> {
    if (checkForWin(grid) != 0 || depth == 0) {
        // Critère d'arrêt : un joueur a gagné ou la profondeur maximale est atteinte
        return Pair(if (checkForWin(grid) == 2) 1 else if (checkForWin(grid) == 1) -1 else 0, null)
    }

    var myAlpha = alpha
    var myBeta = beta

    if (isMaximizing) {
        var bestScore = Int.MIN_VALUE
        var bestColumn: Int? = null
        for (column in 0 until grid[0].size) {
            val row = getPlayableRow(grid, column)
            if (row != null) {
                val newGrid = grid.map { it.toMutableList() }
                newGrid[row][column] = 2 // L'IA joue
                val score = minimaxWithAlphaBeta(newGrid, depth - 1, false, myAlpha, myBeta).first
                if (score > bestScore) {
                    bestScore = score
                    bestColumn = column
                }
                myAlpha = max(myAlpha, bestScore)
                if (myBeta <= myAlpha) {
                    break
                }
            }
        }
        return Pair(bestScore, bestColumn)
    } else {
        var bestScore = Int.MAX_VALUE
        var bestColumn: Int? = null
        for (column in 0 until grid[0].size) {
            val row = getPlayableRow(grid, column)
            if (row != null) {
                val newGrid = grid.map { it.toMutableList() }
                newGrid[row][column] = 1 // Le joueur joue
                val score = minimaxWithAlphaBeta(newGrid, depth - 1, true, myAlpha, myBeta).first
                if (score < bestScore) {
                    bestScore = score
                    bestColumn = column
                }
                myBeta = min(myBeta, bestScore)
                if (myBeta <= myAlpha) {
                    break
                }
            }
        }
        return Pair(bestScore, bestColumn)
    }
}

/**
 * Get the playable row for a given column in the grid.
 *
 * @param grid The game grid.
 * @param column The column to check.
 * @return The row number if there's a playable row, null otherwise.
 */
fun getPlayableRow(grid: List<List<Int>>, column: Int): Int? {
    for (row in grid.size - 1 downTo 0) {
        if (grid[row][column] == 0) {
            return row
        }
    }
    return null
}

/**
 * Check if there's a winner on the grid.
 *
 * @param grid The game grid.
 * @return The player number (1 or 2) if there's a winner, 0 otherwise.
 */
fun checkForWin(grid: List<List<Int>>): Int {
    val maxRow = grid.size
    val maxCol = grid[0].size

    fun checkDirection(row: Int, col: Int, dRow: Int, dCol: Int, player: Int, count: Int = 0): Boolean {
        if (row !in 0 until maxRow || col !in 0 until maxCol || grid[row][col] != player) {
            return false
        }
        // Si nous avons trouvé 4 en ligne, retournez vrai
        if (count == 3) return true
        // Continuez à chercher dans la direction donnée
        return checkDirection(row + dRow, col + dCol, dRow, dCol, player, count + 1)
    }

    for (row in 0 until maxRow) {
        for (col in 0 until maxCol) {
            val player = grid[row][col]
            if (player != 0) {
                // Vérifiez dans toutes les directions nécessaires à partir de ce point
                if (checkDirection(row, col, 0, 1, player) || // Horizontal
                    checkDirection(row, col, 1, 0, player) || // Vertical
                    checkDirection(row, col, 1, 1, player) || // Diagonale vers le bas à droite
                    checkDirection(row, col, 1, -1, player)) { // Diagonale vers le bas à gauche
                    return player
                }
            }
        }
    }
    return 0 // Aucun gagnant trouvé
}

/**
 * Handle a click on a column, updating the grid and returning the winner (if any).
 *
 * @param grid The game grid.
 * @param columnIndex The index of the clicked column.
 * @param currentPlayer The current player.
 * @return A pair containing the updated grid and the winner (if any).
 */
fun onColumnClick(grid: List<MutableList<Int>>, columnIndex: Int, currentPlayer: Int): Pair<List<MutableList<Int>>, Int> {
    val row = getPlayableRow(grid, columnIndex)
    if (row != null) {
        grid[row][columnIndex] = currentPlayer
        val winner = checkForWin(grid)
        return Pair(grid, winner)
    }
    return Pair(grid, 0) // Retourne l'état actuel du jeu si le coup n'est pas possible
}

/**
 * Composable for the game grid.
 *
 * @param grid The game grid.
 * @param currentPlayer The current player.
 * @param onMoveMade The callback to be invoked when a move is made.
 */
@Composable
fun GameGrid(grid: List<MutableList<Int>>, currentPlayer: Int, onMoveMade: (MutableList<MutableList<Int>>, Int) -> Unit) {
    Column {
        for (row in grid) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for ((columnIndex, cell) in row.withIndex()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .background(Color.LightGray, shape = RoundedCornerShape(4.dp))
                            .clickable {
                                // Appel à onColumnClick à chaque fois qu'une colonne est cliquée
                                val (updatedGrid, winner) = onColumnClick(grid, columnIndex, currentPlayer)
                                onMoveMade(updatedGrid.toMutableList(), winner)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (cell != 0) { // Si la cellule n'est pas vide, dessinez un cercle
                            Box(
                                modifier = Modifier
                                    .size(30.dp) // Taille du cercle
                                    .background(
                                        color = when (cell) {
                                            1 -> Color.Red // Joueur 1
                                            2 -> Color.Yellow // Joueur 2
                                            else -> Color.Transparent // Ne devrait pas arriver
                                        },
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Composable for the main menu.
 *
 * @param onNavigate The callback to be invoked when navigating to another screen.
 * @param onStartPlayerVsAI The callback to be invoked when starting a game against the AI.
 */
@Composable
fun MainMenu(onNavigate: (String, Int?) -> Unit, onStartPlayerVsAI: (Int) -> Unit) {
    var showDifficultySelector by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = { onNavigate("playerVsPlayer", null) }) {
            Text("Joueur contre Joueur")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { showDifficultySelector = true }) {
            Text("Joueur contre IA")
        }
        // Affiche le sélecteur de difficulté si demandé
        if (showDifficultySelector) {
            DifficultySelector { difficulty ->
                onStartPlayerVsAI(difficulty)
                showDifficultySelector = false
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onNavigate("gameHistory", null) }) {
            Text("Historique des parties")
        }
    }
}

/**
 * Composable for the game history screen.
 *
 * @param onNavigateBack The callback to be invoked when navigating back to the main menu.
 */
@Composable
fun GameHistoryScreen(onNavigateBack: () -> Unit) {
    // Contexte obtenu via LocalContext en Compose
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("game_history", Context.MODE_PRIVATE)
    val gameHistory = sharedPreferences.getString("history", "") ?: ""

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Historique des parties", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onNavigateBack) {
                Text("Retour au menu")
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(gameHistory.split("\n").filterNot { it.isBlank() }) { historyEntry ->
                    Text(text = historyEntry)
                    Divider()
                }
            }
        }
    }
}

/**
 * Save the game result for a human vs human game.
 *
 * @param context The context of the activity.
 * @param winnerName The name of the winner.
 * @param loserName The name of the loser.
 */
fun saveGameResultHumanVsHuman(context: Context, winnerName: String, loserName: String) {
    val sharedPreferences = context.getSharedPreferences("game_history", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val currentHistory = sharedPreferences.getString("history", "") ?: ""
    val newEntry = "$winnerName contre $loserName\n"
    editor.putString("history", currentHistory + newEntry)
    editor.apply()
}

/**
 * Save the game result for a human vs AI game.
 *
 * @param context The context of the activity.
 * @param winnerName The name of the winner.
 * @param loserName The name of the loser.
 * @param difficulty The difficulty level of the AI.
 */
fun saveGameResultHumanVsAI(context: Context, winnerName: String, loserName: String, difficulty: String?) {
    val sharedPreferences = context.getSharedPreferences("game_history", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val currentHistory = sharedPreferences.getString("history", "") ?: ""
    val newEntry = "$winnerName contre $loserName${if (difficulty != null) " - Difficulté: $difficulty" else ""}\n"
    editor.putString("history", currentHistory + newEntry)
    editor.apply()
}

/**
 * Composable for the difficulty selector.
 *
 * @param onDifficultySelected The callback to be invoked when a difficulty is selected.
 */
@Composable
fun DifficultySelector(onDifficultySelected: (Int) -> Unit) {
    val difficulties = listOf("Facile", "Moyen", "Difficile", "Hardcore")
    val colors = listOf(Color.Green, Color(0xFFFFA500), Color.Red, Color.Black) // Vert, Orange, Rouge, Noir
    var selectedDifficulty by remember { mutableStateOf(0) } // 0: facile, 1: moyen, etc.

    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Sélectionnez la difficulté", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(8.dp))
        difficulties.forEachIndexed { index, difficulty ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                selectedDifficulty = index
                onDifficultySelected(index + 5)
            }.padding(8.dp)) {
                Box(modifier = Modifier
                    .size(20.dp)
                    .background(color = colors[index], shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(difficulty)
            }
        }
    }
}
