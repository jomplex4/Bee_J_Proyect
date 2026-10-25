package com.digitalminds.beej

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.*
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

private val corners = RoundedCornerShape(20.dp)

@Composable
private fun BeeApp(vm: BeeViewModel = viewModel()) {
    val s by vm.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    fun play(name: String) {
        player?.release(); player = null
        val id = context.resources.getIdentifier(name, "raw", context.packageName)
        if (id != 0) {
            player = MediaPlayer.create(context,id)?.apply { setOnCompletionListener { it.release(); if (player === it) player = null }; start() }
        }
    }
    DisposableEffect(Unit) { onDispose { player?.release(); player = null } }
    BackHandler(s.page != "inicio") { vm.go("inicio") }
    MaterialTheme(colorScheme = darkColorScheme(primary=Brand.bright, background=Brand.bg, surface=Brand.panel, onSurface=Brand.white)) {
        Column(Modifier.fillMaxSize().background(Brand.bg).statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal=22.dp, vertical=16.dp), verticalAlignment=Alignment.CenterVertically) {
                if (s.page != "inicio") {
                    Text("‹", Modifier.clickable { vm.go("inicio") }.padding(end=16.dp), color=Brand.white, fontSize=32.sp)
                }
                Text("BEE J", fontSize=23.sp, fontWeight=FontWeight.Black, letterSpacing=2.sp, color=Brand.white, modifier=Modifier.weight(1f))
                Text("●", color=Brand.bright, fontSize=19.sp)
            }
            when(s.page) {
                "inicio" -> Home(s, vm)
                "palabras" -> WordsScreen(s,vm)
                "aprender" -> LearnScreen(s,vm,::play)
                "reto" -> ChallengeScreen(s,vm,::play)
                "alfabeto" -> AlphabetScreen(::play)
                "acerca" -> AboutScreen()
                else -> Home(s,vm)
            }
        }
    }
}

