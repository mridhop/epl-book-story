package ua.acclorite.book_story.domain.repository

import ua.acclorite.book_story.data.remote.dto.LatestReleaseInfo

interface ApiManagementRepository {
    suspend fun checkForUpdates(postNotification: Boolean): LatestReleaseInfo?
}