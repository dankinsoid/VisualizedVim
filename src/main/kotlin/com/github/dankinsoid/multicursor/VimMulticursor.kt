package com.github.dankinsoid.multicursor

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.util.Disposer
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.ui.ModalEntry
import com.maddyhome.idea.vim.ui.ex.ExEntryPanelService
import java.awt.Font
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import kotlin.math.absoluteValue

class VimMulticursor : VimExtension {
	override fun getName(): String = "multicursor"

	override fun init() {
		val highlightHandler = HighlightHandler()
		mapToFunctionAndProvideKeys("/") { MultiselectSearchHandler(highlightHandler, it) }
		mapToFunctionAndProvideKeys("f") { MultiselectFHandler(0, it) }
		mapToFunctionAndProvideKeys("F") { MultiselectFHandler(0, it) }
		mapToFunctionAndProvideKeys("t") { MultiselectFHandler(-1, it) }
		mapToFunctionAndProvideKeys("T") { MultiselectFHandler(-1, it) }
		mapToFunctionAndProvideKeys("w") { MultiselectHandler("(\\w+)", it) }
		mapToFunctionAndProvideKeys("W") { MultiselectHandler("(?<=\\s|\\A)[^\\s]+", it) }
		mapToFunctionAndProvideKeys("b") { MultiselectHandler("(?<=\\W|\\A)[^\\W]", it) }
		mapToFunctionAndProvideKeys("B") { MultiselectHandler("(?<=\\s|\\A)[^\\s]", it) }
		mapToFunctionAndProvideKeys("e") { MultiselectHandler("[^\\W](?=\\W|\\Z)", it) }
		mapToFunctionAndProvideKeys("E") { MultiselectHandler("[^\\s](?=\\s|\\Z)", it) }
		mapToFunctionAndProvideKeys("ge") { MultiselectHandler("[^\\W](?=\\W|\\Z)", it) }
		mapToFunctionAndProvideKeys("gE") { MultiselectHandler("[^\\s](?=\\s|\\Z)", it) }

		// Text object commands with explicit prefixes
		mapToFunctionAndProvideKeys("ab") { MultiselectTextObjectHandler("(", ")", false, it) }
		mapToFunctionAndProvideKeys("aB") { MultiselectTextObjectHandler("{", "}", false, it) }
		mapToFunctionAndProvideKeys("a(") { MultiselectTextObjectHandler("(", ")", false, it) }
		mapToFunctionAndProvideKeys("a{") { MultiselectTextObjectHandler("{", "}", false, it) }
		mapToFunctionAndProvideKeys("a[") { MultiselectTextObjectHandler("[", "]", false, it) }
		mapToFunctionAndProvideKeys("a<") { MultiselectTextObjectHandler("<", ">", false, it) }
		mapToFunctionAndProvideKeys("a\"") { MultiselectTextObjectHandler("\"", "\"", false, it) }
		mapToFunctionAndProvideKeys("a'") { MultiselectTextObjectHandler("'", "'", false, it) }
		mapToFunctionAndProvideKeys("a`") { MultiselectTextObjectHandler("`", "`", false, it) }
		
		// Inside versions
		mapToFunctionAndProvideKeys("ib") { MultiselectTextObjectHandler("(", ")", true, it) }
		mapToFunctionAndProvideKeys("iB") { MultiselectTextObjectHandler("{", "}", true, it) }
		mapToFunctionAndProvideKeys("i(") { MultiselectTextObjectHandler("(", ")", true, it) }
		mapToFunctionAndProvideKeys("i{") { MultiselectTextObjectHandler("{", "}", true, it) }
		mapToFunctionAndProvideKeys("i[") { MultiselectTextObjectHandler("[", "]", true, it) }
		mapToFunctionAndProvideKeys("i<") { MultiselectTextObjectHandler("<", ">", true, it) }
		mapToFunctionAndProvideKeys("i\"") { MultiselectTextObjectHandler("\"", "\"", true, it) }
		mapToFunctionAndProvideKeys("i'") { MultiselectTextObjectHandler("'", "'", true, it) }
		mapToFunctionAndProvideKeys("i`") { MultiselectTextObjectHandler("`", "`", true, it) }

		// Any brackets handlers
		mapToFunctionAndProvideKeys("ia") { MultiselectAnyTextObjectHandler(true, it) }
		mapToFunctionAndProvideKeys("aa") { MultiselectAnyTextObjectHandler(false, it) }

		mapToFunctionAndProvideKeys("c", "mc", MulticursorAddHandler(highlightHandler))
		mapToFunctionAndProvideKeys("r", "mc", MulticursorApplyHandler(highlightHandler))
		mapToFunctionAndProvideKeys("d", "mc", MulticursorRemoveHandler(highlightHandler))
		mapToFunctionAndProvideKeys("aw", "mc", MulticursorAroundWordHandler())
	}

