package ua.acclorite.book_story.data.parser.fb2

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import ua.acclorite.book_story.R
import ua.acclorite.book_story.data.parser.TextParser
import ua.acclorite.book_story.domain.model.ChapterWithText
import ua.acclorite.book_story.domain.util.Resource
import ua.acclorite.book_story.domain.util.UIText
import ua.acclorite.book_story.presentation.core.constants.Constants
import java.io.File
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

private const val FB2_TAG = "FB2 Parser"

class Fb2TextParser @Inject constructor() : TextParser {

    override suspend fun parse(file: File): Resource<List<ChapterWithText>> {
        Log.i(FB2_TAG, "Started FB2 parsing: ${file.name}.")

        return try {
            val document = parseDocument(file)
            val bodyNode = getBodyNode(document) ?: return Resource.Error(UIText.StringResource(R.string.error_file_empty))

            val unformattedLines = extractUnformattedLines(bodyNode)
            val formattedLines = formatLines(unformattedLines)

            if (formattedLines.isEmpty()) {
                return Resource.Error(UIText.StringResource(R.string.error_file_empty))
            }

            Log.i(FB2_TAG, "Successfully finished FB2 parsing.")
            Resource.Success(listOf(ChapterWithText(chapter = Constants.EMPTY_CHAPTER, text = formattedLines)))
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(UIText.StringResource(R.string.error_query, e.message?.take(40)?.trim() ?: ""))
        }
    }

    private suspend fun parseDocument(file: File) = withContext(Dispatchers.IO) {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        builder.parse(file)
    }

    private fun getBodyNode(document: org.w3c.dom.Document): Element? {
        val bodyNodes = document.getElementsByTagName("body")
        return if (bodyNodes.length > 0) bodyNodes.item(0) as Element else null
    }

    private suspend fun extractUnformattedLines(bodyNode: Element): List<String> {
        val unformattedLines = mutableListOf<String>()
        val paragraphNodes = bodyNode.getElementsByTagName("p")

        for (element in paragraphNodes.asList()) {
            yield()
            if (element.textContent.isNotBlank()) {
                unformattedLines.add(element.textContent.trim())
            }
        }
        return unformattedLines
    }

    private suspend fun formatLines(unformattedLines: List<String>): List<String> {
        val lines = mutableListOf<String>()
        unformattedLines.forEachIndexed { index, string ->
            yield()
            val line = string.trim()
            if (index == 0 || line.first().isUpperCase() || line.first().isDigit()) {
                lines.add(line)
            } else if (line.first().isLowerCase() || line.first().isLetter()) {
                val currentLine = lines[lines.lastIndex]
                if (currentLine.last() == '-' && currentLine[currentLine.lastIndex - 1].isLowerCase()) {
                    lines[lines.lastIndex] = currentLine.dropLast(1) + line
                } else {
                    lines[lines.lastIndex] += " $line"
                }
            }
        }
        return lines.map { it.trim() }
    }

    private fun NodeList.asList(): List<Element> {
        val list = mutableListOf<Element>()
        for (i in 0 until this.length) {
            list.add(this.item(i) as Element)
        }
        return list
    }
}