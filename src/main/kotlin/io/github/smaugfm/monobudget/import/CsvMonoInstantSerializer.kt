package io.github.smaugfm.monobudget.import

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atDate
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class CsvMonoInstantSerializer : KSerializer<Instant> {
    override val descriptor =
        PrimitiveSerialDescriptor(this::class.qualifiedName!!, PrimitiveKind.STRING)

    private val dateFormat =
        LocalDate.Format {
            dayOfMonth()
            char('.')
            monthNumber()
            char('.')
            year()
        }

    override fun deserialize(decoder: Decoder): Instant {
        val str = decoder.decodeString()

        val localdate = LocalDate.parse(str.substringBefore(" "), dateFormat)
        val localtime = LocalTime.parse(str.substringAfter(" "))

        return localtime.atDate(localdate).toInstant(TimeZone.currentSystemDefault())
    }

    override fun serialize(
        encoder: Encoder,
        value: Instant,
    ) {
        TODO("Not yet implemented")
    }
}
