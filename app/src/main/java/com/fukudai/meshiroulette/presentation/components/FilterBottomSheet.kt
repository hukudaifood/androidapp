package com.fukudai.meshiroulette.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fukudai.meshiroulette.domain.model.Genre
import com.fukudai.meshiroulette.domain.model.PriceRange

private val ChipSelectedContainer = Color(0xFFFF8000)
private val ChipSelectedLabel = Color.White
private val ChipUnselectedContainer = Color(0xFFF3E0CC)
private val ChipUnselectedLabel = Color(0xFF775A40)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    selectedGenres: Set<Genre>,
    selectedPriceRange: PriceRange,
    isOpenNowOnly: Boolean,
    onGenreToggled: (Genre) -> Unit,
    onPriceRangeSelected: (PriceRange) -> Unit,
    onOpenNowOnlyChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "ジャンル",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val allSelected = selectedGenres.isEmpty()
                FilterChip(
                    selected = allSelected,
                    onClick = { onGenreToggled(Genre.ALL) },
                    label = { Text("すべて") },
                    colors = chipColors(allSelected),
                    border = chipBorder(allSelected)
                )
                Genre.entries.filter { it != Genre.ALL }.forEach { genre ->
                    val selected = genre in selectedGenres
                    FilterChip(
                        selected = selected,
                        onClick = { onGenreToggled(genre) },
                        label = { Text(genre.displayName) },
                        colors = chipColors(selected),
                        border = chipBorder(selected)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "価格帯",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PriceRange.entries.forEach { priceRange ->
                    val selected = priceRange == selectedPriceRange
                    FilterChip(
                        selected = selected,
                        onClick = { onPriceRangeSelected(priceRange) },
                        label = { Text(priceRange.displayName) },
                        colors = chipColors(selected),
                        border = chipBorder(selected)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "営業状況",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            FilterChip(
                selected = isOpenNowOnly,
                onClick = { onOpenNowOnlyChanged(!isOpenNowOnly) },
                label = { Text("営業中のみ") },
                colors = chipColors(isOpenNowOnly),
                border = chipBorder(isOpenNowOnly)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun chipColors(selected: Boolean) = FilterChipDefaults.filterChipColors(
    containerColor = ChipUnselectedContainer,
    labelColor = ChipUnselectedLabel,
    selectedContainerColor = ChipSelectedContainer,
    selectedLabelColor = ChipSelectedLabel
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun chipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = Color.Transparent,
    selectedBorderColor = Color.Transparent
)
