#!/usr/bin/env python3
"""Regenerate client schema bindings; never imports an inference target."""
from pathlib import Path
import re, subprocess, tempfile
if subprocess.check_output(['flatc','--version'],text=True).strip() != 'flatc version 25.2.10':
 raise SystemExit('Use flatc 25.2.10 to match the committed bindings and runtime')
root=Path(__file__).resolve().parents[2]
source=root/'Libraries/DataModels/Sources/config.fbs'
out=root/'android/protocol/src/main/java'
text=source.read_text().replace(' (primary)','').replace(' (indexed)','')
with tempfile.TemporaryDirectory() as tmp:
 f=Path(tmp)/'config.fbs'; f.write_text('namespace com.ryderdavid.drawremote.schema;\n'+text)
 subprocess.run(['flatc','--java','-o',str(out),str(f)],check=True)
enums={m.group(1):[x.strip().rstrip(',') for x in m.group(2).splitlines() if x.strip()] for m in re.finditer(r'enum (\w+): \w+ \{(.*?)\}',text,re.S)}
tables={}
for m in re.finditer(r'table (\w+) \{(.*?)\}',text,re.S):
 fields=[]
 for line in m.group(2).splitlines():
  line=line.strip()
  if not line:continue
  a=re.match(r'(\w+): (\[?\w+\]?)(?: = ([\w.\-]+))?(?: \((\w+)\))?;',line)
  assert a,line
  name,typ,default,attr=a.groups()
  fields.append((name,typ,default,attr))
 tables[m.group(1)]=fields
k=['package com.ryderdavid.drawremote.protocol','', 'import com.google.flatbuffers.FlatBufferBuilder','import org.json.JSONObject','', '// Generated from Libraries/DataModels/Sources/config.fbs. Do not edit.', 'object ConfigSchema {','  data class Field(val name: String, val type: String, val default: String, val options: List<String> = emptyList())']
for table,fields in tables.items():
 k.append(f'  val {table}Fields = listOf(')
 for name,typ,default,attr in fields:
  if attr=='deprecated':continue
  d=default or ('""' if typ=='string' else 'false' if typ=='bool' else '0')
  if typ in enums:d=str(enums[typ].index(default)) if default else '0'
  d=d.replace('"','')
  opts='listOf('+','.join('"'+v+'"' for v in enums[typ])+')' if typ in enums else 'emptyList()'
  k.append(f'    Field("{name}", "{typ}", "{d}", {opts}),')
 k.append('  )')
 k.append(f'  fun encode{table}(b: FlatBufferBuilder, j: JSONObject): Int {{')
 for name,typ,default,attr in fields:
  if typ=='string':k.append(f'    val {name}Offset = j.optString("{name}").takeIf {{ it.isNotBlank() }}?.let {{ b.createString(it) }} ?: 0')
  elif typ.startswith('['):
   t=typ[1:-1]
   k.append(f'    val {name}Array = j.optJSONArray("{name}")')
   k.append(f'    val {name}Offsets = IntArray({name}Array?.length() ?: 0) {{ i -> '+(f'b.createString({name}Array!!.getString(i))' if t=='string' else f'encode{t}(b, {name}Array!!.getJSONObject(i))')+' }')
   k.append(f'    val {name}Offset = if ({name}Offsets.isEmpty()) 0 else {{ b.startVector(4, {name}Offsets.size, 4); {name}Offsets.reversed().forEach {{ b.addOffset(it) }}; b.endVector() }}')
 k.append(f'    b.startTable({len(fields)})')
 for i,(name,typ,default,attr) in enumerate(fields):
  if attr=='deprecated':continue
  if typ=='string' or typ.startswith('['):k.append(f'    b.addOffset({i}, {name}Offset, 0)');continue
  if typ in enums:
   d=enums[typ].index(default) if default else 0
   k.append(f'    b.addByte({i}, j.optInt("{name}", {d}).toByte(), {d})');continue
  if typ=='bool':k.append(f'    b.addBoolean({i}, j.optBoolean("{name}", {default or "false"}), {default or "false"})');continue
  if typ=='float':
   d=(default or '0'); d=d if '.' in d else d+'.0'
   k.append(f'    b.addFloat({i}, j.optDouble("{name}", {d}).toFloat(), {d})');continue
  method={'long':'Long','uint':'Int','int':'Int','ushort':'Short','ubyte':'Byte'}[typ]; d=default or '0'
  k.append(f'    b.add{method}({i}, j.optLong("{name}", {d}L).to{method}(), {d}'+('L' if typ=='long' else '')+')')
 k.extend(['    return b.endTable()','  }'])
k.extend(['  fun encode(j: JSONObject): ByteArray {','    val b = FlatBufferBuilder(2048)','    val root = encodeGenerationConfiguration(b, j)','    b.finish(root)','    return b.sizedByteArray()','  }','}'])
f=out/'com/ryderdavid/drawremote/protocol/ConfigSchema.kt';f.write_text('\n'.join(k)+'\n')
p=root/'Libraries/GRPC/Models/Sources/imageService/imageService.proto'
s=p.read_text().replace('syntax = "proto3";', 'syntax = "proto3";\noption java_package = "com.ryderdavid.drawremote.wire";\noption java_outer_classname = "ImageService";')
(root/'android/protocol/src/main/proto/imageService.proto').write_text(s)

# Normalize generated whitespace without changing wire definitions.
for generated in list((out/'com/ryderdavid/drawremote/schema').glob('*.java')) + [root/'android/protocol/src/main/proto/imageService.proto']:
 generated.write_text('\n'.join(line.rstrip() for line in generated.read_text().splitlines()).rstrip()+'\n')