@Composable private fun Tag(text: String) { Text(text.uppercase(), color=Brand.bright, fontSize=11.sp, fontWeight=FontWeight.Bold, letterSpacing=2.sp) }
@Composable private fun Heading(title: String, subtitle: String) {
    Text(title, color=Brand.white, fontWeight=FontWeight.Bold, fontSize=29.sp, lineHeight=34.sp)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, color=Brand.muted, fontSize=13.sp, lineHeight=19.sp)
}
@Composable private fun Pill(label: String, onClick: () -> Unit, primary: Boolean = true, enabled: Boolean = true) {
    Button(onClick=onClick, enabled=enabled, modifier=Modifier.fillMaxWidth().height(54.dp), shape=RoundedCornerShape(50),
        colors=ButtonDefaults.buttonColors(containerColor=if(primary) Brand.red else Brand.raised, contentColor=Brand.white, disabledContainerColor=Brand.raised,disabledContentColor=Brand.muted)) {
        Text(label, fontWeight=FontWeight.Bold, fontSize=15.sp)
    }
}
@Composable private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.background(Brand.panel,corners).border(1.dp,Brand.raised,corners).padding(18.dp), content=content)
}
@Composable private fun Home(s: BeeState, vm: BeeViewModel) {
    LazyColumn(contentPadding=PaddingValues(start=22.dp,end=22.dp,bottom=32.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Spacer(Modifier.height(8.dp))
            Tag("Tu entrenamiento comienza aquí")
            Spacer(Modifier.height(12.dp))
            Heading("Domina cada letra.","30 palabras. 14 días. Una meta: deletrear con seguridad.")
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Tag("Plan intensivo")
                        Spacer(Modifier.height(6.dp))
                        Text("DÍA ${s.currentDay} / 14", color=Brand.white, fontSize=25.sp,fontWeight=FontWeight.Bold)
                    }
                    Text("${s.mastered.size}/30", color=Brand.white,fontSize=23.sp,fontWeight=FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(progress={s.mastered.size/30f},modifier=Modifier.fillMaxWidth().height(7.dp),color=Brand.bright,trackColor=Brand.raised)
                Spacer(Modifier.height(14.dp))
                Text("${Words.dayGroup(s.currentDay).joinToString(" · ") { it.value.lowercase() }}",color=Brand.muted,fontSize=12.sp,lineHeight=18.sp)
            }
        }
        item { HomeAction("01", "APRENDER", "Escucha, comprende y memoriza", onClick={vm.go("palabras")}) }
        item { HomeAction("02", "ENTRENAR", "Escribe la palabra letra por letra", onClick={vm.start("practica")}) }
        item { HomeAction("03", "CONCURSO", "Un error y termina la ronda", onClick={vm.start("concurso")}) }
        item { HomeAction("04", "REPASAR ERRORES", "Trabaja tus palabras difíciles (${s.wrong.size})", onClick={vm.start("repaso")}) }
        item { HomeAction("05", "ALFABETO", "Escucha las 26 letras en inglés", onClick={vm.go("alfabeto")}) }
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("${s.attempts} intentos · ${s.wins} aciertos",color=Brand.muted,fontSize=12.sp)
                Text("Acerca de",Modifier.clickable{vm.go("acerca")},color=Brand.muted,fontSize=12.sp)
            }
        }
    }
}
@Composable private fun HomeAction(number:String,title:String,description:String,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().background(Brand.panel,corners).clickable(onClick=onClick).padding(19.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(number,color=Brand.bright,fontSize=16.sp,fontWeight=FontWeight.Bold)
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title,color=Brand.white,fontSize=14.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
            Spacer(Modifier.height(4.dp))
            Text(description,color=Brand.muted,fontSize=12.sp)
        }
        Text("›",fontSize=28.sp,color=Brand.muted)
    }
}
@Composable private fun WordsScreen(s:BeeState,vm:BeeViewModel) {
    Column(Modifier.fillMaxSize().padding(horizontal=22.dp)) {
        Heading("Las 30 palabras","Selecciona una palabra para aprender su sonido y deletreo.")
        Spacer(Modifier.height(18.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=30.dp)) {
            itemsIndexed(Words.all){ index, w ->
                Row(Modifier.fillMaxWidth().background(Brand.panel,RoundedCornerShape(14.dp)).clickable { vm.select(index) }.padding(15.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("${w.id.toString().padStart(2,'0')}",color=Brand.muted,fontSize=12.sp)
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(w.value,color=Brand.white,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
                        Text(w.meaning,color=Brand.muted,fontSize=12.sp)
                    }
                    if (w.id in s.mastered) Text("✓",color=Brand.bright,fontSize=19.sp) else Text("›",color=Brand.muted,fontSize=23.sp)
                }
            }
        }
    }
}
@Composable private fun LearnScreen(s:BeeState,vm:BeeViewModel,play:(String)->Unit) {
    val w = Words.all[s.selected]
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=22.dp)) {
        Tag("Palabra ${w.id} de 30")
        Spacer(Modifier.height(16.dp))
        Panel(Modifier.fillMaxWidth()) {
            Text(w.value,color=Brand.white,fontSize=30.sp,fontWeight=FontWeight.Bold,letterSpacing=2.sp)
            Spacer(Modifier.height(4.dp)); Text(w.meaning,color=Brand.muted,fontSize=14.sp)
            Spacer(Modifier.height(22.dp))
            Pill("▶  ESCUCHAR PALABRA", {play("word_${w.id}")})
            Spacer(Modifier.height(9.dp))
            Pill("♫  ESCUCHAR DELETREO", {play("spell_${w.id}")},primary=false)
        }
        Spacer(Modifier.height(22.dp))
        Tag("Escucha letra por letra")
        Spacer(Modifier.height(12.dp))
        LetterTiles(w.letters,play)
        Spacer(Modifier.height(22.dp))
        Text("Método BEE J",color=Brand.white,fontWeight=FontWeight.Bold,fontSize=17.sp)
        Spacer(Modifier.height(8.dp))
        Text("1. Escucha la palabra. 2. Repite su pronunciación. 3. Escucha el deletreo. 4. Dilo sin mirar. 5. Pruébalo por escrito.",color=Brand.muted,fontSize=14.sp,lineHeight=23.sp)
        Spacer(Modifier.height(22.dp))
        Pill("PRACTICAR ESTA PALABRA",{vm.select(s.selected,"reto")})
        Spacer(Modifier.height(12.dp))
        Pill("SIGUIENTE PALABRA",{vm.select((s.selected+1)%30)},primary=false)
        Spacer(Modifier.height(24.dp))
    }
}
@Composable private fun LetterTiles(value:String,play:(String)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        value.chunked(7).forEach { row ->
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                row.forEach { letter ->
                    Box(Modifier.size(43.dp).background(Brand.raised,RoundedCornerShape(10.dp)).clickable {play("letter_${letter.lowercase()}")},contentAlignment=Alignment.Center) {
                        Text(letter.toString(),color=Brand.white,fontWeight=FontWeight.Bold,fontSize=18.sp)
                    }
                }
            }
        }
    }
}
@Composable private fun ChallengeScreen(s:BeeState,vm:BeeViewModel,play:(String)->Unit) {
    val w = Words.all[s.selected]
    val correct = Words.isCorrect(w,s.answer)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=22.dp)) {
        Tag(when(s.mode){"concurso"->"Simulador de concurso";"repaso"->"Repaso inteligente";else->"Entrenamiento de precisión"})
        Spacer(Modifier.height(12.dp))
        Heading(if(s.mode=="concurso") "Ronda ${s.contestRound}" else "Escucha y deletrea",if(s.mode=="concurso") "Un error termina tu participación." else "Escucha la palabra y escribe todas sus letras.")
        Spacer(Modifier.height(24.dp))
        Panel(Modifier.fillMaxWidth()) {
            Text("ESCUCHA ATENTAMENTE",color=Brand.muted,fontSize=11.sp,letterSpacing=2.sp)
            Spacer(Modifier.height(20.dp))
            Pill("▶  REPRODUCIR PALABRA",{play("word_${w.id}")})
            Spacer(Modifier.height(10.dp))
            Text("Pista: ${w.meaning}",color=Brand.muted,fontSize=13.sp)
            if(s.mode!="concurso") {
                Spacer(Modifier.height(10.dp))
                Text("${w.letters.length} letras",color=Brand.muted,fontSize=12.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value=s.answer,onValueChange=vm::answer, modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!s.checked,
            label={Text("Escribe el deletreo")},placeholder={Text("A B C ...")},
            keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Characters,autoCorrectEnabled=false),
            colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Brand.bright,unfocusedBorderColor=Brand.raised,focusedTextColor=Brand.white,unfocusedTextColor=Brand.white),shape=RoundedCornerShape(15.dp))
        Spacer(Modifier.height(14.dp))
        if (!s.checked) {
            Pill("COMPROBAR",vm::check,enabled=s.answer.isNotBlank())
        } else {
            Panel(Modifier.fillMaxWidth()) {
                Text(if(correct) "¡CORRECTO!" else "AÚN NO",color=if(correct) Brand.bright else Brand.error,fontSize=24.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Respuesta: ${w.letters}",color=Brand.white,fontSize=17.sp,fontWeight=FontWeight.Bold)
                if(!correct) {Spacer(Modifier.height(7.dp)); Text("Tu respuesta: ${s.answer}",color=Brand.muted,fontSize=13.sp)}
                Spacer(Modifier.height(14.dp))
                Pill("♫  ESCUCHAR DELETREO",{play("spell_${w.id}")},primary=false)
            }
            Spacer(Modifier.height(14.dp))
            if(s.mode=="concurso" && !s.contestAlive) {
                Pill("REINICIAR CONCURSO",{vm.start("concurso")})
            } else Pill("SIGUIENTE",vm::next)
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("Aciertos: ${s.wins}/${s.attempts}",color=Brand.muted,fontSize=12.sp)
            Text("Racha: ${s.streak}",color=Brand.muted,fontSize=12.sp)
        }
        Spacer(Modifier.height(28.dp))
    }
}
@Composable private fun AlphabetScreen(play:(String)->Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal=22.dp)) {
        Heading("Alfabeto A-Z","Toca cada letra y escucha su pronunciación estadounidense.")
        Spacer(Modifier.height(23.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=20.dp)) {
            items(7) { row ->
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    ('A'..'Z').drop(row*4).take(4).forEach { c ->
                        Box(Modifier.weight(1f).aspectRatio(1f).background(Brand.panel,RoundedCornerShape(17.dp)).clickable { play("letter_${c.lowercase()}") },contentAlignment=Alignment.Center) {
                            Text(c.toString(),color=Brand.white,fontSize=29.sp,fontWeight=FontWeight.Bold)
                        }
                    }
                    repeat(4-('A'..'Z').drop(row*4).take(4).size){ Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
@Composable private fun AboutScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val version = try { context.packageManager.getPackageInfo(context.packageName,0).versionName } catch(_:Exception) { "1.0" }
    Column(Modifier.fillMaxSize().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        Image(painterResource(R.drawable.ic_mark),null,modifier=Modifier.size(96.dp,57.dp))
        Spacer(Modifier.height(20.dp))
        Text("BEE J · $version",color=Brand.white,fontSize=22.sp,fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Creado por Joel Mamani Pauccara",color=Brand.muted,fontSize=13.sp)
        Spacer(Modifier.height(32.dp))
        Image(painterResource(R.drawable.splash_brand),null,modifier=Modifier.size(200.dp,56.dp))
        Spacer(Modifier.height(26.dp))
        Text("30 palabras. 14 días. Sin internet.",color=Brand.muted,fontSize=12.sp)
    }
}
