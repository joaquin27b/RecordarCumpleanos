package com.blair.cumpleanos.datos

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Persona::class, Cumple::class],
    version = 3,
    exportSchema = true,
)
abstract class BaseDatos : RoomDatabase() {
    abstract fun personaDao(): PersonaDao
    abstract fun cumpleDao(): CumpleDao
}
