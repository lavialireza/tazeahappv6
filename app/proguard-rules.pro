
# Tazieh Viewer hardening: keep Android/Room/Compose reflection metadata as needed.
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
-dontwarn javax.annotation.**
