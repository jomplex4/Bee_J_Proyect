@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.digitalminds.beej

import android.Manifest
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        setContent { BeeApp() }
    }
}

private val cardShape = RoundedCornerShape(22.dp)
private val chipShape = RoundedCornerShape(50)

@Composable
private fun BeeApp(vm: BeeViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val handler = remember { Handler(Looper.getMainLooper()) }
    var currentPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var pendingIntent by remember { mutableStateOf<Intent?>(null) }
    var pendingCallback by remember { mutableStateOf<(String) -> Unit>({}) }

    fun stopAudio() {
        handler.removeCallbacksAndMessages(null)
        currentPlayer?.release()
        currentPlayer = null
    }

    fun playRaw(name: String, speed: Float = 1.0f) {
        stopAudio()
        val id = context.resources.getIdentifier(name, "raw", context.packageName)
        if (id == 0) return
        currentPlayer = MediaPlayer.create(context, id)?.apply {
            try {
                playbackParams = playbackParams.setSpeed(speed)
            } catch (_: Exception) {
            }
            setOnCompletionListener {
                it.release()
                if (currentPlayer === it) currentPlayer = null
            }
            start()
        }
    }

    fun playSpelling(word: BeeWord, speed: Float, useDoubleLetterCall: Boolean) {
        stopAudio()
        val sequence = mutableListOf<String>()
        var index = 0
        while (index < word.letters.length) {
            val current = word.letters[index]
            val next = word.letters.getOrNull(index + 1)
            if (useDoubleLetterCall && next == current) {
                sequence += "term_double"
                sequence += "letter_${current.lowercaseChar()}"
                index += 2
            } else {
                sequence += "letter_${current.lowercaseChar()}"
                index += 1
            }
        }
        val gap = when {
            speed <= 0.85f -> 340L
            speed >= 1.15f -> 130L
            else -> 220L
        }
        fun playAt(position: Int) {
            if (position >= sequence.size) return
            val id = context.resources.getIdentifier(sequence[position], "raw", context.packageName)
            if (id == 0) {
                playAt(position + 1)
                return
            }
            currentPlayer = MediaPlayer.create(context, id)?.apply {
                try {
                    playbackParams = playbackParams.setSpeed(speed)
                } catch (_: Exception) {
                }
                setOnCompletionListener { mp ->
                    mp.release()
                    if (currentPlayer === mp) currentPlayer = null
                    handler.postDelayed({ playAt(position + 1) }, gap)
                }
                start()
            }
        }
        playAt(0)
    }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        pendingCallback(text)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val launchIntent = pendingIntent
        if (granted && launchIntent != null) {
            speechLauncher.launch(launchIntent)
        } else {
            Toast.makeText(context, "Micrófono no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchSpeech(prompt: String, onResult: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Reconocimiento de voz no disponible en este dispositivo", Toast.LENGTH_LONG).show()
            return
        }
        pendingCallback = onResult
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
        pendingIntent = intent
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                speechLauncher.launch(intent)
            }
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose { stopAudio() }
    }

    BackHandler(state.screen != Screen.HOME) {
        when (state.screen) {
            Screen.LEARN_DETAIL -> vm.go(Screen.LEARN_LIST)
            Screen.TRAIN_DETAIL -> vm.go(Screen.TRAIN_LIST)
            Screen.REVIEW_DETAIL -> vm.go(Screen.REVIEW_LIST)
            else -> vm.go(Screen.HOME)
        }
    }

    MaterialTheme {
        Surface(color = Brand.bg) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brand.bg)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                when (state.screen) {
                    Screen.HOME -> HomeScreen(state, onOpen = vm::go)
                    Screen.LEARN_LIST -> WordListScreen(words = Words.all, onBack = { vm.go(Screen.HOME) }, onSelect = vm::openLearn)
                    Screen.LEARN_DETAIL -> LearnDetailScreen(
                        word = Words.byId(state.selectedWordId),
                        state = state,
                        onBack = { vm.go(Screen.LEARN_LIST) },
                        onPrevious = vm::previousWord,
                        onNext = vm::nextWord,
                        onWord = { playRaw("word_${state.selectedWordId}", state.wordSpeed) },
                        onSpell = { playSpelling(Words.byId(state.selectedWordId), state.spellSpeed, state.useDoubleLetterCall) },
                        onWordSpeed = vm::setWordSpeed,
                        onSpellSpeed = vm::setSpellSpeed,
                        onDoubleMode = vm::setDoubleLetterMode
                    )
                    Screen.TRAIN_LIST -> WordListScreen(words = Words.all, onBack = { vm.go(Screen.HOME) }, onSelect = vm::openTrain)
                    Screen.TRAIN_DETAIL -> TrainDetailScreen(
                        word = Words.byId(state.selectedWordId),
                        state = state,
                        onBack = { vm.go(Screen.TRAIN_LIST) },
                        onPrevious = vm::previousWord,
                        onNext = vm::nextWord,
                        onWord = { playRaw("word_${state.selectedWordId}", state.wordSpeed) },
                        onSpell = { playSpelling(Words.byId(state.selectedWordId), state.spellSpeed, state.useDoubleLetterCall) },
                        onWordSpeed = vm::setWordSpeed,
                        onSpellSpeed = vm::setSpellSpeed,
                        onDoubleMode = vm::setDoubleLetterMode,
                        onModeVoice = { vm.setTrainModeVoice(true) },
                        onModeTyped = { vm.setTrainModeVoice(false) },
                        onTypedChange = vm::updateTypedAttempt,
                        onTypedCheck = vm::evaluateTypedAttempt,
                        onVoice = {
                            launchSpeech("Say only the letters of the word", vm::evaluateVoicePractice)
                        }
                    )
                    Screen.CONTEST -> ContestScreen(
                        state = state,
                        onBack = { vm.go(Screen.HOME) },
                        onStart = vm::startContest,
                        onHideWord = vm::hideContestWord,
                        onHearWord = { playRaw("word_${state.contestWordId}", state.wordSpeed) },
                        onSpeak = {
                            launchSpeech("Say the word, spell it, and say the word again", vm::evaluateContestVoice)
                        },
                        onRestart = vm::startContest
                    )
                    Screen.REVIEW_LIST -> WordListScreen(
                        words = Words.all.filter { it.id in state.wrong },
                        onBack = { vm.go(Screen.HOME) },
                        onSelect = vm::openReview,
                        emptyText = "Aún no tienes errores guardados"
                    )
                    Screen.REVIEW_DETAIL -> TrainDetailScreen(
                        word = Words.byId(state.selectedWordId),
                        state = state,
                        onBack = { vm.go(Screen.REVIEW_LIST) },
                        onPrevious = vm::previousWord,
                        onNext = vm::nextWord,
                        onWord = { playRaw("word_${state.selectedWordId}", state.wordSpeed) },
                        onSpell = { playSpelling(Words.byId(state.selectedWordId), state.spellSpeed, state.useDoubleLetterCall) },
                        onWordSpeed = vm::setWordSpeed,
                        onSpellSpeed = vm::setSpellSpeed,
                        onDoubleMode = vm::setDoubleLetterMode,
                        onModeVoice = { vm.setTrainModeVoice(true) },
                        onModeTyped = { vm.setTrainModeVoice(false) },
                        onTypedChange = vm::updateTypedAttempt,
                        onTypedCheck = vm::evaluateTypedAttempt,
                        onVoice = {
                            launchSpeech("Say only the letters of the word", vm::evaluateVoicePractice)
                        }
                    )
                    Screen.ALPHABET -> AlphabetScreen(onBack = { vm.go(Screen.HOME) }, onPlay = { playRaw("letter_${it.lowercaseChar()}", 1.0f) })
                    Screen.METHOD -> MethodScreen(day = state.currentDay, mastered = state.mastered.size, onBack = { vm.go(Screen.HOME) })
                    Screen.ABOUT -> AboutScreen(onBack = { vm.go(Screen.HOME) })
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(state: BeeState, onOpen: (Screen) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBrandBar(day = state.currentDay, onAbout = { onOpen(Screen.ABOUT) })
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "BEE J",
                    color = Brand.white,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "30 palabras, 14 días, práctica offline",
                    color = Brand.muted,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(14.dp))
                ProgressStrip(day = state.currentDay, mastered = state.mastered.size)
            }
            item { HomeSectionCard(index = "01", title = "Aprender", subtitle = "Escucha la palabra y su deletreo", onClick = { onOpen(Screen.LEARN_LIST) }) }
            item { HomeSectionCard(index = "02", title = "Entrenar", subtitle = "Voz y escritura con evaluación", onClick = { onOpen(Screen.TRAIN_LIST) }) }
            item { HomeSectionCard(index = "03", title = "Concurso", subtitle = "Formato oficial con voz", onClick = { onOpen(Screen.CONTEST) }) }
            item { HomeSectionCard(index = "04", title = "Repasar errores", subtitle = "Refuerza las palabras falladas", onClick = { onOpen(Screen.REVIEW_LIST) }) }
            item { HomeSectionCard(index = "05", title = "Alfabeto", subtitle = "Verifica la pronunciación de A a Z", onClick = { onOpen(Screen.ALPHABET) }) }
            item { HomeSectionCard(index = "06", title = "Método", subtitle = "Plan profesional de 14 días", onClick = { onOpen(Screen.METHOD) }) }
            item {
                Spacer(Modifier.height(2.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${state.wins}/${state.attempts} aciertos", color = Brand.muted, fontSize = 12.sp)
                    Text("Racha ${state.streak}", color = Brand.muted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TopBrandBar(day: Int, onAbout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.ic_mark),
            contentDescription = null,
            modifier = Modifier.width(168.dp).height(30.dp)
        )
        Spacer(Modifier.weight(1f))
        Chip(text = "Día $day/14", selected = false, onClick = {})
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFCCCCD2)).clickable(onClick = onAbout),
            contentAlignment = Alignment.Center
        ) {
            Text("i", color = Brand.bg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProgressStrip(day: Int, mastered: Int) {
    Box(
        modifier = Modifier.fillMaxWidth().background(Brand.panel, cardShape).border(1.dp, Brand.raised, cardShape).padding(16.dp)
    ) {
        Column {
            Text("Día $day/14", color = Brand.white, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text("$mastered/30 dominadas", color = Brand.muted, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).background(Brand.raised, RoundedCornerShape(50))) {
                Box(
                    Modifier.fillMaxWidth((mastered / 30f).coerceIn(0f, 1f)).height(8.dp).background(Brand.bright, RoundedCornerShape(50))
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                Words.planWords(day).joinToString(" · ") { it.value.lowercase() },
                color = Brand.muted,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeSectionCard(index: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Brand.panel, cardShape).clickable(onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Brand.raised), contentAlignment = Alignment.Center) {
            Text(index, color = Brand.bright, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Brand.white, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Brand.muted, fontSize = 13.sp)
        }
        Text("›", color = Brand.muted, fontSize = 28.sp)
    }
}

@Composable
private fun WordListScreen(words: List<BeeWord>, onBack: () -> Unit, onSelect: (Int) -> Unit, emptyText: String = "") {
    Column(Modifier.fillMaxSize()) {
        MinimalTopBar(onBack = onBack)
        if (words.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyText, color = Brand.muted, fontSize = 16.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(words) { word ->
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Brand.panel, cardShape).clickable { onSelect(word.id) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Brand.raised), contentAlignment = Alignment.Center) {
                            Text(word.id.toString().padStart(2, '0'), color = Brand.bright, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(word.value, color = Brand.white, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        Text("›", color = Brand.muted, fontSize = 28.sp)
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun LearnDetailScreen(
    word: BeeWord,
    state: BeeState,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onWord: () -> Unit,
    onSpell: () -> Unit,
    onWordSpeed: (Float) -> Unit,
    onSpellSpeed: (Float) -> Unit,
    onDoubleMode: (Boolean) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        MinimalTopBar(onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Text(word.value, color = Brand.white, fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text(word.meaning, color = Brand.muted, fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))
            CompactControlRow(onPrevious = onPrevious, onWord = onWord, onSpell = onSpell, onNext = onNext)
            Spacer(Modifier.height(20.dp))
            SpeedBlock(title = "Pronunciación", current = state.wordSpeed, onSelect = onWordSpeed)
            Spacer(Modifier.height(12.dp))
            SpeedBlock(title = "Deletreo", current = state.spellSpeed, onSelect = onSpellSpeed)
            Spacer(Modifier.height(12.dp))
            DoubleModeBlock(enabled = state.useDoubleLetterCall, onChange = onDoubleMode)
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().background(Brand.panel, cardShape).border(1.dp, Brand.raised, cardShape).padding(18.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("${word.letters.toCharArray().joinToString(" · ")}", color = Brand.white, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text("Usa el ícono de sonido para la palabra y el ícono de deletreo para repetir cada letra con la velocidad elegida.", color = Brand.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun TrainDetailScreen(
    word: BeeWord,
    state: BeeState,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onWord: () -> Unit,
    onSpell: () -> Unit,
    onWordSpeed: (Float) -> Unit,
    onSpellSpeed: (Float) -> Unit,
    onDoubleMode: (Boolean) -> Unit,
    onModeVoice: () -> Unit,
    onModeTyped: () -> Unit,
    onTypedChange: (String) -> Unit,
    onTypedCheck: () -> Unit,
    onVoice: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        MinimalTopBar(onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            CompactControlRow(onPrevious = onPrevious, onWord = onWord, onSpell = onSpell, onNext = onNext)
            Spacer(Modifier.height(16.dp))
            ModeSwitch(isVoice = state.trainModeVoice, onVoice = onModeVoice, onTyped = onModeTyped)
            Spacer(Modifier.height(16.dp))
            SpeedBlock(title = "Pronunciación", current = state.wordSpeed, onSelect = onWordSpeed)
            Spacer(Modifier.height(12.dp))
            SpeedBlock(title = "Deletreo", current = state.spellSpeed, onSelect = onSpellSpeed)
            Spacer(Modifier.height(12.dp))
            DoubleModeBlock(enabled = state.useDoubleLetterCall, onChange = onDoubleMode)
            Spacer(Modifier.height(18.dp))
            if (state.trainModeVoice) {
                VoicePracticeCard(state = state, onVoice = onVoice)
            } else {
                TypedPracticeCard(word = word, state = state, onTypedChange = onTypedChange, onTypedCheck = onTypedCheck)
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun ContestScreen(
    state: BeeState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onHideWord: () -> Unit,
    onHearWord: () -> Unit,
    onSpeak: () -> Unit,
    onRestart: () -> Unit
) {
    val word = Words.byId(state.contestWordId)
    LaunchedEffect(state.contestVisibleWord, state.contestWordId) {
        if (state.contestVisibleWord) {
            delay(2500)
            onHideWord()
        }
    }
    LaunchedEffect(state.contestVisibleWord, state.contestWordId) {
        if (state.contestVisibleWord && state.contestMessage != "Presiona iniciar para mostrar la palabra") {
            onHearWord()
        }
    }
    Column(Modifier.fillMaxSize()) {
        MinimalTopBar(onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().background(Brand.panel, cardShape).border(1.dp, Brand.raised, cardShape).padding(20.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Ronda ${state.contestRound}", color = Brand.bright, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(10.dp))
                    if (state.contestMessage == "Presiona iniciar para mostrar la palabra") {
                        Text(state.contestMessage, color = Brand.muted, fontSize = 16.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton(text = "Iniciar", onClick = onStart)
                    } else {
                        if (state.contestVisibleWord) {
                            Text(word.value, color = Brand.white, fontSize = 34.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Text("Memoriza la palabra", color = Brand.muted, fontSize = 13.sp)
                        } else {
                            Text(state.contestMessage, color = Brand.white, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Text("Formato: palabra, deletreo, palabra", color = Brand.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            SquareAction(text = "Hablar", onClick = onSpeak)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (state.lastVoiceResult.isNotBlank()) {
                ResultCard(
                    label = if (state.contestPassedLast == true) "Perfecto" else "Revisar",
                    message = state.lastVoiceResult,
                    transcript = state.lastVoiceTranscript
                )
                Spacer(Modifier.height(14.dp))
                if (state.contestPassedLast == false) {
                    PrimaryButton(text = "Reiniciar", onClick = onRestart)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AlphabetScreen(onBack: () -> Unit, onPlay: (Char) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        MinimalTopBar(onBack = onBack)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(('A'..'Z').toList()) { letter ->
                Box(
                    modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(18.dp)).background(Brand.panel).clickable { onPlay(letter) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(letter.toString(), color = Brand.white, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MethodScreen(day: Int, mastered: Int, onBack: () -> Unit) {
    val steps = listOf(
        "1. Escucha la palabra y repítela en voz alta dos veces.",
        "2. Escucha el deletreo y luego dilo sin mirar.",
        "3. Recupérala de memoria, primero con voz y luego por escrito.",
        "4. Repite más seguido las palabras falladas. Esa es la base de la repetición espaciada.",
        "5. Simula el formato de concurso: palabra, deletreo, palabra.",
        "6. Al final del día repasa solo tus errores. Al día siguiente mezcla palabras viejas y nuevas."
    )
    val plan = listOf(
        "Días 1 al 10, aprende 3 palabras nuevas por día.",
        "Día 11, repaso fuerte de las palabras 1 a 10.",
        "Día 12, repaso fuerte de las palabras 11 a 20.",
        "Día 13, repaso fuerte de las palabras 21 a 30.",
        "Día 14, simulación completa de concurso y refuerzo final."
    )
    Column(Modifier.fillMaxSize()) {
        MinimalTopBar(onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text("Método Bee J", color = Brand.white, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text("Basado en práctica de recuperación, repetición espaciada y simulación del formato de spelling bee.", color = Brand.muted, fontSize = 14.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().background(Brand.panel, cardShape).padding(16.dp)) {
                Column {
                    Text("Hoy vas en el día $day", color = Brand.bright, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("$mastered/30 palabras marcadas como dominadas.", color = Brand.white, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            steps.forEach {
                StepCard(text = it)
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text("Plan de 14 días", color = Brand.white, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            plan.forEach {
                StepCard(text = it)
                Spacer(Modifier.height(10.dp))
            }
            StepCard(text = "Ejemplo: CAREFUL, C A R E F U L, CAREFUL. Si prefieres usar letras dobles, la app puede decir DOUBLE F en lugar de F, F.")
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.3"
        } catch (_: Exception) {
            "1.3"
        }
    }
    Column(Modifier.fillMaxSize()) {
        MinimalTopBar(onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.foundation.Image(painter = painterResource(R.drawable.ic_mark), contentDescription = null, modifier = Modifier.width(220.dp).height(56.dp))
            Spacer(Modifier.height(18.dp))
            Text("BEE J $version", color = Brand.white, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(8.dp))
            Text("Creado por Joel Mamani Pauccara", color = Brand.muted, fontSize = 14.sp)
            Spacer(Modifier.height(22.dp))
            androidx.compose.foundation.Image(painter = painterResource(R.drawable.splash_brand), contentDescription = null, modifier = Modifier.width(220.dp).height(70.dp))
        }
    }
}

@Composable
private fun MinimalTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(Brand.panel).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Text("‹", color = Brand.white, fontSize = 28.sp)
        }
    }
}

@Composable
private fun CompactControlRow(onPrevious: () -> Unit, onWord: () -> Unit, onSpell: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        SquareAction(text = "‹", onClick = onPrevious)
        SquareAction(text = "🔊", onClick = onWord)
        SquareAction(text = "ABC", onClick = onSpell)
        SquareAction(text = "›", onClick = onNext)
    }
}

@Composable
private fun SquareAction(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(Brand.panel).border(1.dp, Brand.raised, RoundedCornerShape(18.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Brand.white, fontWeight = FontWeight.Bold, fontSize = if (text.length == 1) 28.sp else 18.sp)
    }
}

@Composable
private fun SpeedBlock(title: String, current: Float, onSelect: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, color = Brand.muted, fontSize = 13.sp, modifier = Modifier.widthIn(min = 96.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.85f, 1.0f, 1.15f).forEach { value ->
                Chip(text = "${value}x", selected = current == value, onClick = { onSelect(value) })
            }
        }
    }
}

@Composable
private fun DoubleModeBlock(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("Dobles", color = Brand.muted, fontSize = 13.sp, modifier = Modifier.widthIn(min = 96.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(text = "L, L", selected = !enabled, onClick = { onChange(false) })
            Chip(text = "DOUBLE L", selected = enabled, onClick = { onChange(true) })
        }
    }
}

@Composable
private fun ModeSwitch(isVoice: Boolean, onVoice: () -> Unit, onTyped: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f)) { Chip(text = "Voz", selected = isVoice, onClick = onVoice, fill = true) }
        Box(Modifier.weight(1f)) { Chip(text = "Escritura", selected = !isVoice, onClick = onTyped, fill = true) }
    }
}

@Composable
private fun VoicePracticeCard(state: BeeState, onVoice: () -> Unit) {
    Box(Modifier.fillMaxWidth().background(Brand.panel, cardShape).border(1.dp, Brand.raised, cardShape).padding(18.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Di solo las letras", color = Brand.white, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            PrimaryButton(text = "Escuchar y responder", onClick = onVoice)
            if (state.lastVoiceResult.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                ResultCard(label = state.lastVoiceResult.substringBefore("·").trim(), message = state.lastVoiceResult, transcript = state.lastVoiceTranscript)
            }
        }
    }
}

@Composable
private fun TypedPracticeCard(word: BeeWord, state: BeeState, onTypedChange: (String) -> Unit, onTypedCheck: () -> Unit) {
    Box(Modifier.fillMaxWidth().background(Brand.panel, cardShape).border(1.dp, Brand.raised, cardShape).padding(18.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Escribe el deletreo", color = Brand.white, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            LetterSlots(value = state.typedAttempt, length = word.letters.length)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = state.typedAttempt,
                onValueChange = onTypedChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Solo letras", color = Brand.muted) },
                textStyle = TextStyle(letterSpacing = 4.sp, fontSize = 18.sp, color = Brand.white),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Brand.bright,
                    unfocusedBorderColor = Brand.raised,
                    focusedTextColor = Brand.white,
                    unfocusedTextColor = Brand.white,
                    cursorColor = Brand.white,
                    focusedContainerColor = Brand.bg,
                    unfocusedContainerColor = Brand.bg
                ),
                shape = RoundedCornerShape(18.dp)
            )
            Spacer(Modifier.height(14.dp))
            PrimaryButton(text = "Comprobar", onClick = onTypedCheck, enabled = state.typedAttempt.isNotBlank())
            if (state.lastTypedLabel.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                ResultCard(
                    label = state.lastTypedLabel,
                    message = if (state.lastTypedExact == true) "Respuesta exacta" else "Revisa tus letras",
                    transcript = state.typedAttempt
                )
            }
        }
    }
}

@Composable
private fun LetterSlots(value: String, length: Int) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(length) { index ->
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Brand.bg).border(1.dp, Brand.raised, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(value.getOrNull(index)?.toString() ?: "_", color = Brand.white, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun ResultCard(label: String, message: String, transcript: String) {
    Box(Modifier.fillMaxWidth().background(Brand.bg, RoundedCornerShape(18.dp)).border(1.dp, Brand.raised, RoundedCornerShape(18.dp)).padding(14.dp)) {
        Column {
            Text(label, color = Brand.bright, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(message, color = Brand.white, fontSize = 14.sp)
            if (transcript.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(transcript, color = Brand.muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit, fill: Boolean = false) {
    Box(
        modifier = Modifier
            .then(if (fill) Modifier.fillMaxWidth() else Modifier)
            .clip(chipShape)
            .background(if (selected) Brand.red else Brand.raised)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Brand.white, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = chipShape,
        colors = ButtonDefaults.buttonColors(containerColor = Brand.red, contentColor = Brand.white, disabledContainerColor = Brand.raised, disabledContentColor = Brand.muted),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StepCard(text: String) {
    Box(Modifier.fillMaxWidth().background(Brand.panel, cardShape).padding(16.dp)) {
        Text(text, color = Brand.white, fontSize = 14.sp, lineHeight = 22.sp)
    }
}

