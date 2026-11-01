# Keep the framework-instantiated services so they are discoverable by the OS.
-keep class com.ekoehler.expressivecutout.service.** { *; }

# Shizuku launches this Binder class in its UserService process by class name.
-keep class com.vikram.expressiveisland.system.ShizukuCommandService { *; }
