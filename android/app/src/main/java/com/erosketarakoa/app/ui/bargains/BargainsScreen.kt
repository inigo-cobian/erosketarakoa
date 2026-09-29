package com.erosketarakoa.app.ui.bargains

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erosketarakoa.app.data.Money
import com.erosketarakoa.app.data.bargain.BargainKind
import com.erosketarakoa.app.data.bargain.BargainReason
import com.erosketarakoa.app.data.bargain.ItemBargains

const val BARGAINS_SCREEN_TAG = "bargains_screen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BargainsScreen(
    onBack: () -> Unit,
    viewModel: BargainsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today's bargains") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(BARGAINS_SCREEN_TAG),
        ) {
            if (!state.isLoading && state.bargains.isEmpty()) {
                EmptyBargains()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.bargains, key = { it.itemId }) { bargain ->
                        BargainRow(bargain)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun BargainRow(bargain: ItemBargains) {
    ListItem(
        headlineContent = { Text(bargain.itemName) },
        supportingContent = {
            Column {
                bargain.reasons.forEach { reason ->
                    Text(reasonText(reason), style = MaterialTheme.typography.labelMedium)
                }
            }
        },
    )
}

private fun reasonText(reason: BargainReason): String {
    val price = "€${Money.centsToEuros(reason.priceCents)}"
    return when (reason.kind) {
        BargainKind.BELOW_TARGET -> "At/below target at ${reason.store}: $price"
        BargainKind.PRICE_DROP -> "Price drop at ${reason.store}: $price"
        BargainKind.CHEAPEST_MARKET -> "Cheapest at ${reason.store}: $price"
    }
}

@Composable
private fun EmptyBargains() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "No bargains today.\nLink items to stores and track prices to see deals here.",
            textAlign = TextAlign.Center,
        )
    }
}
