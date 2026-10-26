package com.ryderdavid.drawremote

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.ryderdavid.drawremote.protocol.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date
import kotlin.math.*

class MainActivity:ComponentActivity() {
    private val model:WorkspaceModel by viewModels()
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xffb3dcc5),secondary=Color(0xffacc9e7),background=Color(0xff101714),surface=Color(0xff18221d),surfaceVariant=Color(0xff26352c))) {
                Surface(Modifier.fillMaxSize()) { Workspace(model) }
            }
        }
    }
    override fun onStop() { model.saveDraft();super.onStop() }
}

@Composable private fun Workspace(m:WorkspaceModel) {
    var settings by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var advanced by remember { mutableStateOf(false) }
    var maskEditor by remember { mutableStateOf(false) }
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri!=null)scope.launch {
            try {
                val bytes=withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)!!.use { stream ->
                    val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var n=stream.read(buffer)
                    while(n!=-1) { require(out.size()+n<=32*1024*1024) { "Input exceeds 32 MB" };out.write(buffer,0,n);n=stream.read(buffer) };out.toByteArray()
                } };m.importImage(bytes)
            } catch(t:Exception) { m.error=t.message }
        }
    }
    if(!m.verifiedBefore || settings) {
        ConnectionScreen(m,onDismiss={settings=false},onConnected={settings=false})
        return
    }
    if(advanced) AdvancedDialog(m) { advanced=false }
    if(maskEditor) MaskDialog(m) { maskEditor=false }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(20.dp,12.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("DRAW REMOTE",fontSize=12.sp,letterSpacing=3.sp,color=MaterialTheme.colorScheme.primary)
                Text("Your creative workspace",fontSize=22.sp,fontWeight=FontWeight.Medium)
            }
            TextButton(onClick={settings=true},enabled=!m.busy) { Text(if(m.connected) "Mac connected" else "Connect Mac") }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
            FilterChip(tab==0,{tab=0},label={Text("Create")});Spacer(Modifier.width(8.dp))
            FilterChip(tab==1,{tab=1;m.refreshHistory()},label={Text("Private library · ${m.history.size}")})
        }
        if(m.error!=null) Card(Modifier.fillMaxWidth().padding(horizontal=16.dp)) {
            Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) { Text(m.error!!,Modifier.weight(1f),color=MaterialTheme.colorScheme.error);TextButton(onClick={m.error=null}) { Text("Dismiss") } }
        }
        if(tab==1) {
            Library(m,Modifier.weight(1f));return@Column
        }
        BoxWithConstraints(Modifier.weight(1f)) {
            if(maxWidth>=800.dp) Row(Modifier.fillMaxSize()) {
                Controls(m,{advanced=true},{importer.launch("image/*")},{maskEditor=true},Modifier.width(370.dp))
                Preview(m,Modifier.weight(1f).fillMaxHeight())
            } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Preview(m,Modifier.fillMaxWidth().height(280.dp))
                Controls(m,{advanced=true},{importer.launch("image/*")},{maskEditor=true},Modifier.fillMaxWidth(),scroll=false)
            }
        }
        Surface(tonalElevation=5.dp) {
            Column(Modifier.padding(16.dp)) {
                Text(m.status,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(m.busy)LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=8.dp))
                Row(Modifier.padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Button(onClick={m.generate()},enabled=m.connected&&!m.busy,modifier=Modifier.weight(1f)) { Text("Generate on Mac") }
                    if(m.busy)OutlinedButton(onClick={m.stop()}) { Text("Stop") }
                    else OutlinedButton(onClick={m.saveDraft()}) { Text("Save draft") }
                }
            }
        }
    }
}