	private class MulticursorAroundWordHandler : VimExtensionHandler {
		override fun execute(editor: Editor, context: DataContext) {
			val offset = editor.caretModel.primaryCaret.offset
			val text = editor.document.charsSequence
			
			// Find word boundaries
			var start = offset
			var end = offset
			
			// Find start of word
			while (start > 0 && text[start - 1].isLetterOrDigit()) {
				start--
			}
			
			// Find end of word
			while (end < text.length && text[end].isLetterOrDigit()) {
				end++
			}
			
			// Add carets at word boundaries
			editor.setCarets(sequenceOf(IntRange(start, start), IntRange(end, end)), false)
		}
	}

	private class MultiselectSearchHandler(
		private val highlightHandler: HighlightHandler,
		private val select: Boolean = false
	) : VimExtensionHandler {
		override fun execute(editor: Editor, context: DataContext) {
			val panel = ExEntryPanelService().createPanel(editor.vim, context.vim, "/", "")
			ModalEntry.activate(editor.vim) { key: KeyStroke ->
				return@activate when (key.keyCode) {
					KeyEvent.VK_ESCAPE -> {
						panel.deactivate(refocusOwningEditor = true, resetCaret = true)
						highlightHandler.clearAllMulticursorHighlighters(editor)
						false
					}
					KeyEvent.VK_ENTER -> {
						highlightHandler.clearAllMulticursorHighlighters(editor)
						panel.deactivate(refocusOwningEditor = false, resetCaret = true)
						select(editor, panel.actualText, select)
						false
					}
					else -> {
						panel.handleKey(key)
						highlightHandler.highlightMulticursorRange(editor, ranges(panel.actualText, editor))
						true
					}
				}
			}
			return
		}
	}

	private class MultiselectHandler(private val rexeg: String, private val select: Boolean = false): VimExtensionHandler {
		override fun execute(editor: Editor, context: DataContext) {
			select(editor, rexeg, select)
		}
	}

	private class MultiselectAnyTextObjectHandler(
		private val inside: Boolean = false,
		private val select: Boolean = false
	): VimExtensionHandler {
		override fun execute(editor: Editor, context: DataContext) {
			val offset = editor.caretModel.primaryCaret.offset
			val text = editor.document.charsSequence
			
			// Define all possible pairs
			val pairs = listOf(
				"(" to ")",
				"[" to "]",
				"{" to "}",
				"\"" to "\"",
				"'" to "'",
				"`" to "`"
			)
			
			// Find the closest pair
			var closestRange: Pair<IntRange, IntRange>? = null
			var minDistance = Int.MAX_VALUE
			
			for ((openDelim, closeDelim) in pairs) {
				val range = findPairedRange(text, offset, openDelim, closeDelim)
				if (range != null) {
					val distance = minOf(
						(range.first.first - offset).absoluteValue,
						(range.first.last - offset).absoluteValue,
						(range.second.first - offset).absoluteValue,
						(range.second.last - offset).absoluteValue
					)
					if (distance < minDistance) {
						minDistance = distance
						closestRange = range
					}
				}
			}
			
			if (closestRange != null) {
				var (start, end) = closestRange
				if (inside) {
					// For "i" commands, move start range after the opening delimiter
					start = IntRange(start.first + 1, start.last + 1)
				} else {
					// For "a" commands, extend end range to include the closing delimiter
					end = IntRange(end.first + 1, end.last + 1)
				}
				editor.setCarets(sequenceOf(start, end), select)
			}
		}
	}

