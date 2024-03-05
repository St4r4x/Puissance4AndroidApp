package com.example.puissance4

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.puissance4.ui.theme.Puissance4Theme

class GameScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Puissance4Theme {
                // Utilisation de Surface pour le conteneur avec le fond du thème
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var currentScreen by remember { mutableStateOf("mainMenu") }
                    // Affiche le menu principal au démarrage
                    when (currentScreen) {
                        "mainMenu" -> MainMenu(onNavigate = { screen ->
                            currentScreen = screen
                        })
                        "playerVsPlayer" -> PlayerVsPlayerGameScreen(onGameEnd = {
                            // Réinitialiser pour afficher le menu principal après la fin d'une partie
                            currentScreen = "mainMenu"
                        })
                    }
                }
            }
        }
    }
    @Composable
    fun PlayerVsPlayerGameScreen(onGameEnd: () -> Unit) {
        // Utilisation de Surface pour le conteneur avec le fond du thème
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            var grid by remember { mutableStateOf(List(6) { MutableList(7) { 0 } }) }
            var currentPlayer by remember { mutableStateOf(1) }
            var winner by remember { mutableStateOf(0) } // Ajout d'une variable pour le gagnant

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                // Condition pour afficher le gagnant
                if (winner != 0) {
                    Text(text = "Le joueur $winner a gagné !", style = MaterialTheme.typography.titleLarge)
                } else {
                    Text(text = "Tour du Joueur $currentPlayer", style = MaterialTheme.typography.titleLarge)
                }

                Spacer(modifier = Modifier.height(16.dp)) // Un peu d'espace entre le texte et la grille

                // La grille de jeu
                GameGrid(grid = grid, onColumnClick = { columnIndex ->
                    if (winner == 0) { // Ne permettez les mouvements que si aucun gagnant n'a été déterminé
                        val row = grid.withIndex().lastOrNull { it.value[columnIndex] == 0 }?.index
                        if (row != null) {
                            grid = grid.mapIndexed { rowIndex, rowContent ->
                                if (rowIndex == row) {
                                    rowContent.toMutableList().apply { this[columnIndex] = currentPlayer }
                                } else {
                                    rowContent
                                }
                            }
                            winner = checkForWin(grid) // Vérifiez s'il y a un gagnant
                            if (winner == 0) {
                                currentPlayer = if (currentPlayer == 1) 2 else 1
                            }
                        }
                    }
                })

                // Bouton pour retourner au menu principal
                Button(onClick = onGameEnd, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Retour au menu")
                }
            }
        }
    }
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
@Composable
fun GameGrid(grid: List<List<Int>>, onColumnClick: (Int) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        for (row in grid) {
            Row {
                for ((columnIndex, cell) in row.withIndex()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .background(Color.LightGray, shape = RoundedCornerShape(4.dp)),
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
        // Affichage des boutons en dessous de chaque colonne, comme avant
        Row(modifier = Modifier.padding(top = 8.dp)) {
            for (columnIndex in 0 until grid[0].size) {
                if (grid[0][columnIndex] == 0) { // Vérifie si le premier élément de la colonne est vide
                    Button(
                        onClick = { onColumnClick(columnIndex) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(text = "↓", modifier = Modifier.padding(2.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) // Remplacer par un espace vide si la colonne est pleine
                }
            }
        }
    }
}
@Composable
fun MainMenu(onNavigate: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = { onNavigate("playerVsPlayer") }) {
            Text("Joueur contre Joueur")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onNavigate("playerVsAI") }) {
            Text("Joueur contre IA")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onNavigate("gameHistory") }) {
            Text("Historique des parties")
        }
    }
}