package ua.acclorite.book_story.domain.repository

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import ua.acclorite.book_story.data.local.dto.BookEntity
import ua.acclorite.book_story.domain.model.Book
import ua.acclorite.book_story.domain.model.History
import ua.acclorite.book_story.domain.model.NullableBook
import ua.acclorite.book_story.util.CoverImage
import ua.acclorite.book_story.util.Resource
import java.io.File

interface BookRepository {

    suspend fun getBooks(
        query: String
    ): Flow<Resource<List<Book>>>

    suspend fun fastGetBooks(
        query: String
    ): List<Book>

    suspend fun getBooksById(
        ids: List<Int>
    ): List<Book>

    suspend fun getBookTextById(
        bookId: Int
    ): String

    suspend fun findBook(
        id: Int
    ): BookEntity

    suspend fun insertBooks(
        books: List<Pair<Book, CoverImage?>>
    )

    suspend fun updateBooks(
        books: List<Book>
    )

    suspend fun updateBooksWithText(
        books: List<Book>
    )

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

    suspend fun getFilesFromDevice(query: String = ""): Flow<Resource<List<File>>>

    suspend fun getBooksFromFiles(files: List<File>): List<NullableBook>

    suspend fun insertHistory(history: List<History>)

    suspend fun getHistory(): Flow<Resource<List<History>>>

    suspend fun deleteWholeHistory()

    suspend fun deleteBookHistory(bookId: Int)

    suspend fun deleteHistory(
        history: List<History>
    )
}