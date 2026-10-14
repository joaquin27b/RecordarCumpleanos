package com.blair.cumpleanos.datos

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Columnas presentes en una tabla, para que cada ALTER sea idempotente. */
private fun columnas(db: SupportSQLiteDatabase, tabla: String): Set<String> =
    db.query("PRAGMA table_info(`$tabla`)").use { c ->
        val nombre = c.getColumnIndexOrThrow("name")
        buildSet { while (c.moveToNext()) add(c.getString(nombre)) }
    }

/**
 * v1 -> v2: regalo planeado, regalo real y fecha de festejo en cada ocurrencia.
 *
 * La fecha de festejo entra en ESTA migracion y no en una 2->3: la v2 no se
 * publico nunca, asi que ninguna base de usuario quedo en el 2 viejo.
 *
 * Solo ADD COLUMN: v1 esta publicada y las filas de los testers no se tocan.
 * Nullable, sin DEFAULT: las filas viejas quedan en NULL, que la UI lee
 * como "sin regalo anotado" y "sin festejo aparte". Chequea PRAGMA table_info antes de cada ALTER
 * para que reintentarla sobre una base a medio migrar no tire "duplicate column".
 */
val MIGRACION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val existentes = columnas(db, "cumple")
        if ("regaloPlan" !in existentes) db.execSQL("ALTER TABLE `cumple` ADD COLUMN `regaloPlan` TEXT")
        if ("regaloReal" !in existentes) db.execSQL("ALTER TABLE `cumple` ADD COLUMN `regaloReal` TEXT")
        if ("fechaFestejoDia" !in existentes) db.execSQL("ALTER TABLE `cumple` ADD COLUMN `fechaFestejoDia` INTEGER")
    }
}
