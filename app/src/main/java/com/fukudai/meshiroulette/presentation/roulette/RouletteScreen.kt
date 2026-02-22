package com.fukudai.meshiroulette.presentation.roulette

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.fukudai.meshiroulette.domain.model.Restaurant
import com.fukudai.meshiroulette.presentation.components.FilterBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouletteScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: RouletteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var displayedRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    var localSpinCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    LaunchedEffect(localSpinCount) {
        if (localSpinCount == 0) return@LaunchedEffect

        val candidates = uiState.candidateRestaurants
        var index = 0
        val endTime = System.currentTimeMillis() + 1500L

        // 高速フェーズ: 1.5秒間50ms間隔で店舗カードを順繰り更新
        while (System.currentTimeMillis() < endTime) {
            if (candidates.isNotEmpty()) {
                displayedRestaurant = candidates[index % candidates.size]
                index++
            }
            delay(50)
        }

        // 待機フェーズ: API結果を待機
        viewModel.uiState.first { !it.isSpinning }

        // 停止: 選択された店舗を表示
        displayedRestaurant = viewModel.uiState.value.selectedRestaurant
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
            SlotMachineDisplay(
                displayedRestaurant = displayedRestaurant,
                isSpinning = uiState.isSpinning,
                onDetailClick = onNavigateToDetail
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.selectedRestaurant != null && !uiState.isSpinning) {
                ResultCard(
                    restaurant = uiState.selectedRestaurant!!,
                    onDetailClick = { onNavigateToDetail(uiState.selectedRestaurant!!.id) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    localSpinCount++
                    viewModel.spinRoulette()
                },
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
private fun SlotMachineDisplay(
    displayedRestaurant: Restaurant?,
    isSpinning: Boolean,
    onDetailClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = displayedRestaurant,
            transitionSpec = {
                slideInVertically { it } togetherWith slideOutVertically { -it }
            },
            label = "slot_machine"
        ) { restaurant ->
            if (restaurant != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clickable(enabled = !isSpinning) { onDetailClick(restaurant.id) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = restaurant.imageUrl,
                            contentDescription = restaurant.name,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = restaurant.name,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = restaurant.genre.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!isSpinning) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "タップして詳細を見る →",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "ルーレットを回してください",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
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
