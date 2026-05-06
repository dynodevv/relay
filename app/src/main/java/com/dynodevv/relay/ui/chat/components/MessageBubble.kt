package com.dynodevv.relay.ui.chat.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mikepenz.markdown.compose.LocalMarkdownColors
import com.mikepenz.markdown.compose.LocalMarkdownDimens
import com.mikepenz.markdown.compose.LocalMarkdownPadding
import com.mikepenz.markdown.compose.LocalMarkdownTypography
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownCodeBackground
import com.mikepenz.markdown.compose.elements.MarkdownCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.dynodevv.relay.domain.model.Message
import com.dynodevv.relay.domain.model.MessageRole
import com.dynodevv.relay.ui.theme.GoogleSansCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.io.File
import java.util.Locale

@Composable
fun MessageBubble(
    message: Message,
    onDelete: () -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit = {}
) {
    val isUser = message.role is MessageRole.User
    val clipboardManager = LocalClipboardManager.current
    val density = LocalDensity.current
    val maxWidth = LocalConfiguration.current.screenWidthDp.dp * 0.85f
    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = maxWidth),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { offset ->
                        menuOffset = with(density) {
                            DpOffset(offset.x.toDp(), offset.y.toDp())
                        }
                        showMenu = true
                    })
                }
            ) {
                Surface(
                    modifier = if (message.isStreaming) Modifier else Modifier.animateContentSize(),
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    ),
                    color = if (isUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    tonalElevation = if (isUser) 0.dp else 2.dp
                ) {
                    Column {
                        Box(modifier = Modifier.padding(14.dp)) {
                            if (isUser) {
                                Column {
                                    if (message.imageUris.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            message.imageUris.forEach { path ->
                                                AsyncImage(
                                                    model = File(path),
                                                    contentDescription = "Attached image",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(200.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    Text(
                                        text = message.content,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            } else {
                                if (message.isStreaming && message.content.isEmpty()) {
                                    ThinkingDots()
                                } else if (message.isStreaming) {
                                    Text(
                                        text = message.content,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Markdown(
                                        content = message.content,
                                        modifier = Modifier,
                                        colors = markdownColor(
                                            text = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        typography = markdownTypography(
                                            code = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = GoogleSansCode
                                            ),
                                            inlineCode = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = GoogleSansCode
                                            )
                                        ),
                                        components = markdownComponents(
                                            codeBlock = {
                                                MarkdownCodeBlock(it.content, it.node) { code, _ ->
                                                    CodeBlockWithSyntaxHighlight(code)
                                                }
                                            },
                                            codeFence = {
                                                MarkdownCodeFence(it.content, it.node) { code, _ ->
                                                    CodeBlockWithSyntaxHighlight(code)
                                                }
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    offset = menuOffset
                ) {
                    DropdownMenuItem(
                        text = { Text("Copy") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.content))
                            showMenu = false
                        }
                    )
                    if (isUser) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                onEdit()
                                showMenu = false
                            }
                        )
                    }
                    if (!isUser) {
                        DropdownMenuItem(
                            text = { Text("Regenerate") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            onClick = {
                                onRegenerate()
                                showMenu = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }

            Text(
                text = timeFormat.format(Date(message.createdAt)),
                style = MaterialTheme.typography.labelSmall,
                color = if (isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ThinkingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "dot1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse), label = "dot2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse), label = "dot3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = "Thinking",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = ".",
            modifier = Modifier.alpha(dot1),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = ".",
            modifier = Modifier.alpha(dot2),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = ".",
            modifier = Modifier.alpha(dot3),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun CodeBlockWithSyntaxHighlight(code: String) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var showCopied by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        MarkdownCodeBackground(
            color = LocalMarkdownColors.current.codeBackground,
            shape = RoundedCornerShape(LocalMarkdownDimens.current.codeBackgroundCornerSize),
            modifier = Modifier.fillMaxWidth()
        ) {
            SyntaxHighlightedCode(
                code = code,
                style = LocalMarkdownTypography.current.code,
                color = LocalMarkdownColors.current.codeText
            )
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
        ) {
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(code))
                    showCopied = true
                    scope.launch { delay(2000); showCopied = false }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (showCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = if (showCopied) "Copied" else "Copy code",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SyntaxHighlightedCode(
    code: String,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color
) {
    val keywords = setOf(
        "fun", "val", "var", "class", "interface", "object", "data", "sealed", "open",
        "abstract", "override", "suspend", "return", "if", "else", "when", "for", "while",
        "import", "package", "private", "public", "internal", "protected", "const",
        "lateinit", "by", "in", "is", "as", "try", "catch", "finally", "throw", "true",
        "false", "null", "this", "super", "enum", "companion", "operator", "inline",
        "noinline", "crossinline", "reified", "expect", "actual", "typealias", "where",
        "def", "if", "else", "for", "while", "return", "from", "as",
        "except", "raise", "lambda", "with", "yield", "async", "await",
        "function", "let", "const", "export", "default", "class", "extends", "new",
        "typeof", "instanceof", "undefined"
    )
    val types = setOf(
        "Int", "Long", "Float", "Double", "Boolean", "String", "Char", "Byte", "Short",
        "Unit", "Nothing", "Any", "List", "Map", "Set", "Array", "Sequence", "Flow",
        "MutableList", "MutableMap", "MutableSet"
    )

    val annotated = buildAnnotatedString {
        val tokens = code.split(Regex("(?=[^a-zA-Z0-9_])|(?<=[^a-zA-Z0-9_])"))
        tokens.forEach { token ->
            when {
                token in keywords -> withStyle(
                    SpanStyle(color = androidx.compose.ui.graphics.Color(0xFFCF8E6D), fontFamily = GoogleSansCode)
                ) { append(token) }
                token in types -> withStyle(
                    SpanStyle(color = androidx.compose.ui.graphics.Color(0xFFBCA5C4), fontFamily = GoogleSansCode)
                ) { append(token) }
                token.startsWith("\"") || token.startsWith("'") || token.startsWith("`") -> withStyle(
                    SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF6AAB73), fontFamily = GoogleSansCode)
                ) { append(token) }
                token.toDoubleOrNull() != null || token.matches(Regex("^0[xX][0-9a-fA-F]+")) -> withStyle(
                    SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF2AACB4), fontFamily = GoogleSansCode)
                ) { append(token) }
                token.startsWith("//") || token.startsWith("/*") || token.startsWith("*") -> withStyle(
                    SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF6A8759), fontFamily = GoogleSansCode)
                ) { append(token) }
                else -> withStyle(SpanStyle(color = color, fontFamily = GoogleSansCode)) { append(token) }
            }
        }
    }

    Text(
        text = annotated,
        style = style.copy(fontFamily = GoogleSansCode),
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(LocalMarkdownPadding.current.codeBlock)
    )
}
