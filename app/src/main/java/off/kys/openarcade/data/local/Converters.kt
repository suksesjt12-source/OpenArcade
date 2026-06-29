package off.kys.openarcade.data.local

import androidx.room.TypeConverter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json
import off.kys.openarcade.domain.model.GameCategory

class Converters {
    @TypeConverter
    fun fromCategory(category: GameCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): GameCategory = GameCategory.valueOf(value)

    @TypeConverter
    fun fromStringList(value: ImmutableList<String>): String = Json.encodeToString(value.toList())

    @TypeConverter
    fun toStringList(value: String): ImmutableList<String> = try {
        Json.decodeFromString<List<String>>(value).toImmutableList()
    } catch (_: Exception) {
        kotlinx.collections.immutable.persistentListOf()
    }
}
