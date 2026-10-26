package com.ryderdavid.drawremote.protocol

import com.google.protobuf.ByteString
import com.ryderdavid.drawremote.wire.ImageService.*
import io.grpc.*
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.protobuf.lite.ProtoLiteUtils
import io.grpc.stub.ClientCalls
import io.grpc.stub.StreamObserver
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

class RemoteClient(val endpoint: Endpoint, private val decompress:((ByteArray,Int)->FloatArray)?=null) : AutoCloseable {
    data class Endpoint(val host:String, val port:Int=7859, val tls:Boolean=true, val secret:String="", val pin:String="") {
        init {
            require(host.isNotBlank() && !host.contains('/') && host.none { it.isWhitespace() }) { "Enter a hostname or IP, without a URL scheme" }
            require(port in 1..65535) { "Port must be 1–65535" }
            require(pin.isBlank() || pin.replace(":", "").matches(Regex("[a-fA-F0-9]{64}"))) { "Certificate pin must be a SHA-256 fingerprint" }
            require(tls || pin.isBlank()) { "Certificate pin requires TLS" }
        }
        fun json()=JSONObject().put("host",host).put("port",port).put("tls",tls).put("secret",secret).put("pin",pin)
        companion object { fun from(j:JSONObject)=Endpoint(j.getString("host"),j.getInt("port"),j.getBoolean("tls"),j.optString("secret"),j.optString("pin")) }
    }
    data class Asset(val file:String,val name:String,val metadata:JSONObject)
    data class Inventory(val models:List<Asset>,val loras:List<Asset>,val controls:List<Asset>,val upscalers:List<Asset>,val embeddings:List<Asset>,val serverId:Long)
    private val channel:ManagedChannel
    init {
        val builder=OkHttpChannelBuilder.forAddress(endpoint.host,endpoint.port).maxInboundMessageSize(TensorCodec.MAX_BYTES).disableRetry()
        if(!endpoint.tls) builder.usePlaintext() else if(endpoint.pin.isNotBlank()) {
            val expected=endpoint.pin.replace(":", "").lowercase()
            val trust=object:X509TrustManager {
                override fun getAcceptedIssuers()=emptyArray<X509Certificate>()
                override fun checkClientTrusted(chain:Array<X509Certificate>,authType:String)=throw java.security.cert.CertificateException("Client trust unsupported")
                override fun checkServerTrusted(chain:Array<X509Certificate>,authType:String) {
                    require(chain.isNotEmpty()); chain[0].checkValidity()
                    val actual=MessageDigest.getInstance("SHA-256").digest(chain[0].encoded).joinToString("") { "%02x".format(it) }
                    if(actual!=expected) throw java.security.cert.CertificateException("Server certificate pin mismatch")
                }
            }
            val context=SSLContext.getInstance("TLS"); context.init(null,arrayOf(trust),SecureRandom())
            builder.sslSocketFactory(context.socketFactory).hostnameVerifier { _, _ -> true } // Exact leaf pin is the explicitly configured identity.
        } else builder.useTransportSecurity()
        channel=builder.build()
    }
    fun connect():Inventory {
        val request=EchoRequest.newBuilder().setName("Draw Remote Android").apply { if(endpoint.secret.isNotEmpty()) sharedSecret=endpoint.secret }.build()
        val reply=ClientCalls.blockingUnaryCall(channel,ECHO,CallOptions.DEFAULT.withDeadlineAfter(12,TimeUnit.SECONDS),request)
        check(!reply.sharedSecretMissing) { "This Mac requires a shared secret" }
        val available=reply.filesList.toSet()
        fun assets(bytes:ByteString):List<Asset> {
            if(bytes.isEmpty) return emptyList()
            val array=JSONArray(bytes.toStringUtf8())
            return (0 until array.length()).map { array.getJSONObject(it) }.filter { it.optString("file") in available }.map {
                Asset(it.getString("file"),it.optString("name",it.getString("file")),it)
            }
        }
        val inventory=Inventory(assets(reply.override.models),assets(reply.override.loras),assets(reply.override.controlNets),assets(reply.override.upscalers),assets(reply.override.textualInversions),reply.serverIdentifier)
        check(inventory.models.isNotEmpty()) { "No installed models were advertised. Enable Model Browser in the Mac API server." }
        return inventory
    }
    fun generate(config:JSONObject,prompt:String,negative:String,input:ByteArray?,mask:ByteArray?,progress:(String)->Unit,done:(List<TensorCodec.Image>?,Throwable?)->Unit):ClientCall<ImageGenerationRequest,ImageGenerationResponse> {
        validate(config)
        val request=ImageGenerationRequest.newBuilder().setPrompt(prompt).setNegativePrompt(negative)
            .setConfiguration(ByteString.copyFrom(ConfigSchema.encode(config))).setScaleFactor(1).setUser("Draw Remote Android").setDevice(DeviceType.PHONE).setChunked(true)
        if(endpoint.secret.isNotEmpty()) request.sharedSecret=endpoint.secret
        fun content(bytes:ByteArray)=ByteString.copyFrom(MessageDigest.getInstance("SHA-256").digest(bytes)).also { request.addContents(ByteString.copyFrom(bytes)) }
        if(input!=null) request.image=content(input)
        if(mask!=null) { require(input!=null); request.mask=content(mask) }
        val call=channel.newCall(GENERATE,CallOptions.DEFAULT.withDeadlineAfter(20,TimeUnit.MINUTES))
        val result=mutableListOf<TensorCodec.Image>(); var chunk=ByteArrayOutputStream(); var totalPixels=0L; var terminal=false
        fun fail(t:Throwable) { if(!terminal) { terminal=true; call.cancel("Response validation failed",null); done(null,t) } }
        ClientCalls.asyncServerStreamingCall(call,request.build(),object:StreamObserver<ImageGenerationResponse> {
            override fun onNext(r:ImageGenerationResponse) {
                if(terminal)return
                try {
                    if(r.hasCurrentSignpost()) {
                        val s=r.currentSignpost
                        progress(when(s.signpostCase) {
                            ImageGenerationSignpostProto.SignpostCase.SAMPLING -> "Sampling ${s.sampling.step} / ${config.getInt("steps") }"
                            ImageGenerationSignpostProto.SignpostCase.SECONDPASSSAMPLING -> "Second pass ${s.secondPassSampling.step}"
                            else -> s.signpostCase.name.lowercase().replace('_',' ')
                        })
                    }
                    // Latent previews are deliberately not decoded by a learned model on Android.
                    if(r.chunkState==ChunkState.UNRECOGNIZED) error("Unknown result chunk state")
                    if(r.chunkState==ChunkState.MORE_CHUNKS) require(r.generatedImagesCount==1) { "Malformed partial result" }
                    r.generatedImagesList.forEachIndexed { i,bytes ->
                        require(chunk.size().toLong()+bytes.size()<=TensorCodec.MAX_BYTES) { "Result exceeds memory limit" }
                        chunk.write(bytes.toByteArray())
                        if(r.chunkState==ChunkState.LAST_CHUNK) {
                            require(result.size<4) { "Too many returned images" }
                            val decoded=TensorCodec.decode(chunk.toByteArray(),decompress); totalPixels+=decoded.argb.size
                            require(totalPixels<=TensorCodec.MAX_PIXELS) { "Batch exceeds memory limit" }
                            result.add(decoded); chunk=ByteArrayOutputStream()
                        }
                    }
                } catch(t:Exception) { fail(t) }
            }
            override fun onError(t:Throwable) { if(!terminal) { terminal=true; done(null,t) } }
            override fun onCompleted() {
                if(terminal)return
                if(chunk.size()!=0 || result.isEmpty()) { fail(IllegalStateException("Incomplete result from Mac"));return }
                terminal=true;done(result,null)
            }
        })
        return call
    }
    override fun close() { channel.shutdownNow() }
    companion object {
        private fun <Q:com.google.protobuf.MessageLite,R:com.google.protobuf.MessageLite> method(name:String,q:Q,r:R,type:MethodDescriptor.MethodType)=MethodDescriptor.newBuilder<Q,R>().setType(type).setFullMethodName("ImageGenerationService/$name").setRequestMarshaller(ProtoLiteUtils.marshaller(q)).setResponseMarshaller(ProtoLiteUtils.marshaller(r)).build()
        val ECHO=method("Echo",EchoRequest.getDefaultInstance(),EchoReply.getDefaultInstance(),MethodDescriptor.MethodType.UNARY)
        val GENERATE=method("GenerateImage",ImageGenerationRequest.getDefaultInstance(),ImageGenerationResponse.getDefaultInstance(),MethodDescriptor.MethodType.SERVER_STREAMING)
        fun defaults()=JSONObject().put("start_width",8).put("start_height",8).put("steps",20).put("seed",42L).put("guidance_scale",7.0).put("strength",1.0).put("sampler",0).put("batch_count",1).put("batch_size",1).put("seed_mode",2)
        fun validate(j:JSONObject) {
            require(j.optString("model").isNotBlank()) { "Choose a model installed on the Mac" }
            val w=j.getInt("start_width");val h=j.getInt("start_height")
            require(w in 1..64 && h in 1..64 && w.toLong()*h*4096<=TensorCodec.MAX_PIXELS) { "Resolution exceeds client limit" }
            require(j.getInt("steps") in 1..200) { "Steps must be 1–200" }
            val count=j.optInt("batch_count",1);val size=j.optInt("batch_size",1)
            require(count in 1..4 && size in 1..4 && count*size<=4 && count.toLong()*size*w*h*4096<=TensorCodec.MAX_PIXELS) { "Batch exceeds client limit" }
            require(j.optLong("seed",42) in 0..0xffffffffL) { "Seed must be an unsigned 32-bit number" }
            require(j.optDouble("strength",1.0) in 0.0..1.0) { "Strength must be 0–1" }
            require(j.optInt("sampler",0) in ConfigSchema.GenerationConfigurationFields.first { it.name=="sampler" }.options.indices)
            for(f in ConfigSchema.GenerationConfigurationFields) if(j.has(f.name) && f.type=="float") require(j.getDouble(f.name).isFinite()) { "${f.name} must be finite" }
        }
    }
}
