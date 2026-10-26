package com.ryderdavid.drawremote

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ryderdavid.drawremote.protocol.*
import io.grpc.ClientCall
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID

class WorkspaceModel(app:Application):AndroidViewModel(app) {
    val vault=PrivateVault(app)
    var endpoint by mutableStateOf<RemoteClient.Endpoint?>(null)
    var inventory by mutableStateOf<RemoteClient.Inventory?>(null)
    var connected by mutableStateOf(false)
    var busy by mutableStateOf(false)
    var connecting by mutableStateOf(false)
    var status by mutableStateOf("Connect to your Mac to begin")
    var error by mutableStateOf<String?>(null)
    var prompt by mutableStateOf("")
    var negative by mutableStateOf("")
    var config by mutableStateOf(RemoteClient.defaults())
    var input by mutableStateOf<Bitmap?>(null)
    var mask by mutableStateOf<ByteArray?>(null)
    var displayed by mutableStateOf<Bitmap?>(null)
    var history by mutableStateOf<List<Pair<String,JSONObject>>>(emptyList())
    var verifiedBefore by mutableStateOf(false)
    private var client:RemoteClient?=null
    private var call:ClientCall<*,*>?=null
    private var generation=0
    private var loaded=false
    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val saved=vault.read(PrivateVault.CONNECTION)?.first
                val draft=vault.read(PrivateVault.DRAFT)
                val job=vault.read(PrivateVault.JOB)?.first
                val records=vault.images()
                withContext(Dispatchers.Main) {
                    if(saved!=null) { endpoint=RemoteClient.Endpoint.from(saved);verifiedBefore=true;status="Reconnect to your Mac" }
                    if(draft!=null) { prompt=draft.first.optString("prompt");negative=draft.first.optString("negative");config=draft.first.optJSONObject("config")?:RemoteClient.defaults();if(draft.second.isNotEmpty()) { val decoded=decodeDraft(draft.second);input=decoded.first;mask=decoded.second } }
                    history=records;loaded=true
                    if(job?.optString("state")=="running") { status="Previous run interrupted; Mac outcome unknown. Reconnect before an explicit retry." }
                }
            } catch(t:Exception) { withContext(Dispatchers.Main) { error="Cannot open private library: ${t.message}" } }
        }
    }
    fun update(name:String,value:Any) { if(!busy) config=JSONObject(config.toString()).put(name,value) }
    fun connect(target:RemoteClient.Endpoint) {
        if(busy||connecting)return
        connected=false;connecting=true;error=null;client?.close();client=null
        viewModelScope.launch {
            var candidate:RemoteClient?=null
            try {
                val inv=withContext(Dispatchers.IO) { candidate=RemoteClient(target,Fpzip::decode);val inv=candidate!!.connect();vault.write(PrivateVault.CONNECTION,target.json());inv }
                client=candidate;endpoint=target;inventory=inv;connected=true;verifiedBefore=true;status="Connected · ${inv.models.size} models on Mac"
                if(inv.models.none { it.file==config.optString("model") }) update("model",inv.models.first().file)
            } catch(t:Exception) { candidate?.close();status="Connection failed";error=t.message ?: "Cannot connect to Mac" }
            finally { connecting=false }
        }
    }
    fun disconnect() { if(busy)return;client?.close();client=null;connected=false;inventory=null;status="Disconnected" }
    fun saveDraft() {
        if(!loaded)return
        val meta=JSONObject().put("prompt",prompt).put("negative",negative).put("config",config)
        val currentInput=input;val currentMask=mask?.copyOf()
        viewModelScope.launch(Dispatchers.IO) {
            try { vault.write(PrivateVault.DRAFT,meta,encodeDraft(currentInput,currentMask)) }
            catch(t:Exception) { withContext(Dispatchers.Main) { error="Draft could not be saved: ${t.message}" } }
        }
    }
    fun generate() {
        val remote=client?:return
        if(!connected||busy)return
        val settings=JSONObject(config.toString());val text=prompt;val neg=negative;val source=input;val currentMask=mask?.copyOf()
        try {
            RemoteClient.validate(settings)
            check(inventory!!.models.any { it.file==settings.optString("model") }) { "Selected model is not available on this Mac" }
            check(settings.optDouble("strength",1.0)==1.0 || source!=null) { "Image-to-image strength requires an input image" }
        } catch(t:Exception) { error=t.message;return }
        busy=true;error=null;status="Sending to Mac";val token=++generation
        viewModelScope.launch {
            try {
                val prepared=withContext(Dispatchers.IO) {
                    vault.write(PrivateVault.DRAFT,JSONObject().put("prompt",text).put("negative",neg).put("config",settings),encodeDraft(source,currentMask))
                    vault.write(PrivateVault.JOB,JSONObject().put("state","running").put("started",System.currentTimeMillis()))
                    val image=source?.let { Bitmap.createScaledBitmap(it,settings.getInt("start_width")*64,settings.getInt("start_height")*64,true) }
                    val tensor=image?.let { val pixels=IntArray(it.width*it.height);it.getPixels(pixels,0,it.width,0,0,it.width,it.height);TensorCodec.encode(TensorCodec.Image(it.width,it.height,pixels)) }
                    val encodedMask=if(image!=null && currentMask!=null) TensorCodec.encodeMask(image.width,image.height,currentMask) else null
                    tensor to encodedMask
                }
                if(token!=generation)return@launch
                call=remote.generate(settings,text,neg,prepared.first,prepared.second,{ message -> viewModelScope.launch { if(token==generation)status=message } },{ images,failure ->
                    viewModelScope.launch {
                        if(token!=generation)return@launch
                        if(failure!=null) { connected=false;status="Run interrupted; Mac outcome may be unknown";error=failure.message?:"Generation failed";busy=false;call=null;return@launch }
                        status="Saving to private library"
                        try {
                            val bitmaps=withContext(Dispatchers.IO) {
                                images!!.map { image ->
                                    val bitmap=Bitmap.createBitmap(image.argb,image.width,image.height,Bitmap.Config.ARGB_8888)
                                    val metadata=JSONObject().put("prompt",text).put("negative",neg).put("config",settings).put("created",System.currentTimeMillis()).put("width",image.width).put("height",image.height)
                                    vault.write(UUID.randomUUID().toString(),metadata,png(bitmap));bitmap
                                }.also { vault.write(PrivateVault.JOB,JSONObject().put("state","saved")) }
                            }
                            displayed=bitmaps.first();refreshHistory();status="Saved ${bitmaps.size} image${if(bitmaps.size>1) "s" else ""} in your private library"
                        } catch(t:Exception) { status="Save failed";error="Mac finished, but private storage failed: ${t.message}" }
                        finally { busy=false;call=null }
                    }
                })
            } catch(t:Exception) { busy=false;status="Could not start generation";error=t.message }
        }
    }
    fun stop() { if(!busy)return;generation++;call?.cancel("User stopped",null);call=null;busy=false;status="Stopped receiving. Mac termination is not confirmed.";saveDraft() }
    fun refreshHistory() { viewModelScope.launch { try { history=withContext(Dispatchers.IO) { vault.images() } } catch(t:Exception) { error=t.message } } }
    fun openImage(id:String,reuse:Boolean=false) { viewModelScope.launch { try {
        val record=withContext(Dispatchers.IO) { vault.read(id) }?:return@launch
        val bitmap=withContext(Dispatchers.IO) { BitmapFactory.decodeByteArray(record.second,0,record.second.size) }
        displayed=bitmap
        if(reuse && !busy) { prompt=record.first.optString("prompt");negative=record.first.optString("negative");config=record.first.getJSONObject("config");input=bitmap;mask=null }
    } catch(t:Exception) { error=t.message } } }
    fun deleteImage(id:String) { viewModelScope.launch { try { withContext(Dispatchers.IO) { vault.delete(id) };displayed=null;refreshHistory() } catch(t:Exception) { error=t.message } } }
    fun importImage(bytes:ByteArray) { viewModelScope.launch { try {
        val bitmap=withContext(Dispatchers.IO) {
            require(bytes.size<=32*1024*1024) { "Input exceeds 32 MB" }
            val info=BitmapFactory.Options().apply { inJustDecodeBounds=true };BitmapFactory.decodeByteArray(bytes,0,bytes.size,info)
            require(info.outWidth>0 && info.outHeight>0) { "Unsupported image" }
            val decode=BitmapFactory.Options();while(info.outWidth.toLong()*info.outHeight/(decode.inSampleSize.coerceAtLeast(1).toLong()*decode.inSampleSize.coerceAtLeast(1))>TensorCodec.MAX_PIXELS)decode.inSampleSize=decode.inSampleSize.coerceAtLeast(1)*2
            BitmapFactory.decodeByteArray(bytes,0,bytes.size,decode)?:error("Cannot decode input")
        }
        input=bitmap;mask=null;saveDraft()
    } catch(t:Exception) { error=t.message } } }
    override fun onCleared() { call?.cancel("Workspace closed",null);client?.close() }
    companion object {
        fun encodeDraft(bitmap:Bitmap?,mask:ByteArray?):ByteArray {
            if(bitmap==null)return byteArrayOf()
            val image=png(bitmap)
            return ByteArrayOutputStream().use { output -> java.io.DataOutputStream(output).use { it.writeInt(image.size);it.write(image);if(mask!=null)it.write(mask) };output.toByteArray() }
        }
        fun decodeDraft(bytes:ByteArray):Pair<Bitmap?,ByteArray?> {
            if(bytes.isEmpty())return null to null
            return java.io.DataInputStream(java.io.ByteArrayInputStream(bytes)).use { stream ->
                val size=stream.readInt();require(size>0 && size<=bytes.size-4)
                val image=ByteArray(size);stream.readFully(image)
                val mask=stream.readBytes().takeIf { it.isNotEmpty() }
                BitmapFactory.decodeByteArray(image,0,image.size) to mask
            }
        }
        fun png(bitmap:Bitmap)=ByteArrayOutputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it));it.toByteArray() } }
}
