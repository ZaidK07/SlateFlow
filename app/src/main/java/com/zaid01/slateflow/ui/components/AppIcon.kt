package com.zaid01.slateflow.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.zaid01.slateflow.R

enum class AppSymbol(val resource: Int) {
    DASHBOARD(R.drawable.ic_dashboard), FOLDER(R.drawable.ic_folder),
    DOCUMENT(R.drawable.ic_document), SETTINGS(R.drawable.ic_settings),
    BACK(R.drawable.ic_back), ADD(R.drawable.ic_add),
    TUNE(R.drawable.ic_tune), GRID(R.drawable.ic_grid), LIST(R.drawable.ic_list),
    REFRESH(R.drawable.ic_refresh), MORE(R.drawable.ic_more), SEARCH(R.drawable.ic_search),
    CHEVRON(R.drawable.ic_chevron), PLAY(R.drawable.ic_play),
}

@Composable
fun AppIcon(symbol: AppSymbol, description: String? = null, modifier: Modifier = Modifier.size(24.dp)) {
    Icon(painterResource(symbol.resource), contentDescription = description, modifier = modifier)
}
