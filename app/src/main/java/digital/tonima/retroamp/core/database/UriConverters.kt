package digital.tonima.retroamp.core.database

import android.net.Uri
import androidx.room.TypeConverter
import androidx.core.net.toUri

class UriConverters {
    @TypeConverter
    fun fromString(value: String?): Uri? {
        return value?.toUri()
    }

    @TypeConverter
    fun toString(uri: Uri?): String? {
        return uri?.toString()
    }
}