	private class MultiselectTextObjectHandler(
		private val startDelimiter: String,
		private val endDelimiter: String,
		private  val inside: Boolean = false,
		private val select: Boolean = false
	): VimExtensionHandler {
		override fun execute(editor: Editor, context: DataContext) {
			val offset = editor.caretModel.primaryCaret.offset
			val text = editor.document.charsSequence
			val ranges = findPairedRange(text, offset, startDelimiter, endDelimiter)
			if (ranges != null) {
				var (start, end) = ranges
				if (inside) {
					// For "i" commands, move start range after the opening delimiter
					start = IntRange(start.first + startDelimiter.length, start.last + startDelimiter.length)
				} else {
					// For "a" commands, extend end range to include the closing delimiter
					end = IntRange(end.first + endDelimiter.length, end.last + endDelimiter.length)
				}
				editor.setCarets(sequenceOf(start, end), select)
			}
		}
	}

	private class MultiselectFHandler(private val offset: Int = 0, private val select: Boolean = false): VimExtensionHandler {
		override fun execute(editor: Editor, context: DataContext) {
			val char = getChar(editor) ?: return
			val text = editor.document.charsSequence
			val selections = editor.selections()
			val ranges = mutableListOf<IntRange>()
			
			if (selections.count() > 0) {
				// If there are selections, only search within them
				for (selection in selections) {
					var pos = selection.first
					while (pos <= selection.last) {
						if (text[pos] == char) {
							val rangeStart = pos + offset
							if (rangeStart >= selection.first && rangeStart <= selection.last) {
								ranges.add(IntRange(rangeStart, rangeStart))
							}
						}
						pos++
					}
				}
			} else {
				// No selections, search in entire document
				val caretOffset = editor.caretModel.primaryCaret.offset
				
				// Search forward from caret
				var pos = caretOffset
				while (pos < text.length) {
					if (text[pos] == char) {
						val rangeStart = pos + offset
						ranges.add(IntRange(rangeStart, rangeStart))
					}
					pos++
				}
				
				// Search backward from caret
				pos = caretOffset - 1
				while (pos >= 0) {
					if (text[pos] == char) {
						val rangeStart = pos + offset
						ranges.add(IntRange(rangeStart, rangeStart))
					}
					pos--
				}
			}
			
			if (ranges.isNotEmpty()) {
				editor.setCarets(ranges.asSequence(), select)
			}
		}

		private fun getChar(editor: Editor): Char? {
			val key = VimExtensionFacade.inputKeyStroke(editor)
			return when {
				key.keyChar == KeyEvent.CHAR_UNDEFINED || key.keyCode == KeyEvent.VK_ESCAPE -> null
				else -> key.keyChar
			}
		}
	}

	private class MulticursorAddHandler(private val highlighter: HighlightHandler): VimExtensionHandler {
		override fun execute(editor: Editor, context: DataContext) {
			val offset = editor.caretModel.primaryCaret.offset
			val range = IntRange(offset, offset)
			if (selectedCarets.contains(range)) {
				selectedCarets.remove(range)
				highlighter.clearSingleRange(editor, range)
			} else {
				selectedCarets.add(range)
				highlighter.highlightSingleRange(editor, range)
			}
		}
	}

	private class MulticursorRemoveHandler(private val highlighter: HighlightHandler): VimExtensionHandler {
		override fun execute(editor: Editor, context: DataContext) {
			selectedCarets.clear()
			highlighter.clearAllMulticursorHighlighters(editor)
		}
	}

	private class MulticursorApplyHandler(private val highlighter: HighlightHandler): VimExtensionHandler {
		override fun execute(editor: Editor, context: DataContext) {
			highlighter.clearAllMulticursorHighlighters(editor)
			val offset = editor.caretModel.primaryCaret.offset
			val range = IntRange(offset, offset)
			if (!selectedCarets.contains(range)) {
				selectedCarets.add(0, range)
			}
			editor.setCarets(selectedCarets.asSequence(), false)
			selectedCarets.clear()
		}
	}