@Composable private fun ConnectionScreen(m:WorkspaceModel,onDismiss:()->Unit,onConnected:()->Unit) {
    var host by remember(m.endpoint) { mutableStateOf(m.endpoint?.host ?: "") }
    var port by remember(m.endpoint) { mutableStateOf((m.endpoint?.port?:7859).toString()) }
    var tls by remember(m.endpoint) { mutableStateOf(m.endpoint?.tls?:true) }
    var secret by remember(m.endpoint) { mutableStateOf(m.endpoint?.secret?:"") }
    var pin by remember(m.endpoint) { mutableStateOf(m.endpoint?.pin?:"") }
    var attempted by remember { mutableStateOf(false) }
    LaunchedEffect(m.connected) { if(m.connected&&attempted)onConnected() }
    Box(Modifier.fillMaxSize().safeDrawingPadding(),contentAlignment=Alignment.Center) {
        Column(Modifier.widthIn(max=520.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(28.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("DRAW REMOTE",letterSpacing=4.sp,color=MaterialTheme.colorScheme.primary,fontSize=13.sp)
            Text("Connect to your Mac",fontSize=34.sp,fontWeight=FontWeight.SemiBold)
            Text("Your Mac creates the images. Your private library stays encrypted on this device.",color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(host,{host=it},label={Text("Mac hostname or IP")},singleLine=true,modifier=Modifier.fillMaxWidth(),enabled=!m.connecting)
            OutlinedTextField(port,{port=it},label={Text("Port")},singleLine=true,modifier=Modifier.fillMaxWidth(),enabled=!m.connecting)
            Row(verticalAlignment=Alignment.CenterVertically) { Switch(tls,{tls=it},enabled=!m.connecting);Spacer(Modifier.width(12.dp));Text("TLS encrypted connection") }
            if(!tls)Text("Plaintext sends prompts and the shared secret without transport encryption. Use only a trusted private or encrypted VPN route.",color=MaterialTheme.colorScheme.error,fontSize=12.sp)
            OutlinedTextField(secret,{secret=it},label={Text("Shared secret (if required)")},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth(),enabled=!m.connecting)
            if(tls)OutlinedTextField(pin,{pin=it},label={Text("Certificate SHA-256 pin (optional)")},supportingText={Text("For a private certificate, verify its fingerprint on your Mac before entering it here.")},modifier=Modifier.fillMaxWidth(),enabled=!m.connecting)
            Text("On the Mac: enable API Server, choose gRPC, enable Model Browser, and keep Bridge Mode off.",fontSize=12.sp)
            if(m.error!=null)Text(m.error!!,color=MaterialTheme.colorScheme.error)
            Button(onClick={try { attempted=true;m.connect(RemoteClient.Endpoint(host.trim(),port.toInt(),tls,secret,if(tls)pin.trim() else "")) } catch(t:Exception) { m.error=t.message?:"Invalid connection" }},enabled=!m.connecting,modifier=Modifier.fillMaxWidth()) { Text(if(m.connecting) "Connecting…" else "Connect to Mac") }
            if(m.connecting)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(m.verifiedBefore)TextButton(onClick=onDismiss,modifier=Modifier.fillMaxWidth()) { Text("Open private workspace") }
        }
    }
}

@Composable private fun Controls(m:WorkspaceModel,advanced:()->Unit,import:()->Unit,mask:()->Unit,modifier:Modifier,scroll:Boolean=true) {
    Column((if(scroll)modifier.verticalScroll(rememberScrollState()) else modifier).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("COMPOSITION",letterSpacing=2.sp,fontSize=11.sp,color=MaterialTheme.colorScheme.primary)
        OutlinedTextField(m.prompt,{if(!m.busy)m.prompt=it},label={Text("Describe your image")},minLines=3,modifier=Modifier.fillMaxWidth(),enabled=!m.busy)
        OutlinedTextField(m.negative,{if(!m.busy)m.negative=it},label={Text("Negative prompt")},minLines=2,modifier=Modifier.fillMaxWidth(),enabled=!m.busy)
        AssetPicker("Model",m.config.optString("model"),m.inventory?.models.orEmpty(),!m.busy) { m.update("model",it) }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            NumericField("Width",(m.config.optInt("start_width",8)*64).toString(),Modifier.weight(1f),!m.busy) { it.toIntOrNull()?.takeIf { n->n in 64..4096&&n%64==0 }?.let { n->m.update("start_width",n/64);m.mask=null } }
            NumericField("Height",(m.config.optInt("start_height",8)*64).toString(),Modifier.weight(1f),!m.busy) { it.toIntOrNull()?.takeIf { n->n in 64..4096&&n%64==0 }?.let { n->m.update("start_height",n/64);m.mask=null } }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            NumericField("Steps",m.config.optInt("steps",20).toString(),Modifier.weight(1f),!m.busy) { it.toIntOrNull()?.let { n->m.update("steps",n) } }
            NumericField("Guidance",m.config.optDouble("guidance_scale",7.0).toString(),Modifier.weight(1f),!m.busy) { it.toDoubleOrNull()?.let { n->m.update("guidance_scale",n) } }
        }
        NumericField("Seed",m.config.optLong("seed",42).toString(),Modifier.fillMaxWidth(),!m.busy) { it.toLongOrNull()?.let { n->m.update("seed",n) } }
        val samplers=ConfigSchema.GenerationConfigurationFields.first { it.name=="sampler" }.options
        Choice("Sampler",samplers.getOrElse(m.config.optInt("sampler")){"Unknown"},samplers,!m.busy) { m.update("sampler",samplers.indexOf(it)) }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick=import,enabled=!m.busy,modifier=Modifier.weight(1f)) { Text("Import image") }
            if(m.input!=null)OutlinedButton(onClick=mask,enabled=!m.busy) { Text("Paint mask") }
        }
        if(m.input!=null) {
            NumericField("Image strength (0–1)",m.config.optDouble("strength",1.0).toString(),Modifier.fillMaxWidth(),!m.busy) { it.toDoubleOrNull()?.let { n->m.update("strength",n) } }
            TextButton(onClick={m.input=null;m.mask=null;m.update("strength",1.0)},enabled=!m.busy) { Text("Remove input image") }
        }
        Button(onClick=advanced,enabled=!m.busy,modifier=Modifier.fillMaxWidth()) { Text("Advanced controls & LoRAs") }
        Text("Images are saved only in this app. Uninstalling the app removes access to its encrypted library.",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun NumericField(label:String,value:String,modifier:Modifier=Modifier,enabled:Boolean=true,change:(String)->Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(text,{text=it;change(it)},label={Text(label)},singleLine=true,modifier=modifier,enabled=enabled)
}
@Composable private fun Choice(label:String,selected:String,options:List<String>,enabled:Boolean=true,choose:(String)->Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick={open=true},enabled=enabled,modifier=Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth()) { Text(label,fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(selected.ifBlank { "Choose…" },maxLines=2) } }
    if(open)AlertDialog(onDismissRequest={open=false},title={Text(label)},text={LazyColumn(Modifier.heightIn(max=420.dp)) { items(options) { value -> TextButton(onClick={choose(value);open=false},modifier=Modifier.fillMaxWidth()) { Text(value) } } }},confirmButton={TextButton(onClick={open=false}){Text("Close")}})
}
@Composable private fun AssetPicker(label:String,selected:String,assets:List<RemoteClient.Asset>,enabled:Boolean=true,choose:(String)->Unit) {
    val names=assets.map { "${it.name} · ${it.file}" }
    Choice(label,assets.firstOrNull { it.file==selected }?.name?:selected,names,enabled&&assets.isNotEmpty()) { choose(assets[names.indexOf(it)].file) }
}

@Composable private fun Preview(m:WorkspaceModel,modifier:Modifier) {
    var zoom by remember { mutableFloatStateOf(1f) };var x by remember { mutableFloatStateOf(0f) };var y by remember { mutableFloatStateOf(0f) }
    val bitmap=m.displayed?:m.input
    LaunchedEffect(bitmap) { zoom=1f;x=0f;y=0f }
    Box(modifier.padding(16.dp).clipToBounds().background(Color(0xff0b100d),RoundedCornerShape(20.dp)).pointerInput(bitmap) { detectTransformGestures { _,pan,scale,_ -> zoom=(zoom*scale).coerceIn(1f,6f);x+=pan.x;y+=pan.y } },contentAlignment=Alignment.Center) {
        if(bitmap!=null)Image(bitmap.asImageBitmap(),"Private image preview",Modifier.fillMaxSize().graphicsLayer { scaleX=zoom;scaleY=zoom;translationX=x;translationY=y },contentScale=ContentScale.Fit)
        else Column(horizontalAlignment=Alignment.CenterHorizontally) { Text("A space for your next idea",fontSize=21.sp,color=Color(0xffb3c4b9));Text("Create on your Mac. Keep it here.",fontSize=13.sp,color=Color(0xff7f9787)) }
    }
}

@Composable private fun AdvancedDialog(m:WorkspaceModel,close:()->Unit) {
    var query by remember { mutableStateOf("") }
    Dialog(onDismissRequest=close) {
        Surface(shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth().fillMaxHeight(.92f)) {
            Column(Modifier.padding(16.dp)) {
                Text("Advanced controls",fontSize=22.sp)
                OutlinedTextField(query,{query=it},label={Text("Find a setting")},modifier=Modifier.fillMaxWidth())
                LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    item { WeightedAssets(m,"loras","LoRAs",m.inventory?.loras.orEmpty()) }
                    item { WeightedAssets(m,"controls","Controls",m.inventory?.controls.orEmpty()) }
                    item { Text("Controls requiring depth, pose, or other learned preprocessing need a server-supported input path. No preprocessing models run here.",fontSize=11.sp) }
                    items(ConfigSchema.GenerationConfigurationFields.filter { !it.type.startsWith('[')&&it.name!="id"&&it.name.contains(query,true) }) { f ->
                        val label=f.name.replace('_',' ')
                        when {
                            f.name=="model" -> AssetPicker("Model",m.config.optString(f.name),m.inventory?.models.orEmpty()) { m.update(f.name,it) }
                            f.name=="upscaler" -> AssetPicker("Upscaler",m.config.optString(f.name),m.inventory?.upscalers.orEmpty()) { m.update(f.name,it) }
                            f.name=="refiner_model" -> AssetPicker("Refiner",m.config.optString(f.name),m.inventory?.models.orEmpty()) { m.update(f.name,it) }
                            f.type=="bool" -> Row(verticalAlignment=Alignment.CenterVertically) { Switch(m.config.optBoolean(f.name,f.default=="true"),{m.update(f.name,it)});Text(label) }
                            f.options.isNotEmpty() -> Choice(label,f.options.getOrElse(m.config.optInt(f.name,f.default.toInt())) { "Unknown" },f.options) { m.update(f.name,f.options.indexOf(it)) }
                            else -> NumericField(label,m.config.optString(f.name,f.default),Modifier.fillMaxWidth()) { v -> when(f.type) { "string"->m.update(f.name,v);"float"->v.toDoubleOrNull()?.let { m.update(f.name,it) };else->v.toLongOrNull()?.let { m.update(f.name,it) } } }
                        }
                    }
                }
                TextButton(onClick={m.saveDraft();close()},modifier=Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}
@Composable private fun WeightedAssets(m:WorkspaceModel,key:String,label:String,assets:List<RemoteClient.Asset>) {
    val array=m.config.optJSONArray(key)?:JSONArray()
    Text(label,fontWeight=FontWeight.Bold)
    for(i in 0 until array.length()) {
        val item=array.getJSONObject(i)
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(assets.firstOrNull { it.file==item.optString("file") }?.name?:item.optString("file"),fontSize=12.sp)
                NumericField("Weight",item.optDouble("weight",1.0).toString()) { value -> value.toDoubleOrNull()?.let { val copy=JSONArray(array.toString());copy.getJSONObject(i).put("weight",it);m.update(key,copy) } }
            }
            TextButton(onClick={val copy=JSONArray(array.toString());copy.remove(i);m.update(key,copy)}) { Text("Remove") }
        }
    }
    AssetPicker("Add $label","",assets) { file -> val copy=JSONArray(array.toString());copy.put(JSONObject().put("file",file).put("weight",1.0));m.update(key,copy) }
}

@Composable private fun Library(m:WorkspaceModel,modifier:Modifier) {
    var delete by remember { mutableStateOf<String?>(null) }
    if(delete!=null)AlertDialog(onDismissRequest={delete=null},title={Text("Delete private image?")},text={Text("This removes this app’s saved copy and its metadata.")},confirmButton={TextButton(onClick={m.deleteImage(delete!!);delete=null}){Text("Delete")}},dismissButton={TextButton(onClick={delete=null}){Text("Cancel")}})
    Column(modifier) {
        if(m.displayed!=null)Preview(m,Modifier.fillMaxWidth().height(280.dp))
        if(m.history.isEmpty())Text("Your private library is empty. Completed images will appear here.",Modifier.padding(24.dp))
        LazyColumn(Modifier.weight(1f).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            items(m.history,key={it.first}) { (id,meta) ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                    Text(meta.optString("prompt").ifBlank { "Untitled image" },maxLines=2)
                    Text("${meta.optInt("width")} × ${meta.optInt("height")} · ${DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(meta.optLong("created")))}",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Row { TextButton(onClick={m.openImage(id)}){Text("View")};TextButton(onClick={m.openImage(id,true)},enabled=!m.busy){Text("Reuse")};TextButton(onClick={delete=id}){Text("Delete")} }
                } }
            }
        }
    }
}
@Composable private fun MaskDialog(m:WorkspaceModel,close:()->Unit) {
    val w=m.config.optInt("start_width",8)*64;val h=m.config.optInt("start_height",8)*64
    if(w.toLong()*h>TensorCodec.MAX_PIXELS){m.error="Canvas exceeds memory limit";close();return}
    var revision by remember { mutableIntStateOf(0) }
    val pixels=remember(w,h) { m.mask?.takeIf { it.size==w*h }?.copyOf()?:ByteArray(w*h) }
    var brush by remember { mutableFloatStateOf(24f) };var erase by remember { mutableStateOf(false) }
    Dialog(onDismissRequest=close) { Surface(shape=RoundedCornerShape(20.dp)) { Column(Modifier.padding(16.dp)) {
        Text("Paint the area to change",fontSize=20.sp);Text("Brush marks use the configured image strength.",fontSize=12.sp)
        Box(Modifier.fillMaxWidth().aspectRatio(w.toFloat()/h).background(Color.Black)) {
            m.input?.let { Image(it.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds) }
            Canvas(Modifier.fillMaxSize().pointerInput(brush,erase) {
                fun paint(x:Float,y:Float) { val cx=(x/size.width*w).toInt();val cy=(y/size.height*h).toInt();val radius=brush.toInt();for(yy in max(0,cy-radius)..min(h-1,cy+radius))for(xx in max(0,cx-radius)..min(w-1,cx+radius))if((xx-cx).toLong()*(xx-cx)+(yy-cy).toLong()*(yy-cy)<=radius*radius)pixels[yy*w+xx]=if(erase)0 else 2;revision++ }
                detectDragGestures(onDragStart={paint(it.x,it.y)}) { change,_ -> change.consume();paint(change.position.x,change.position.y) }
            }) {
                revision
                val bitmap=android.graphics.Bitmap.createBitmap(w,h,android.graphics.Bitmap.Config.ARGB_8888)
                bitmap.setPixels(IntArray(w*h) { if(pixels[it].toInt()==0)0 else 0x66b3dcc5 },0,w,0,0,w,h)
                drawImage(bitmap.asImageBitmap(),dstSize=androidx.compose.ui.unit.IntSize(size.width.toInt(),size.height.toInt()))
            }
        }
        Slider(brush,{brush=it},valueRange=4f..100f)
        Row { FilterChip(erase,{erase=!erase},label={Text("Eraser")});TextButton(onClick={pixels.fill(0);revision++}){Text("Clear")};TextButton(onClick={m.mask=pixels.copyOf();m.saveDraft();close()}){Text("Use mask")} }
    } } }
}
