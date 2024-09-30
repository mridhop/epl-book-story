package ua.acclorite.book_story.domain.repository

import ua.acclorite.book_story.domain.model.NullableBook
import ua.acclorite.book_story.domain.model.SelectableFile
import ua.acclorite.book_story.domain.model.ChapterWithText
import ua.acclorite.book_story.domain.util.Resource
import java.io.File

interface FileSystemManagementRepository {
    suspend fun getFilesFromDevice(query: String = ""): List<SelectableFile>
    suspend fun getBookFromFile(file: File): NullableBook
    suspend fun parseText(file: File): Resource<List<ChapterWithText>>
}