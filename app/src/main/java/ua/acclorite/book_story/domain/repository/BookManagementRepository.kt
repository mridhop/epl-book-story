package ua.acclorite.book_story.domain.repository

import ua.acclorite.book_story.domain.model.Book
import ua.acclorite.book_story.domain.model.BookWithText
import ua.acclorite.book_story.domain.model.BookWithTextAndCover
import ua.acclorite.book_story.domain.util.CoverImage

interface BookManagementRepository {
    suspend fun getBooks(query: String): List<Book>
    suspend fun getBooksById(ids: List<Int>): List<Book>
    suspend fun getBookText(textPath: String): List<String>
    suspend fun insertBook(bookWithTextAndCover: BookWithTextAndCover): Boolean
    suspend fun updateBook(book: Book)
    suspend fun updateBookWithText(bookWithText: BookWithText): Boolean
    suspend fun updateCoverImageOfBook(bookWithOldCover: Book, newCoverImage: CoverImage?)
    suspend fun deleteBooks(books: List<Book>)
    suspend fun canResetCover(bookId: Int): Boolean
    suspend fun resetCoverImage(bookId: Int): Boolean
}