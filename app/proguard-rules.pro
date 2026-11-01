# Keep the framework-instantiated services so they are discoverable by the OS.
-keep class com.ekoehler.expressivecutout.service.** { *; }

# Shizuku's remote-process bridge is resolved reflectively for API-version compatibility.
-keep class rikka.shizuku.** { *; }
