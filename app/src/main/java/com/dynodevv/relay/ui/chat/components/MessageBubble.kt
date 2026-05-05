package com.dynodevv.relay.ui.chat.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
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

@Composable
fun MessageBubble(
    message: Message,
    onDelete: () -> Unit,
    onRegenerate: () -> Unit
) {
    val isUser = message.role is MessageRole.User
    val clipboardManager = LocalClipboardManager.current
    var showActions by remember { mutableStateOf(false) }
    val maxWidth = LocalConfiguration.current.screenWidthDp.dp * 0.85f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .then(if (message.isStreaming) Modifier else Modifier.animateContentSize()),
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
            tonalElevation = if (isUser) 0.dp else 2.dp,
            onClick = { showActions = !showActions }
        ) {
            Column {
                Box(modifier = Modifier.padding(14.dp)) {
                    if (isUser) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        if (message.isStreaming) {
                            if (message.content.isEmpty()) {
                                Text(
                                    text = "Thinking\u2026",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            } else {
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                                            CodeBlockWithCopy(code)
                                        }
                                    },
                                    codeFence = {
                                        MarkdownCodeFence(it.content, it.node) { code, _ ->
                                            CodeBlockWithCopy(code)
                                        }
                                    }
                                )
                            )
                        }
                    }
                }

                if (showActions) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(message.content))
                        }) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (!isUser) {
                            IconButton(onClick = onRegenerate) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Regenerate",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockWithCopy(code: String) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var showCopied by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        MarkdownCodeBackground(
            color = LocalMarkdownColors.current.codeBackground,
            shape = RoundedCornerShape(LocalMarkdownDimens.current.codeBackgroundCornerSize),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = code,
                style = LocalMarkdownTypography.current.code,
                color = LocalMarkdownColors.current.codeText,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(LocalMarkdownPadding.current.codeBlock)
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
