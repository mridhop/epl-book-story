package ua.acclorite.book_story.domain.repository

interface FavoriteDirectoryManagementRepository {
    suspend fun updateFavoriteDirectory(path: String)
}