	companion object {

		private val selectedCarets: MutableList<IntRange> = mutableListOf()

		private fun select(editor: Editor, regex: String, select: Boolean = true, offset: Int = 0) {
			editor.setCarets(ranges(regex, editor), select, offset)
		}

		private fun ranges(text: String, editor: Editor): Sequence<IntRange> {
			val selections = editor.selections()
			return if (selections.count() > 0) {
				val start = selections.minOf { it.first }
				val chars = editor.document.charsSequence.subSequence(start, selections.maxOf { it.last })
				val ranges = text.toRegex().findAll(chars).map { IntRange(it.range.first + start, it.range.last + start) }
				return ranges.intersectionsWith(selections)
			} else {
				text.toRegex().findAll(editor.document.charsSequence).map { it.range }
			}
		}


		private fun findPairedRange(text: CharSequence, offset: Int, start: String, end: String): Pair<IntRange, IntRange>? {
			// First try searching forward from cursor
			val forwardEnd = findClosingPosition(text, offset, start, end)
			if (forwardEnd != null) {
				val forwardStart = findOpeningPosition(text, forwardEnd, start, end)
				if (forwardStart != null) {
					return Pair(
						IntRange(forwardStart, forwardStart + start.length - 1),
						IntRange(forwardEnd, forwardEnd + end.length - 1)
					)
				}
			}
			return null
		}

		private fun findClosingPosition(
			text: CharSequence,
			fromOffset: Int,
			start: String,
			end: String,
			maxOffset: Int = text.length
		): Int? {
			var nesting = 0
			var pos = fromOffset
			while (pos < maxOffset) {
				when {
					text.matchesAt(pos, start) -> {
						nesting++
						pos += start.length
					}
					text.matchesAt(pos, end) -> {
						if (nesting == 0) return pos
						nesting--
						pos += end.length
					}
					else -> pos++
				}
			}
			return null
		}

		private fun findOpeningPosition(text: CharSequence, fromOffset: Int, start: String, end: String): Int? {
			var nesting = 0
			var pos = fromOffset
			while (pos >= start.length - 1) {
				when {
					pos >= end.length && text.matchesAt(pos - end.length, end) -> {
						nesting++
						pos -= end.length
					}
					pos >= start.length && text.matchesAt(pos - start.length, start) -> {
						if (nesting == 0) return pos - start.length
						nesting--
						pos -= start.length
					}
					else -> pos--
				}
			}
			return null
		}
	}

	private class HighlightHandler {
		private val sneakHighlighters: MutableSet<RangeHighlighter> = mutableSetOf()

		fun highlightMulticursorRange(editor: Editor, ranges: Sequence<IntRange>) {
			clearAllMulticursorHighlighters(editor)

			val project = editor.project
			if (project != null) {
				Disposer.register(ProjectService.getInstance(project)) {
					sneakHighlighters.clear()
				}
			}

			if (ranges.count() > 0) {
				for (i in 0 until ranges.count()) {
					highlightSingleRange(editor, ranges.elementAt(i))
				}
			}
		}

		fun clearAllMulticursorHighlighters(editor: Editor) {
			sneakHighlighters.forEach { highlighter ->
				editor.markupModel.removeHighlighter(highlighter)
			}
			sneakHighlighters.clear()
		}

		fun highlightSingleRange(editor: Editor, range: IntRange) {
			val highlighter = editor.markupModel.addRangeHighlighter(
				range.first,
				range.last + 1,
				HighlighterLayer.SELECTION,
				getHighlightTextAttributes(editor),
				HighlighterTargetArea.EXACT_RANGE
			)
			sneakHighlighters.add(highlighter)
		}

		fun clearSingleRange(editor: Editor, range: IntRange) {
				sneakHighlighters.first { it.startOffset == range.first }.let {
					editor.markupModel.removeHighlighter(it)
					sneakHighlighters.remove(it)
				}
		}

		private fun getHighlightTextAttributes(editor: Editor) = TextAttributes(
			null,
			EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES.defaultAttributes.backgroundColor,
			editor.colorsScheme.getColor(EditorColors.CARET_COLOR),
			EffectType.SEARCH_MATCH,
			Font.PLAIN
		)
	}
}

private fun CharSequence.matchesAt(index: Int, str: String): Boolean {
	if (index + str.length > length) return false
	for (i in str.indices) {
		if (this[index + i] != str[i]) return false
	}
	return true
}
