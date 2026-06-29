package off.kys.openarcade.ui.detail

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import off.kys.openarcade.domain.model.GameEntry

data class GameDetailUiState(
    val game: GameEntry? = null,
    val showCategoryDialog: Boolean = false,
    val editingCategories: ImmutableList<String> = persistentListOf(),
    val newCategoryDraft: String = "",
    val showScrollbar: Boolean = true,
    val hapticFeedback: Boolean = true
)
