package ua.acclorite.book_story.domain.repository

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import ua.acclorite.book_story.data.remote.dto.ReleaseResponse
import ua.acclorite.book_story.domain.model.Book
import ua.acclorite.book_story.domain.model.History
import ua.acclorite.book_story.domain.model.NullableBook
import ua.acclorite.book_story.domain.model.StringWithId
import ua.acclorite.book_story.domain.util.CoverImage
import ua.acclorite.book_story.domain.util.Resource
import ua.acclorite.book_story.presentation.data.MainState
import java.io.File

interface BookRepository {

    suspend fun getBooks(
        query: String
    ): List<Book>

    suspend fun getBooksById(
        ids: List<Int>
    ): List<Book>

    suspend fun getBookTextById(
        textPath: String
    ): List<StringWithId>

    suspend fun insertBooks(
        books: List<Pair<Book, CoverImage?>>
    ): Boolean

    suspend fun updateBooks(
        books: List<Book>
    )

    suspend fun updateBooksWithText(
        books: List<Book>
    ): Boolean

    suspend fun updateCoverImageOfBook(
        bookWithOldCover: Book,
        newCoverImage: CoverImage?
    )

    suspend fun deleteBooks(
        books: List<Book>
    )

    suspend fun <T> retrieveDataFromDataStore(
        key: Preferences.Key<T>,
        defaultValue: T
    ): Flow<T>

    suspend fun <T> putDataToDataStore(
        key: Preferences.Key<T>,
        value: T
    )

    suspend fun getAllSettings(scope: CoroutineScope): MainState

    suspend fun getFilesFromDevice(query: String = ""): Flow<Resource<List<File>>>

    suspend fun getBooksFromFiles(files: List<File>): List<NullableBook>

    suspend fun insertHistory(history: List<History>)

    suspend fun getHistory(): Flow<Resource<List<History>>>

    suspend fun getLatestBookHistory(bookId: Int): History?

    suspend fun deleteWholeHistory()

    suspend fun deleteBookHistory(bookId: Int)

    suspend fun deleteHistory(
        history: List<History>
    )

    suspend fun checkForUpdates(postNotification: Boolean): ReleaseResponse?
}