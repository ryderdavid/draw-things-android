import com.google.protobuf.gradle.*
plugins { id("org.jetbrains.kotlin.jvm"); id("com.google.protobuf") }
kotlin { jvmToolchain(17) }
dependencies {
    api("io.grpc:grpc-okhttp:1.69.1")
    api("io.grpc:grpc-protobuf-lite:1.69.1")
    api("io.grpc:grpc-stub:1.69.1")
    api("com.google.protobuf:protobuf-javalite:3.25.5")
    implementation("com.google.flatbuffers:flatbuffers-java:25.2.10")
    compileOnly("org.json:json:20240303")
    testImplementation("org.json:json:20240303")
    testImplementation("junit:junit:4.13.2")
}
protobuf {
    protoc { artifact = "com.google.protobuf:protoc:3.25.5" }
    generateProtoTasks { all().forEach { task -> task.builtins { named("java") { option("lite") } } } }
}
