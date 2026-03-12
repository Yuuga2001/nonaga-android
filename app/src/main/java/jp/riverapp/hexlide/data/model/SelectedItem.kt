package jp.riverapp.hexlide.data.model

sealed class SelectedItem {
    data class PieceItem(val pieceId: String) : SelectedItem()
    data class TileIndexItem(val index: Int) : SelectedItem()
}
