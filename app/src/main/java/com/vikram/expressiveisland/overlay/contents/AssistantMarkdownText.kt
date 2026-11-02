package com.vikram.expressiveisland.overlay.contents

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * Lightweight Markdown renderer for assistant output. Supports paragraphs, headings, lists,
 * bold/italic/strikethrough and inline code without exposing Markdown delimiters to the user.
 */
@Composable
internal fun AssistantMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val bodyStyle = MaterialTheme.typography.bodyMedium
    val rendered = buildAnnotatedString {
        markdown.replace("\\r\\n", "\\n").lines().forEachIndexed { index, rawLine ->
            val line = rawLine.trimEnd()
            val trimmed = line.trimStart()
            val heading = trimmed.takeWhile { it == '#' }.length.takeIf { it in 1..6 && trimmed.getOrNull(it) == ' ' }
            val content = if (heading != null) trimmed.drop(heading + 1) else trimmed
            val listLine = content.startsWith("- ") || content.startsWith("* ") || Regex("^\\d+[.)] ").containsMatchIn(content)
            if (index > 0) append("\\n")
            if (heading != null) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = (bodyStyle.fontSize.value + (7 - heading) * 1.5f).sp)) {
                    appendInlineMarkdown(content, color)
                }
            } else {
                val item = when {
                    content.startsWith("- ") || content.startsWith("* ") -> "• " + content.drop(2)
                    Regex("^\\d+[.)] ").containsMatchIn(content) -> content
                    else -> content
                }
                if (listLine) append("  ")
                appendInlineMarkdown(item, color)
            }
        }
    }
    Text(text = rendered, modifier = modifier, style = bodyStyle, color = color)
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInlineMarkdown(
    source: String,
    color: Color,
) {
    val token = Regex("(\\*\\*|__)(.+?)\\1|(?<!\\*)\\*([^*]+?)\\*|(?<!_)_([^_]+?)_|~~(.+?)~~|`([^`]+?)`")
    var cursor = 0
    token.findAll(source).forEach { match ->
        append(source.substring(cursor, match.range.first))
        val bold = match.groups[2]?.value
        val italic = match.groups[3]?.value ?: match.groups[4]?.value
        val strike = match.groups[5]?.value
        val code = match.groups[6]?.value
        when {
            bold != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) { append(bold) }
            italic != null -> withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = color)) { append(italic) }
            strike != null -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = color)) { append(strike) }
            code != null -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = color.copy(alpha = 0.12f), color = color)) { append(code) }
        }
        cursor = match.range.last + 1
    }
    append(source.substring(cursor))
}
