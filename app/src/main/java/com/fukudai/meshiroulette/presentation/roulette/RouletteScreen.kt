package com.fukudai.meshiroulette.presentation.roulette

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fukudai.meshiroulette.domain.model.Restaurant
import com.fukudai.meshiroulette.presentation.components.FilterBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouletteScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: RouletteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("福大メシ・ルーレット") },
                actions = {
                    IconButton(onClick = { viewModel.showFilterSheet() }) {
                        Icon(Icons.Default.FilterList, contentDescription = "フィルター")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            RouletteAnimation(isSpinning = uiState.isSpinning)

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.selectedRestaurant != null) {
                ResultCard(
                    restaurant = uiState.selectedRestaurant!!,
                    onDetailClick = { onNavigateToDetail(uiState.selectedRestaurant!!.id) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = { viewModel.spinRoulette() },
                enabled = !uiState.isSpinning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (uiState.isSpinning) "選択中..." else "ルーレットを回す",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.selectedGenre.displayName != "すべて") {
                    FilterChipDisplay(label = uiState.selectedGenre.displayName)
                }
                if (uiState.selectedPriceRange.displayName != "すべて") {
                    FilterChipDisplay(label = uiState.selectedPriceRange.displayName)
                }
                if (uiState.isOpenNowOnly) {
                    FilterChipDisplay(label = "営業中のみ")
                }
            }
        }
    }

    if (uiState.showFilterSheet) {
        FilterBottomSheet(
            selectedGenre = uiState.selectedGenre,
            selectedPriceRange = uiState.selectedPriceRange,
            isOpenNowOnly = uiState.isOpenNowOnly,
            onGenreSelected = { viewModel.setGenre(it) },
            onPriceRangeSelected = { viewModel.setPriceRange(it) },
            onOpenNowOnlyChanged = { viewModel.setOpenNowOnly(it) },
            onDismiss = { viewModel.hideFilterSheet() }
        )
    }
}

@Composable
private fun RouletteAnimation(isSpinning: Boolean) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isSpinning) {
        if (isSpinning) {
            rotation.animateTo(
                targetValue = rotation.value + 1080f,
                animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .size(200.dp)
            .rotate(rotation.value),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Restaurant,
            contentDescription = "Roulette",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ResultCard(
    restaurant: Restaurant,
    onDetailClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "今日のおすすめ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = restaurant.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${restaurant.genre.displayName} | ${restaurant.priceRange.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onDetailClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("詳細を見る")
            }
        }
    }
}

@Composable
private fun FilterChipDisplay(label: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
