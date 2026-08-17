# androidx.startup instantiates this reflectively from a manifest meta-data string, so nothing in
# the bytecode references it and R8 would otherwise remove it.
-keep class dev.yjyoon.lineloginkmp.internal.LineLoginInitializer { <init>(); }

# Everything else this library needs is already covered:
#  - LineLoginProxyActivity is kept by AGP's own manifest-derived rules;
#  - the LINE SDK ships its own consumer rules inside linesdk.aar (proguard.txt), which AGP
#    applies transitively. Do not duplicate them here — they include app-wide keeps that are
#    LINE's to own, not this library's.
