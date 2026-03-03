package com.punitkumar.gruhkharch.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.punitkumar.gruhkharch.data.local.converter.Converters
import com.punitkumar.gruhkharch.data.local.dao.ExpenseDao
import com.punitkumar.gruhkharch.data.local.dao.ProjectDao
import com.punitkumar.gruhkharch.data.local.entity.ExpenseEntity
import com.punitkumar.gruhkharch.data.local.entity.ProjectEntity

@Database(
    entities = [ExpenseEntity::class, ProjectEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GruhKharchDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun projectDao(): ProjectDao
}
