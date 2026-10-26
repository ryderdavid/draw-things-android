#!/usr/bin/env python3
"""Development-only, bounded AX inspection of the running official Mac app."""
import ctypes as c, subprocess, sys
cf=c.CDLL('/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation')
ax=c.CDLL('/System/Library/Frameworks/ApplicationServices.framework/ApplicationServices')
P=c.c_void_p
cf.CFStringCreateWithCString.argtypes=[P,c.c_char_p,c.c_uint];cf.CFStringCreateWithCString.restype=P
cf.CFStringGetCString.argtypes=[P,c.c_char_p,c.c_long,c.c_uint];cf.CFStringGetCString.restype=c.c_bool
cf.CFArrayGetCount.argtypes=[P];cf.CFArrayGetCount.restype=c.c_long
cf.CFArrayGetValueAtIndex.argtypes=[P,c.c_long];cf.CFArrayGetValueAtIndex.restype=P
cf.CFGetTypeID.argtypes=[P];cf.CFGetTypeID.restype=c.c_ulong
cf.CFStringGetTypeID.restype=c.c_ulong
ax.AXUIElementCreateApplication.argtypes=[c.c_int];ax.AXUIElementCreateApplication.restype=P
ax.AXUIElementCopyAttributeValue.argtypes=[P,P,c.POINTER(P)];ax.AXUIElementCopyAttributeValue.restype=c.c_int
ax.AXUIElementPerformAction.argtypes=[P,P];ax.AXUIElementPerformAction.restype=c.c_int
ax.AXUIElementSetAttributeValue.argtypes=[P,P,P];ax.AXUIElementSetAttributeValue.restype=c.c_int
ax.AXUIElementSetMessagingTimeout.argtypes=[P,c.c_float]
def s(v):return cf.CFStringCreateWithCString(None,v.encode(),0x08000100)
def attr(el,key):
 out=P();return out.value if ax.AXUIElementCopyAttributeValue(el,s(key),c.byref(out))==0 else None
def string(v):
 if not v or cf.CFGetTypeID(v)!=cf.CFStringGetTypeID():return ''
 out=c.create_string_buffer(2048);cf.CFStringGetCString(v,out,2048,0x08000100);return out.value.decode(errors='replace')
def children(el):
 a=attr(el,'AXChildren');return [cf.CFArrayGetValueAtIndex(a,i) for i in range(cf.CFArrayGetCount(a))] if a else []
pid=int(subprocess.check_output(['pgrep','-x','DrawThings']).decode().strip())
root=ax.AXUIElementCreateApplication(pid);ax.AXUIElementSetMessagingTimeout(root,2)
def find_server(el):
 cs=children(el)
 if any(string(attr(x,'AXDescription'))=='API Server' for x in cs):return el
 for x in cs:
  found=find_server(x)
  if found:return found
 return None
def find_toggle(label):
 server=find_server(root)
 if not server:raise RuntimeError('API panel not found')
 cs=children(server)
 for i,x in enumerate(cs):
  if string(attr(x,'AXDescription'))==label:
   for y in [cs[i+1]]+children(cs[i+1]):
    if string(attr(y,'AXRole'))=='AXCheckBox':return y
 raise RuntimeError('Toggle not found')
if len(sys.argv)>3 and sys.argv[1]=='server-set':
 import time
 label=sys.argv[2];desired=sys.argv[3]
 el=find_toggle(label);ax.AXUIElementPerformAction(el,s('AXScrollToVisible'));time.sleep(0.3)
 el=find_toggle(label)
 if string(attr(el,'AXValue'))!=desired:ax.AXUIElementPerformAction(el,s('AXPress'));time.sleep(0.3)
 print(label+': '+string(attr(find_toggle(label),'AXValue')))
 sys.exit(0)
if len(sys.argv)>2:
 el=root
 for i in sys.argv[2].split('.'):el=children(el)[int(i)]
 if sys.argv[1]=='show':print(ax.AXUIElementPerformAction(el,s('AXScrollToVisible')))
 elif sys.argv[1]=='click':print(ax.AXUIElementPerformAction(el,s('AXPress')))
 elif sys.argv[1]=='actions':
  ax.AXUIElementCopyActionNames.argtypes=[P,c.POINTER(P)];out=P();ax.AXUIElementCopyActionNames(el,c.byref(out));print([string(cf.CFArrayGetValueAtIndex(out,i)) for i in range(cf.CFArrayGetCount(out))])
 elif sys.argv[1]=='value':
  v=attr(el,'AXValue');print(string(v) or '<non-string value>')
 elif sys.argv[1]=='set':print(ax.AXUIElementSetAttributeValue(el,s('AXValue'),s(sys.argv[3])))
else:
 def walk(el,path='',depth=0):
  if depth>35:return
  role=string(attr(el,'AXRole'));title=string(attr(el,'AXTitle'));desc=string(attr(el,'AXDescription'))
  if role in ('AXButton','AXCheckBox','AXPopUpButton','AXStaticText','AXTextField','AXRadioButton','AXRadioGroup','AXTabGroup','AXMenuItem'):
   value=string(attr(el,'AXValue')) if role=='AXStaticText' else ''
   print(path,role,title,'|',desc,'|',value[:160])
  for i,x in enumerate(children(el)):walk(x,(path+'.' if path else '')+str(i),depth+1)
 walk(root)
