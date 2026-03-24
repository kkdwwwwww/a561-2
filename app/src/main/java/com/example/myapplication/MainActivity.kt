package com.example.myapplication

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.gson.Gson
import kotlinx.coroutines.delay
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import kotlin.random.Random

class MainActivity : ComponentActivity(), SensorEventListener {
    lateinit var sm : SensorManager
    var rx by mutableStateOf(0f)
    var ry by mutableStateOf(0f)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sm = getSystemService(SENSOR_SERVICE) as SensorManager
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding),rx,ry
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sm.registerListener(this,sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),1)
    }

    override fun onPause() {
        super.onPause()
        sm.unregisterListener(this)
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    override fun onSensorChanged(p0: SensorEvent?) {
        p0?.let {
            rx = rx * 0.7f + (-it.values[0]) * 0.3f
            ry = ry * 0.7f + (it.values[1]) * 0.3f
        }
    }
}
data class QSS(val question: String,val selections : List<String>,val answer: String){val ans:Int get() = selections.indexOf(answer)+1}
data class QSD(val questionCount: Int,val questionList: List<QSS>)
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier, rx: Float, ry: Float) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val w = with(density){configuration.screenWidthDp.dp.toPx()}
    val h = with(density){configuration.screenHeightDp.dp.toPx()}
    val dot = with(density){30.dp.toPx()}
    var qs by remember { mutableStateOf(listOf<QSS>()) }
    LaunchedEffect(Unit) {
        val clp = OkHttpClient()
        val re = Request.Builder().url("https://raw.githubusercontent.com/kkdwwwwww/englishapi/refs/heads/main/question.json").build()
        clp.newCall(re).enqueue(object : Callback{
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if(!it.isSuccessful) return
                    val body = it.body.string()
                    val data = Gson().fromJson(body,QSD::class.java)
                    qs = data.questionList
                }
            }
        })
    }
    if(qs.isEmpty()) return
    var qIdx by remember { mutableStateOf(Random.nextInt(5)) }
    var qqIdx by remember { mutableStateOf(0) }
    var sc by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(0) }
    var wrong by remember { mutableStateOf(0) }
    var pt by remember { mutableStateOf(0f) }
    var lock by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(0L) }
    var cp by remember { mutableStateOf(Offset(w/2,h/2)) }
    var cal by remember { mutableStateOf(Offset.Zero) }
    val q = qs[qIdx];
    var r1 by remember { mutableStateOf(Rect.Zero) }
    var r2 by remember { mutableStateOf(Rect.Zero) }
    var r3 by remember { mutableStateOf(Rect.Zero) }
    var r4 by remember { mutableStateOf(Rect.Zero) }
    var hover = 0
    if(page == 2){
        if(!lock){
            if(r1.contains(cp)) hover = 1
            if(r2.contains(cp)) hover = 2
            if(r3.contains(cp)) hover = 3
            if(r4.contains(cp)) hover = 4
            if(hover >0){
                pt+=0.01f
                if(pt>=1f){
                    val ct = hover==q.ans
                    if(ct) sc+=10
                    else{
                        sc-=5
                        lock=true
                        locked = System.currentTimeMillis() + 3000
                        wrong++
                    }
                    pt = 0f
                    cp = Offset(w/2,h/2)
                    qIdx = Random.nextInt(5)
                    qqIdx++
                    if(qqIdx>=10){
                        page = 1
                    }
                }
            }else pt = 0f
        }
    }else{hover=0;pt=0f}
    LaunchedEffect(rx,ry,pt) {
        while (true){
            if(page == 2){
                if(lock && locked < System.currentTimeMillis()) lock = false
                cp = Offset((cp.x + rx - cal.x).coerceIn(0f,w-dot),(cp.y + ry - cal.y).coerceIn(0f,h-dot))
            }
            delay(16)
        }
    }
    Box(Modifier
        .fillMaxSize()
        .background(Color.White)){
        when(page){
            0->{
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(20.dp))
                    Text("Gyro Quiz", style = TextStyle(fontSize = 60.sp, fontWeight = FontWeight.Bold, color = Color.Black))
                    Spacer(Modifier.height(40.dp))
                    Button({page = 2}) { Text("開始", style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.Black)) }
                }
            }
            1->{
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(20.dp))
                    Text("結束", style = TextStyle(fontSize = 60.sp, fontWeight = FontWeight.Bold, color = Color.Black))
                    Spacer(Modifier.height(40.dp))
                    Text("得分：$sc", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black))
                    Spacer(Modifier.height(20.dp))
                    Text("正確率：${((10-wrong)/10f*100).toInt()}%", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black))
                    Spacer(Modifier.height(40.dp))
                    Button({page = 2;wrong=0;qqIdx=0;sc = 0}) { Text("重新開始", style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.Black)) }
                }
            }
            2->{
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(20.dp))
                    Text("題目", style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.Black))
                    Spacer(Modifier.height(20.dp))
                    Text(q.question, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black))
                    Spacer(Modifier.height(20.dp))
                    Text("得分：$sc", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black))
                    Spacer(Modifier.height(20.dp))
                    Text("進度：${qqIdx+1}/10", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black))
                    Spacer(Modifier.height(20.dp))
                    Column(Modifier
                        .fillMaxWidth()
                        .weight(2f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AA(Modifier
                                .weight(1f)
                                .onGloballyPositioned { r1 = it.boundsInRoot() },hover==1,q.selections[0])
                            AA(Modifier
                                .weight(1f)
                                .onGloballyPositioned { r2 = it.boundsInRoot() },hover==2,q.selections[1])
                        }
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AA(Modifier
                                .weight(1f)
                                .onGloballyPositioned { r3 = it.boundsInRoot() },hover==3,q.selections[2])
                            AA(Modifier
                                .weight(1f)
                                .onGloballyPositioned { r4 = it.boundsInRoot() },hover==4,q.selections[3])
                        }
                    }
                }
                Box(Modifier.fillMaxSize(),Alignment.BottomCenter){
                    Button({cp = Offset(w/2,h/2);cal= Offset(rx,ry)},) { Text("較整中心點") }
                }
                Box(Modifier
                    .size(30.dp)
                    .offset { IntOffset(cp.x.toInt(), cp.y.toInt()) }, Alignment.Center){
                    if(pt>0){
                        CircularProgressIndicator(
                            {pt},Modifier.fillMaxSize(), strokeWidth = 3.dp, color = Color.Red
                        )
                    }
                    Box(Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.Red))
                }
                if(lock){
                    Box(Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(0.7f)),Alignment.Center){
                        Text("LOCK", style = TextStyle(fontSize = 60.sp, fontWeight = FontWeight.Bold, color = Color.Red))
                    }
                }
            }
        }
    }
}

@Composable
fun AA(x0: Modifier, x1: Boolean, x2: String) {
    val scale by animateFloatAsState(if(x1) 1.1f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    Box(modifier = x0
        .fillMaxSize()
        .clip(RoundedCornerShape(20.dp))
        .background(if (x1) Color.LightGray else Color.Gray, shape = RoundedCornerShape(20.dp)),
        Alignment.Center){
        Text(x2, fontSize = 30.sp, fontWeight = FontWeight.Bold,color = Color.Black)
    }
}
