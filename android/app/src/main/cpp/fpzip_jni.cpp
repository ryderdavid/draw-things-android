#include <jni.h>
#include <fpzip.h>
#include <cstdio>
#include <cstdint>
#include <vector>
#include <stdexcept>
#include <memory>
#include <mutex>

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_ryderdavid_drawremote_Fpzip_decodeNative(JNIEnv* env, jobject, jbyteArray input, jint expected) {
    // fpzip has global error state; serialize codec calls. No filesystem or inference engine.
    static std::mutex mutex;
    std::lock_guard<std::mutex> lock(mutex);
    try {
        const jsize length=env->GetArrayLength(input);
        if(length<16 || length>32*1024*1024 || expected<=0 || expected>4*4*1024*1024) throw std::runtime_error("Invalid compressed image bounds");
        std::vector<jbyte> data(length);env->GetByteArrayRegion(input,0,length,data.data());
        if(env->ExceptionCheck()) return nullptr;
        std::unique_ptr<FILE,decltype(&fclose)> file(fmemopen(data.data(),data.size(),"rb"),fclose);
        if(!file)throw std::runtime_error("Cannot open compressed image in memory");
        std::unique_ptr<FPZ,decltype(&fpzip_read_close)> fpz(fpzip_read_from_file(file.get()),fpzip_read_close);
        if(!fpz || !fpzip_read_header(fpz.get()))throw std::runtime_error("Invalid fpzip header");
        uint64_t count=1;
        for(int d:{fpz->nx,fpz->ny,fpz->nz,fpz->nf}) {
            if(d<=0 || d>expected || count>static_cast<uint64_t>(expected)/d)throw std::runtime_error("Invalid fpzip dimensions");
            count*=d;
        }
        if(count!=static_cast<uint64_t>(expected) || fpz->type!=FPZIP_TYPE_FLOAT || fpz->prec<0 || fpz->prec>32)throw std::runtime_error("Unsupported fpzip image format");
        std::vector<float> output(expected);
        if(!fpzip_read(fpz.get(),output.data()))throw std::runtime_error("Corrupt fpzip data");
        jfloatArray result=env->NewFloatArray(expected);
        if(result) env->SetFloatArrayRegion(result,0,expected,output.data());
        return result;
    } catch(const std::exception& e) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),e.what());return nullptr;
    }
}
