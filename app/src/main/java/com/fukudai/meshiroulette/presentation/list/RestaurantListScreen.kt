package com.fukudai.meshiroulette.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fukudai.meshiroulette.domain.model.Genre
import com.fukudai.meshiroulette.presentation.components.ErrorContent
import com.fukudai.meshiroulette.presentation.components.FilterBottomSheet
import com.fukudai.meshiroulette.presentation.components.LoadingContent
import com.fukudai.meshiroulette.presentation.components.RestaurantCard

private val genreChips = listOf(
    Genre.TEISHOKU, Genre.CHINESE, Genre.RAMEN, Genre.CURRY,
    Genre.UDON, Genre.GYUDON, Genre.TONKATSU, Genre.YAKINIKU,
    Genre.IZAKAYA, Genre.CAFE, Genre.OTHER
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantListScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: RestaurantListViewModel = hiltViewModel()
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
                title = { Text("お店一覧") },
                actions = {
                    IconButton(onClick = { viewModel.showFilterSheet() }) {
                        Icon(Icons.Default.FilterList, contentDescription = "フィルター")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.restaurants.isEmpty() -> {
                LoadingContent(modifier = Modifier.padding(paddingValues))
            }
            uiState.error != null && uiState.restaurants.isEmpty() -> {
                ErrorContent(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadRestaurants() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    CategoryFilterRow(
                        selectedGenre = uiState.selectedGenre,
                        isOpenNowOnly = uiState.isOpenNowOnly,
                        onAllSelected = {
                            viewModel.setGenre(Genre.ALL)
                            viewModel.setOpenNowOnly(false)
                        },
                        onOpenNowToggle = { viewModel.setOpenNowOnly(!uiState.isOpenNowOnly) },
                        onGenreSelected = { viewModel.setGenre(it) }
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.restaurants,
                            key = { it.id }
                        ) { restaurant ->
                            RestaurantCard(
                                restaurant = restaurant,
                                onClick = { onNavigateToDetail(restaurant.id) }
                            )
                        }
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterRow(
    selectedGenre: Genre,
    isOpenNowOnly: Boolean,
    onAllSelected: () -> Unit,
    onOpenNowToggle: () -> Unit,
    onGenreSelected: (Genre) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedGenre == Genre.ALL && !isOpenNowOnly,
                onClick = onAllSelected,
                label = { Text("すべて") }
            )
        }
        item {
            FilterChip(
                selected = isOpenNowOnly,
                onClick = onOpenNowToggle,
                label = { Text("営業中") }
            )
        }
        items(genreChips) { genre ->
            FilterChip(
                selected = selectedGenre == genre,
                onClick = { onGenreSelected(genre) },
                label = { Text(genre.displayName) }
            )
        }
    }
}
