package com.example.chessrepertoiretrainer.core.engine

import android.content.Context
import android.util.Log
import com.example.chessrepertoiretrainer.core.chess.domain.toSan
import com.example.chessrepertoiretrainer.core.chess.domain.uciToMove
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettingsRepository
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

private const val TAG = "StockfishEngine"

class StockfishEngine(
    private val context: Context,
    private val settingsRepository: UserSettingsRepository
) {
    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _analysis = MutableStateFlow<EngineAnalysis?>(null)
    val analysis: StateFlow<EngineAnalysis?> = _analysis.asStateFlow()

    private val _searchState = MutableStateFlow(EngineSearchState.IDLE)
    val searchState: StateFlow<EngineSearchState> = _searchState.asStateFlow()

    private val _engineError = MutableStateFlow<String?>(null)
    val engineError: StateFlow<String?> = _engineError.asStateFlow()

    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null
    private var readingJob: Job? = null

    @Volatile
    private var currentFen: String? = null

    @Volatile
    private var lastSearchDepth: Int = 0

    @Volatile
    private var pendingForcedStop = false

    @Volatile
    private var pendingNewSearch: (() -> Unit)? = null

    val binaryPath: String
        get() = File(context.applicationInfo.nativeLibraryDir, "libstockfish.so").absolutePath

    val isBinaryAvailable: Boolean
        get() = File(context.applicationInfo.nativeLibraryDir, "libstockfish.so").exists()

    /** Enable engine for the given board position. */
    fun enable(fen: String) {
        if (_isEnabled.value) return
        currentFen = fen
        _engineError.value = null
        engineScope.launch {
            Log.d(TAG, "Starting engine, binary path: $binaryPath")
            if (!startProcessIfNeeded()) return@launch
            _isEnabled.value = true
            Log.d(TAG, "Engine enabled, analyzing FEN: $fen")
            analyzeCurrentPosition()
        }
    }

    fun disable() {
        _isEnabled.value = false
        _analysis.value = null
        _searchState.value = EngineSearchState.IDLE
        engineScope.launch { sendCommand("stop") }
    }

    fun updatePosition(fen: String) {
        currentFen = fen
        if (!_isEnabled.value) return

        // Handle terminal positions (checkmate / stalemate) locally without the engine.
        val terminal = checkTerminalPosition(fen)
        if (terminal != null) {
            _analysis.value = terminal
            _searchState.value = EngineSearchState.COMPLETE
            return
        }

        // Keep the previous analysis visible while the new search runs (no 0.5f flash).
        engineScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            stopAndThen {
                sendCommand("position fen $fen")
                sendGo(settings.engineDepth, settings.engineMovetime)
            }
        }
    }

    private fun checkTerminalPosition(fen: String): EngineAnalysis? = try {
        val board = Board()
        board.loadFromFen(fen)
        if (board.legalMoves().isNotEmpty()) null
        else if (board.isKingAttacked) {
            // Side to move is mated
            val whiteMated = board.sideToMove == Side.WHITE
            EngineAnalysis(
                centipawns = null,
                mateIn = if (whiteMated) -1 else 1,
                depth = 0,
                line = if (whiteMated) "Checkmate – Black wins" else "Checkmate – White wins"
            )
        } else {
            EngineAnalysis(centipawns = 0, mateIn = null, depth = 0, line = "Stalemate – Draw")
        }
    } catch (_: Exception) {
        null
    }

    /**
     * Analyze [fen] at [depth] without changing [isEnabled] — used by batch analyzers so the
     * user-visible engine toggle is not affected. Clears the [analysis] StateFlow immediately
     * so callers don't collect stale results from the previous position.
     */
    fun analyzePositionForBatch(fen: String, depth: Int) {
        currentFen = fen
        _analysis.value = null
        engineScope.launch {
            if (!startProcessIfNeeded()) return@launch
            stopAndThen {
                sendCommand("position fen $fen")
                sendGo(depth, 8_000)
            }
        }
    }

    fun analyzeDeeper() {
        val fen = currentFen ?: return
        if (!_isEnabled.value) return
        val deeperDepth = (lastSearchDepth + 5).coerceAtLeast(18)
        engineScope.launch {
            stopAndThen {
                sendCommand("position fen $fen")
                sendGo(deeperDepth, 5000)
            }
        }
    }

    fun shutdown() {
        engineScope.launch { sendCommand("quit") }
        process?.destroyForcibly()
        readingJob?.cancel()
        engineScope.cancel()
    }

    private fun stopAndThen(action: () -> Unit) {
        if (_searchState.value == EngineSearchState.SEARCHING) {
            pendingForcedStop = true
            pendingNewSearch = action
            sendCommand("stop")
        } else {
            action()
        }
    }

    private suspend fun analyzeCurrentPosition() {
        val fen = currentFen ?: return
        val settings = settingsRepository.settingsFlow.first()
        sendCommand("position fen $fen")
        sendGo(settings.engineDepth, settings.engineMovetime)
    }

    private fun sendGo(depth: Int, movetime: Int) {
        _searchState.value = EngineSearchState.SEARCHING
        sendCommand("go depth $depth movetime $movetime")
    }

    private fun sendCommand(cmd: String) {
        try {
            val w = writer ?: return
            w.write("$cmd\n")
            w.flush()
        } catch (e: Exception) {
            Log.e(TAG, "sendCommand failed for '$cmd': ${e.message}")
        }
    }

    private suspend fun startProcessIfNeeded(): Boolean {
        if (process?.isAlive == true) return true

        val binary = File(context.applicationInfo.nativeLibraryDir, "libstockfish.so")
        if (!binary.exists()) {
            val msg = "Binary not found at: ${binary.absolutePath}"
            Log.e(TAG, msg)
            _engineError.value = msg
            return false
        }
        if (!binary.canExecute()) {
            Log.w(TAG, "Binary not executable, attempting chmod: ${binary.absolutePath}")
            try {
                Runtime.getRuntime().exec("chmod 755 ${binary.absolutePath}").waitFor()
            } catch (_: Exception) {
            }
        }

        return try {
            Log.d(TAG, "Launching: ${binary.absolutePath}")
            val p = ProcessBuilder(binary.absolutePath).redirectErrorStream(true).start()

            // Give the process a moment to crash if it's going to (wrong CPU variant, etc.)
            delay(300L)
            if (!p.isAlive) {
                val exitCode = p.exitValue()
                val output = try {
                    p.inputStream.bufferedReader().readText().take(300)
                } catch (_: Exception) {
                    ""
                }
                val hint = when (exitCode) {
                    132 -> " (SIGILL — binary uses CPU instructions not supported by this device; try a different build)"
                    139 -> " (SIGSEGV — binary crashed)"
                    126 -> " (permission denied — binary not executable)"
                    else -> ""
                }
                val msg = "Engine exited immediately (code $exitCode)$hint. Output: $output"
                Log.e(TAG, msg)
                _engineError.value = msg
                return false
            }

            process = p
            // Use a raw OutputStreamWriter — no extra buffering layer so flushes are immediate
            writer = OutputStreamWriter(p.outputStream, Charsets.UTF_8)
            reader = BufferedReader(InputStreamReader(p.inputStream, Charsets.UTF_8))

            val ready = CompletableDeferred<Unit>()
            readingJob = engineScope.launch {
                val r = reader ?: return@launch
                var initialized = false
                while (isActive) {
                    val line = try {
                        r.readLine()
                    } catch (_: Exception) {
                        break
                    } ?: break
                    Log.v(TAG, "< $line")
                    if (!initialized) {
                        if (line.trim() == "readyok") {
                            initialized = true
                            Log.d(TAG, "Engine ready")
                            ready.complete(Unit)
                        }
                    } else {
                        handleOutputLine(line)
                    }
                }
                Log.d(TAG, "Reading loop ended")
            }

            sendCommand("uci")
            val settings = settingsRepository.settingsFlow.first()
            sendCommand("setoption name Threads value ${settings.engineThreads}")
            sendCommand("isready")

            val timedOut = withTimeoutOrNull(10_000L) { ready.await() } == null
            if (timedOut) {
                val msg = "Engine init timed out (no readyok in 10s)"
                Log.e(TAG, msg)
                _engineError.value = msg
                p.destroyForcibly()
                process = null
                return false
            }
            true
        } catch (e: Exception) {
            val msg = "Failed to start engine: ${e.message}"
            Log.e(TAG, msg, e)
            _engineError.value = msg
            false
        }
    }

    private fun handleOutputLine(line: String) {
        when {
            line.startsWith("info") && line.contains(" pv ") -> parseInfoLine(line)
            line.startsWith("bestmove") -> {
                if (pendingForcedStop) {
                    pendingForcedStop = false
                    pendingNewSearch?.invoke()
                    pendingNewSearch = null
                } else {
                    _searchState.value = EngineSearchState.COMPLETE
                }
            }
        }
    }

    private fun parseInfoLine(line: String) {
        val tokens = line.split(" ")
        var depth = 0
        var cp: Int? = null
        var mateIn: Int? = null
        val pvMoves = mutableListOf<String>()
        var i = 1
        while (i < tokens.size) {
            when (tokens[i]) {
                "depth" -> depth = tokens.getOrNull(i + 1)?.toIntOrNull() ?: 0
                "score" -> when (tokens.getOrNull(i + 1)) {
                    "cp" -> {
                        cp = tokens.getOrNull(i + 2)?.toIntOrNull(); mateIn = null
                    }

                    "mate" -> {
                        mateIn = tokens.getOrNull(i + 2)?.toIntOrNull(); cp = null
                    }
                }

                "pv" -> {
                    var j = i + 1
                    while (j < tokens.size && tokens[j].length in 4..5 && tokens[j][0].isLetter()) {
                        pvMoves.add(tokens[j]); j++
                    }
                    break
                }

                "upperbound", "lowerbound" -> return
            }
            i++
        }

        if (depth > 0 && pvMoves.isNotEmpty() && (cp != null || mateIn != null)) {
            lastSearchDepth = depth
            val fen = currentFen ?: return
            // Stockfish may report scores from the side-to-move perspective.
            // Normalise to white's perspective so the eval bar is always correct.
            val isBlackToMove = fen.split(" ").getOrNull(1) == "b"
            val whiteCp = if (isBlackToMove && cp != null) -cp else cp
            val whiteMate = if (isBlackToMove && mateIn != null) -mateIn else mateIn
            val pvSan = convertPvToSan(fen, pvMoves)
            _analysis.value = EngineAnalysis(
                centipawns = whiteCp,
                mateIn = whiteMate,
                depth = depth,
                line = pvSan
            )
        }
    }

    private fun convertPvToSan(startFen: String, uciMoves: List<String>): String {
        return try {
            val board = Board()
            board.loadFromFen(startFen)
            val fenParts = startFen.split(" ")
            var moveNum = fenParts.getOrNull(5)?.toIntOrNull() ?: 1
            val firstMoveIsBlack = board.sideToMove == Side.BLACK
            val sb = StringBuilder()

            uciMoves.take(8).forEachIndexed { index, uciMove ->
                val isWhiteMove = board.sideToMove == Side.WHITE
                if (isWhiteMove) sb.append("$moveNum. ")
                else if (index == 0 && firstMoveIsBlack) sb.append("$moveNum... ")

                val move = uciToMove(uciMove, board) ?: return@forEachIndexed
                val san = board.toSan(move)
                board.doMove(move)
                sb.append("$san ")
                if (board.sideToMove == Side.WHITE) moveNum++
            }
            sb.toString().trim()
        } catch (_: Exception) {
            uciMoves.take(5).joinToString(" ")
        }
    }
}