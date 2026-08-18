package com.hninakari.saletracker.sales.data.local

import androidx.room.TypeConverter
import com.hninakari.saletracker.sales.data.models.PaymentMethod
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime): String {
        return dateTime.format(formatter)
    }
    
    @TypeConverter
    fun toLocalDateTime(dateTimeString: String): LocalDateTime {
        return LocalDateTime.parse(dateTimeString, formatter)
    }
    
    @TypeConverter
    fun fromPaymentMethod(method: PaymentMethod): String {
        return method.name
    }
    
    @TypeConverter
    fun toPaymentMethod(methodString: String): PaymentMethod {
        return PaymentMethod.valueOf(methodString)
    }
}
