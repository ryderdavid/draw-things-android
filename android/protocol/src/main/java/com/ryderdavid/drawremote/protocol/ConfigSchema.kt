package com.ryderdavid.drawremote.protocol

import com.google.flatbuffers.FlatBufferBuilder
import org.json.JSONObject

// Generated from Libraries/DataModels/Sources/config.fbs. Do not edit.
object ConfigSchema {
  data class Field(val name: String, val type: String, val default: String, val options: List<String> = emptyList())
  val ControlFields = listOf(
    Field("file", "string", "", emptyList()),
    Field("weight", "float", "1", emptyList()),
    Field("guidance_start", "float", "0", emptyList()),
    Field("guidance_end", "float", "1", emptyList()),
    Field("no_prompt", "bool", "false", emptyList()),
    Field("global_average_pooling", "bool", "true", emptyList()),
    Field("down_sampling_rate", "float", "1", emptyList()),
    Field("control_mode", "ControlMode", "0", listOf("Balanced","Prompt","Control")),
    Field("target_blocks", "[string]", "0", emptyList()),
    Field("input_override", "ControlInputType", "0", listOf("Unspecified","Custom","Depth","Canny","Scribble","Pose","Normalbae","Color","Lineart","Softedge","Seg","Inpaint","Ip2p","Shuffle","Mlsd","Tile","Blur","Lowquality","Gray")),
  )
  fun encodeControl(b: FlatBufferBuilder, j: JSONObject): Int {
    val fileOffset = j.optString("file").takeIf { it.isNotBlank() }?.let { b.createString(it) } ?: 0
    val target_blocksArray = j.optJSONArray("target_blocks")
    val target_blocksOffsets = IntArray(target_blocksArray?.length() ?: 0) { i -> b.createString(target_blocksArray!!.getString(i)) }
    val target_blocksOffset = if (target_blocksOffsets.isEmpty()) 0 else { b.startVector(4, target_blocksOffsets.size, 4); target_blocksOffsets.reversed().forEach { b.addOffset(it) }; b.endVector() }
    b.startTable(10)
    b.addOffset(0, fileOffset, 0)
    b.addFloat(1, j.optDouble("weight", 1.0).toFloat(), 1.0)
    b.addFloat(2, j.optDouble("guidance_start", 0.0).toFloat(), 0.0)
    b.addFloat(3, j.optDouble("guidance_end", 1.0).toFloat(), 1.0)
    b.addBoolean(4, j.optBoolean("no_prompt", false), false)
    b.addBoolean(5, j.optBoolean("global_average_pooling", true), true)
    b.addFloat(6, j.optDouble("down_sampling_rate", 1.0).toFloat(), 1.0)
    b.addByte(7, j.optInt("control_mode", 0).toByte(), 0)
    b.addOffset(8, target_blocksOffset, 0)
    b.addByte(9, j.optInt("input_override", 0).toByte(), 0)
    return b.endTable()
  }
  val LoRAFields = listOf(
    Field("file", "string", "", emptyList()),
    Field("weight", "float", "0.6", emptyList()),
    Field("mode", "LoRAMode", "0", listOf("All","Base","Refiner")),
  )
  fun encodeLoRA(b: FlatBufferBuilder, j: JSONObject): Int {
    val fileOffset = j.optString("file").takeIf { it.isNotBlank() }?.let { b.createString(it) } ?: 0
    b.startTable(3)
    b.addOffset(0, fileOffset, 0)
    b.addFloat(1, j.optDouble("weight", 0.6).toFloat(), 0.6)
    b.addByte(2, j.optInt("mode", 0).toByte(), 0)
    return b.endTable()
  }
  val GenerationConfigurationFields = listOf(
    Field("id", "long", "0", emptyList()),
    Field("start_width", "ushort", "0", emptyList()),
    Field("start_height", "ushort", "0", emptyList()),
    Field("seed", "uint", "0", emptyList()),
    Field("steps", "uint", "0", emptyList()),
    Field("guidance_scale", "float", "0", emptyList()),
    Field("strength", "float", "0", emptyList()),
    Field("model", "string", "", emptyList()),
    Field("sampler", "SamplerType", "0", listOf("DPMPP2MKarras","EulerA","DDIM","PLMS","DPMPPSDEKarras","UniPC","LCM","EulerASubstep","DPMPPSDESubstep","TCD","EulerATrailing","DPMPPSDETrailing","DPMPP2MAYS","EulerAAYS","DPMPPSDEAYS","DPMPP2MTrailing","DDIMTrailing","UniPCTrailing","UniPCAYS","TCDTrailing")),
    Field("batch_count", "uint", "1", emptyList()),
    Field("batch_size", "uint", "1", emptyList()),
    Field("hires_fix", "bool", "false", emptyList()),
    Field("hires_fix_start_width", "ushort", "0", emptyList()),
    Field("hires_fix_start_height", "ushort", "0", emptyList()),
    Field("hires_fix_strength", "float", "0.7", emptyList()),
    Field("upscaler", "string", "", emptyList()),
    Field("image_guidance_scale", "float", "1.5", emptyList()),
    Field("seed_mode", "SeedMode", "0", listOf("Legacy","TorchCpuCompatible","ScaleAlike","NvidiaGpuCompatible")),
    Field("clip_skip", "uint", "1", emptyList()),
    Field("controls", "[Control]", "0", emptyList()),
    Field("loras", "[LoRA]", "0", emptyList()),
    Field("mask_blur", "float", "0", emptyList()),
    Field("face_restoration", "string", "", emptyList()),
    Field("clip_weight", "float", "1", emptyList()),
    Field("negative_prompt_for_image_prior", "bool", "true", emptyList()),
    Field("image_prior_steps", "uint", "5", emptyList()),
    Field("refiner_model", "string", "", emptyList()),
    Field("original_image_height", "uint", "0", emptyList()),
    Field("original_image_width", "uint", "0", emptyList()),
    Field("crop_top", "int", "0", emptyList()),
    Field("crop_left", "int", "0", emptyList()),
    Field("target_image_height", "uint", "0", emptyList()),
    Field("target_image_width", "uint", "0", emptyList()),
    Field("aesthetic_score", "float", "6", emptyList()),
    Field("negative_aesthetic_score", "float", "2.5", emptyList()),
    Field("zero_negative_prompt", "bool", "false", emptyList()),
    Field("refiner_start", "float", "0.7", emptyList()),
    Field("negative_original_image_height", "uint", "0", emptyList()),
    Field("negative_original_image_width", "uint", "0", emptyList()),
    Field("name", "string", "", emptyList()),
    Field("fps_id", "uint", "5", emptyList()),
    Field("motion_bucket_id", "uint", "127", emptyList()),
    Field("cond_aug", "float", "0.02", emptyList()),
    Field("start_frame_cfg", "float", "1.0", emptyList()),
    Field("num_frames", "uint", "14", emptyList()),
    Field("mask_blur_outset", "int", "0", emptyList()),
    Field("sharpness", "float", "0", emptyList()),
    Field("shift", "float", "1.0", emptyList()),
    Field("stage_2_steps", "uint", "10", emptyList()),
    Field("stage_2_cfg", "float", "1.0", emptyList()),
    Field("stage_2_shift", "float", "1.0", emptyList()),
    Field("tiled_decoding", "bool", "false", emptyList()),
    Field("decoding_tile_width", "ushort", "10", emptyList()),
    Field("decoding_tile_height", "ushort", "10", emptyList()),
    Field("decoding_tile_overlap", "ushort", "2", emptyList()),
    Field("stochastic_sampling_gamma", "float", "0.3", emptyList()),
    Field("preserve_original_after_inpaint", "bool", "true", emptyList()),
    Field("tiled_diffusion", "bool", "false", emptyList()),
    Field("diffusion_tile_width", "ushort", "16", emptyList()),
    Field("diffusion_tile_height", "ushort", "16", emptyList()),
    Field("diffusion_tile_overlap", "ushort", "2", emptyList()),
    Field("upscaler_scale_factor", "ubyte", "0", emptyList()),
    Field("t5_text_encoder", "bool", "true", emptyList()),
    Field("separate_clip_l", "bool", "false", emptyList()),
    Field("clip_l_text", "string", "", emptyList()),
    Field("separate_open_clip_g", "bool", "false", emptyList()),
    Field("open_clip_g_text", "string", "", emptyList()),
    Field("speed_up_with_guidance_embed", "bool", "true", emptyList()),
    Field("guidance_embed", "float", "3.5", emptyList()),
    Field("resolution_dependent_shift", "bool", "true", emptyList()),
    Field("tea_cache_start", "int", "5", emptyList()),
    Field("tea_cache_end", "int", "-1", emptyList()),
    Field("tea_cache_threshold", "float", "0.06", emptyList()),
    Field("tea_cache", "bool", "false", emptyList()),
    Field("separate_t5", "bool", "false", emptyList()),
    Field("t5_text", "string", "", emptyList()),
    Field("tea_cache_max_skip_steps", "int", "3", emptyList()),
    Field("causal_inference_enabled", "bool", "false", emptyList()),
    Field("causal_inference", "int", "3", emptyList()),
    Field("causal_inference_pad", "int", "0", emptyList()),
    Field("cfg_zero_star", "bool", "false", emptyList()),
    Field("cfg_zero_init_steps", "int", "0", emptyList()),
    Field("compression_artifacts", "CompressionMethod", "0", listOf("Disabled","H264","H265","Jpeg")),
    Field("compression_artifacts_quality", "float", "43.1", emptyList()),
    Field("color_calibration", "ColorCalibration", "0", listOf("Disabled","Lab")),
    Field("expand_prompt_to_json", "bool", "false", emptyList()),
    Field("shift_for_audio", "float", "3.0", emptyList()),
    Field("uses_sol_attention", "bool", "false", emptyList()),
    Field("sol_attention_start", "int", "2", emptyList()),
    Field("sol_attention_tau", "float", "0.5", emptyList()),
  )
  fun encodeGenerationConfiguration(b: FlatBufferBuilder, j: JSONObject): Int {
    val modelOffset = j.optString("model").takeIf { it.isNotBlank() }?.let { b.createString(it) } ?: 0
    val upscalerOffset = j.optString("upscaler").takeIf { it.isNotBlank() }?.let { b.createString(it) } ?: 0
    val controlsArray = j.optJSONArray("controls")
    val controlsOffsets = IntArray(controlsArray?.length() ?: 0) { i -> encodeControl(b, controlsArray!!.getJSONObject(i)) }
    val controlsOffset = if (controlsOffsets.isEmpty()) 0 else { b.startVector(4, controlsOffsets.size, 4); controlsOffsets.reversed().forEach { b.addOffset(it) }; b.endVector() }
    val lorasArray = j.optJSONArray("loras")
    val lorasOffsets = IntArray(lorasArray?.length() ?: 0) { i -> encodeLoRA(b, lorasArray!!.getJSONObject(i)) }
    val lorasOffset = if (lorasOffsets.isEmpty()) 0 else { b.startVector(4, lorasOffsets.size, 4); lorasOffsets.reversed().forEach { b.addOffset(it) }; b.endVector() }
    val face_restorationOffset = j.optString("face_restoration").takeIf { it.isNotBlank() }?.let { b.createString(it) } ?: 0
    val refiner_modelOffset = j.optString("refiner_model").takeIf { it.isNotBlank() }?.let { b.createString(it) } ?: 0
    val nameOffset = j.optString("name").takeIf { it.isNotBlank() }?.let { b.createString(it) } ?: 0
    val clip_l_textOffset = j.optString("clip_l_text").takeIf { it.isNotBlank() }?.let { b.createString(it) } ?: 0
    val open_clip_g_textOffset = j.optString("open_clip_g_text").takeIf { it.isNotBlank() }?.let { b.createString(it) } ?: 0
    val t5_textOffset = j.optString("t5_text").takeIf { it.isNotBlank() }?.let { b.createString(it) } ?: 0
    b.startTable(92)
    b.addLong(0, j.optLong("id", 0L).toLong(), 0L)
    b.addShort(1, j.optLong("start_width", 0L).toShort(), 0)
    b.addShort(2, j.optLong("start_height", 0L).toShort(), 0)
    b.addInt(3, j.optLong("seed", 0L).toInt(), 0)
    b.addInt(4, j.optLong("steps", 0L).toInt(), 0)
    b.addFloat(5, j.optDouble("guidance_scale", 0.0).toFloat(), 0.0)
    b.addFloat(6, j.optDouble("strength", 0.0).toFloat(), 0.0)
    b.addOffset(7, modelOffset, 0)
    b.addByte(8, j.optInt("sampler", 0).toByte(), 0)
    b.addInt(9, j.optLong("batch_count", 1L).toInt(), 1)
    b.addInt(10, j.optLong("batch_size", 1L).toInt(), 1)
    b.addBoolean(11, j.optBoolean("hires_fix", false), false)
    b.addShort(12, j.optLong("hires_fix_start_width", 0L).toShort(), 0)
    b.addShort(13, j.optLong("hires_fix_start_height", 0L).toShort(), 0)
    b.addFloat(14, j.optDouble("hires_fix_strength", 0.7).toFloat(), 0.7)
    b.addOffset(15, upscalerOffset, 0)
    b.addFloat(16, j.optDouble("image_guidance_scale", 1.5).toFloat(), 1.5)
    b.addByte(17, j.optInt("seed_mode", 0).toByte(), 0)
    b.addInt(18, j.optLong("clip_skip", 1L).toInt(), 1)
    b.addOffset(19, controlsOffset, 0)
    b.addOffset(20, lorasOffset, 0)
    b.addFloat(21, j.optDouble("mask_blur", 0.0).toFloat(), 0.0)
    b.addOffset(22, face_restorationOffset, 0)
    b.addFloat(25, j.optDouble("clip_weight", 1.0).toFloat(), 1.0)
    b.addBoolean(26, j.optBoolean("negative_prompt_for_image_prior", true), true)
    b.addInt(27, j.optLong("image_prior_steps", 5L).toInt(), 5)
    b.addOffset(28, refiner_modelOffset, 0)
    b.addInt(29, j.optLong("original_image_height", 0L).toInt(), 0)
    b.addInt(30, j.optLong("original_image_width", 0L).toInt(), 0)
    b.addInt(31, j.optLong("crop_top", 0L).toInt(), 0)
    b.addInt(32, j.optLong("crop_left", 0L).toInt(), 0)
    b.addInt(33, j.optLong("target_image_height", 0L).toInt(), 0)
    b.addInt(34, j.optLong("target_image_width", 0L).toInt(), 0)
    b.addFloat(35, j.optDouble("aesthetic_score", 6.0).toFloat(), 6.0)
    b.addFloat(36, j.optDouble("negative_aesthetic_score", 2.5).toFloat(), 2.5)
    b.addBoolean(37, j.optBoolean("zero_negative_prompt", false), false)
    b.addFloat(38, j.optDouble("refiner_start", 0.7).toFloat(), 0.7)
    b.addInt(39, j.optLong("negative_original_image_height", 0L).toInt(), 0)
    b.addInt(40, j.optLong("negative_original_image_width", 0L).toInt(), 0)
    b.addOffset(41, nameOffset, 0)
    b.addInt(42, j.optLong("fps_id", 5L).toInt(), 5)
    b.addInt(43, j.optLong("motion_bucket_id", 127L).toInt(), 127)
    b.addFloat(44, j.optDouble("cond_aug", 0.02).toFloat(), 0.02)
    b.addFloat(45, j.optDouble("start_frame_cfg", 1.0).toFloat(), 1.0)
    b.addInt(46, j.optLong("num_frames", 14L).toInt(), 14)
    b.addInt(47, j.optLong("mask_blur_outset", 0L).toInt(), 0)
    b.addFloat(48, j.optDouble("sharpness", 0.0).toFloat(), 0.0)
    b.addFloat(49, j.optDouble("shift", 1.0).toFloat(), 1.0)
    b.addInt(50, j.optLong("stage_2_steps", 10L).toInt(), 10)
    b.addFloat(51, j.optDouble("stage_2_cfg", 1.0).toFloat(), 1.0)
    b.addFloat(52, j.optDouble("stage_2_shift", 1.0).toFloat(), 1.0)
    b.addBoolean(53, j.optBoolean("tiled_decoding", false), false)
    b.addShort(54, j.optLong("decoding_tile_width", 10L).toShort(), 10)
    b.addShort(55, j.optLong("decoding_tile_height", 10L).toShort(), 10)
    b.addShort(56, j.optLong("decoding_tile_overlap", 2L).toShort(), 2)
    b.addFloat(57, j.optDouble("stochastic_sampling_gamma", 0.3).toFloat(), 0.3)
    b.addBoolean(58, j.optBoolean("preserve_original_after_inpaint", true), true)
    b.addBoolean(59, j.optBoolean("tiled_diffusion", false), false)
    b.addShort(60, j.optLong("diffusion_tile_width", 16L).toShort(), 16)
    b.addShort(61, j.optLong("diffusion_tile_height", 16L).toShort(), 16)
    b.addShort(62, j.optLong("diffusion_tile_overlap", 2L).toShort(), 2)
    b.addByte(63, j.optLong("upscaler_scale_factor", 0L).toByte(), 0)
    b.addBoolean(64, j.optBoolean("t5_text_encoder", true), true)
    b.addBoolean(65, j.optBoolean("separate_clip_l", false), false)
    b.addOffset(66, clip_l_textOffset, 0)
    b.addBoolean(67, j.optBoolean("separate_open_clip_g", false), false)
    b.addOffset(68, open_clip_g_textOffset, 0)
    b.addBoolean(69, j.optBoolean("speed_up_with_guidance_embed", true), true)
    b.addFloat(70, j.optDouble("guidance_embed", 3.5).toFloat(), 3.5)
    b.addBoolean(71, j.optBoolean("resolution_dependent_shift", true), true)
    b.addInt(72, j.optLong("tea_cache_start", 5L).toInt(), 5)
    b.addInt(73, j.optLong("tea_cache_end", -1L).toInt(), -1)
    b.addFloat(74, j.optDouble("tea_cache_threshold", 0.06).toFloat(), 0.06)
    b.addBoolean(75, j.optBoolean("tea_cache", false), false)
    b.addBoolean(76, j.optBoolean("separate_t5", false), false)
    b.addOffset(77, t5_textOffset, 0)
    b.addInt(78, j.optLong("tea_cache_max_skip_steps", 3L).toInt(), 3)
    b.addBoolean(79, j.optBoolean("causal_inference_enabled", false), false)
    b.addInt(80, j.optLong("causal_inference", 3L).toInt(), 3)
    b.addInt(81, j.optLong("causal_inference_pad", 0L).toInt(), 0)
    b.addBoolean(82, j.optBoolean("cfg_zero_star", false), false)
    b.addInt(83, j.optLong("cfg_zero_init_steps", 0L).toInt(), 0)
    b.addByte(84, j.optInt("compression_artifacts", 0).toByte(), 0)
    b.addFloat(85, j.optDouble("compression_artifacts_quality", 43.1).toFloat(), 43.1)
    b.addByte(86, j.optInt("color_calibration", 0).toByte(), 0)
    b.addBoolean(87, j.optBoolean("expand_prompt_to_json", false), false)
    b.addFloat(88, j.optDouble("shift_for_audio", 3.0).toFloat(), 3.0)
    b.addBoolean(89, j.optBoolean("uses_sol_attention", false), false)
    b.addInt(90, j.optLong("sol_attention_start", 2L).toInt(), 2)
    b.addFloat(91, j.optDouble("sol_attention_tau", 0.5).toFloat(), 0.5)
    return b.endTable()
  }
  fun encode(j: JSONObject): ByteArray {
    val b = FlatBufferBuilder(2048)
    val root = encodeGenerationConfiguration(b, j)
    b.finish(root)
    return b.sizedByteArray()
  }
}
