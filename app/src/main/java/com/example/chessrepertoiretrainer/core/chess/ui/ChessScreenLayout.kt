package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessrepertoiretrainer.core.chess.controller.BoardAnnotations
import com.example.chessrepertoiretrainer.core.chess.controller.ChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.Arrow
import com.example.chessrepertoiretrainer.core.chess.pgn.navigator.GameNavigator
import com.example.chessrepertoiretrainer.core.chess.pgn.navigator.MoveNode
import com.example.chessrepertoiretrainer.core.chess.pgn.navigator.MoveNodeId
import com.example.chessrepertoiretrainer.core.chess.pgn.navigator.MoveTree
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons

/**
 * Primary shared layout for all chess screens.
 *
 * Structure (top → bottom):
 * 1. [topBar] — title row with back button and action icons
 * 2. [engineSection] — horizontal eval bar + engine line, hidden when engine is off
 * 3. Chessboard — always full width, square
 * 4. [contentBar] — fills remaining space (PGN, status, comments, move list, …)
 * 5. [bottomBar] — fixed-height action buttons
 */
@Composable
fun ChessScreenLayout(
    chessCtrl: ChessBoardController,
    topBar: @Composable () -> Unit,
    annotations: BoardAnnotations = remember { BoardAnnotations() },
    engineSection: @Composable () -> Unit = {},
    contentBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    arrowDrawingMode: Boolean = false,
    onArrowDrawn: ((Arrow) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        topBar()
        engineSection()
        Box(modifier = Modifier.fillMaxWidth()) {
            ChessboardUI(
                state = chessCtrl,
                arrows = annotations.arrows,
                lastMoveAnnotation = annotations.lastMoveAnnotation,
                arrowDrawingMode = arrowDrawingMode,
                onArrowDrawn = onArrowDrawn,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            contentBar()
        }
        bottomBar()
    }
}

// ---------------------------------------------------------------------------
// Shared top-bar helper
// ---------------------------------------------------------------------------

@Composable
fun ChessTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChessUiConstants.ChessTopBar.height)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    AppIcons.Back,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            actions()
        }
    }
}

// ---------------------------------------------------------------------------
// Shared bottom-bar helper
// ---------------------------------------------------------------------------

/**
 * Bottom action bar. Fill it with [BottomBarButton]s — each takes equal width via weight(1f).
 */
@Composable
fun ChessBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChessUiConstants.ChessBottomBar.height)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

/**
 * A single icon-above-label button that fills an equal share of the bottom bar.
 * Place it directly inside a [ChessBottomBar] lambda.
 */
@Composable
fun RowScope.BottomBarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val contentColor = if (enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(28.dp),
            tint = contentColor,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = contentColor,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------
// PGN viewer — navigator-driven, renders the mainline with variations in parens.
// ---------------------------------------------------------------------------

@Composable
fun PgnTextViewer(
    navigator: GameNavigator,
    onMoveClick: (MoveNodeId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tree = navigator.tree
    val currentNodeId = navigator.currentNodeId

    val scrollState = rememberScrollState()
    val tokens = androidx.compose.runtime.remember(tree) { flattenTree(tree) }
    val currentTokenIdx = tokens.indexOfFirst { it is PgnToken.Move && it.nodeId == currentNodeId }
    LaunchedEffect(currentTokenIdx) {
        if (currentTokenIdx >= 0) scrollState.animateScrollTo(scrollState.maxValue)
    }

    val highlightBg = MaterialTheme.colorScheme.primaryContainer
    val highlightFg = MaterialTheme.colorScheme.onPrimaryContainer
    val normalFg = MaterialTheme.colorScheme.onBackground
    val dimFg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    val varFg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)

    val annotated = buildAnnotatedString {
        if (tokens.isEmpty()) {
            withStyle(SpanStyle(color = dimFg)) { append("No moves yet.") }
        } else {
            tokens.forEach { token ->
                when (token) {
                    is PgnToken.MoveNumber -> withStyle(SpanStyle(color = dimFg)) { append(token.text) }
                    is PgnToken.Move -> {
                        val isCurrent = token.nodeId == currentNodeId
                        val fg = if (token.inVariation) varFg else normalFg
                        val spanStyle =
                            if (isCurrent) SpanStyle(background = highlightBg, color = highlightFg)
                            else SpanStyle(color = fg)
                        pushLink(
                            LinkAnnotation.Clickable(
                                tag = token.nodeId.value.toString(),
                                styles = TextLinkStyles(spanStyle),
                                linkInteractionListener = { onMoveClick(token.nodeId) },
                            )
                        )
                        append(token.san)
                        pop()
                    }
                    is PgnToken.Punctuation -> withStyle(SpanStyle(color = dimFg)) { append(token.text) }
                }
            }
        }
    }

    Text(
        text = annotated,
        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp),
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

private sealed interface PgnToken {
    data class MoveNumber(val text: String) : PgnToken
    data class Move(val nodeId: MoveNodeId, val san: String, val inVariation: Boolean) : PgnToken
    data class Punctuation(val text: String) : PgnToken
}

/** Flattens a [MoveTree] depth-first into a renderable token list. */
private fun flattenTree(tree: MoveTree): List<PgnToken> {
    val tokens = mutableListOf<PgnToken>()
    appendSection(tree, tree.rootChildren, tokens, inVariation = false, needsMoveNum = false)
    return tokens
}

private fun appendSection(
    tree: MoveTree,
    childIds: List<MoveNodeId>,
    tokens: MutableList<PgnToken>,
    inVariation: Boolean,
    needsMoveNum: Boolean,
) {
    val mainlineId = childIds.firstOrNull() ?: return
    val variations = childIds.drop(1)
    val node = tree.nodes[mainlineId] ?: return

    val fenParts = node.fenBefore.split(" ")
    val moveNum = fenParts.getOrNull(5)?.toIntOrNull() ?: 1
    val isWhite = fenParts.getOrNull(1) != "b"

    if (tokens.isNotEmpty()) tokens.add(PgnToken.Punctuation(" "))

    // Move number
    when {
        isWhite -> tokens.add(PgnToken.MoveNumber("$moveNum."))
        needsMoveNum || inVariation -> tokens.add(PgnToken.MoveNumber("$moveNum…"))  // ellipsis
    }
    if (isWhite || needsMoveNum || inVariation) tokens.add(PgnToken.Punctuation(" "))

    tokens.add(PgnToken.Move(mainlineId, node.san, inVariation))

    // Inline variations in parens after mainline move
    for (varId in variations) {
        val varNode = tree.nodes[varId] ?: continue
        val varFenParts = varNode.fenBefore.split(" ")
        val varMoveNum = varFenParts.getOrNull(5)?.toIntOrNull() ?: 1
        val varIsWhite = varFenParts.getOrNull(1) != "b"

        tokens.add(PgnToken.Punctuation(" ("))
        if (varIsWhite) {
            tokens.add(PgnToken.MoveNumber("$varMoveNum."))
        } else {
            tokens.add(PgnToken.MoveNumber("$varMoveNum…"))
        }
        tokens.add(PgnToken.Punctuation(" "))
        tokens.add(PgnToken.Move(varId, varNode.san, inVariation = true))
        appendSection(tree, varNode.children, tokens, inVariation = true, needsMoveNum = false)
        tokens.add(PgnToken.Punctuation(")"))
    }

    // Continue mainline
    appendSection(
        tree,
        node.children,
        tokens,
        inVariation = inVariation,
        needsMoveNum = variations.isNotEmpty(),  // re-state move number after variations
    )
}