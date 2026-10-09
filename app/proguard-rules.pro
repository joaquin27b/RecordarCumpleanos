# Room genera implementaciones por reflexion sobre los nombres de entidad.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# WorkManager instancia el Worker por nombre de clase desde el JobService.
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Los enums se serializan a INTEGER por su ordinal: renombrarlos rompe la DB.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# El JOIN de CumpleUi / PersonaConHistorial se mapea por nombre de campo.
-keep class com.blair.cumpleanos.datos.** { *; }
