package io.github.smaugfm.monobudget.common.model.serializer

import com.livefront.sealedenum.GenSealedEnum
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SpringLikeDurationDeserializer : KSerializer<Duration> {
    override val descriptor = PrimitiveSerialDescriptor(this::class.qualifiedName!!, PrimitiveKind.STRING)
    private val simpleDurationPattern = "^([+-]?\\d+)([a-zA-Z]{0,2})$".toRegex()

    override fun deserialize(decoder: Decoder): Duration {
        val str = decoder.decodeString()
        try {
            val result = simpleDurationPattern.matchEntire(str)
            require(result != null) { "Does not match simple duration pattern" }
            val suffix = result.groups[2]
            val unit =
                if (suffix != null && suffix.value.isNotEmpty()) {
                    TimeUnit.fromSuffix(suffix.value)
                } else {
                    TimeUnit.SECONDS
                }
            return unit.parse(result.groups[1]!!.value)
        } catch (e: Throwable) {
            throw IllegalArgumentException("'$str' is not a valid simple duration", e)
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: Duration,
    ) {
        throw NotImplementedError("unsupported")
    }

    sealed class TimeUnit(
        private val unit: DurationUnit,
        private val suffix: String,
    ) {
        data object NANOS : TimeUnit(DurationUnit.NANOSECONDS, "ns")

        data object MICROS : TimeUnit(DurationUnit.MICROSECONDS, "us")

        data object MILLIS : TimeUnit(DurationUnit.MILLISECONDS, "ms")

        data object SECONDS : TimeUnit(DurationUnit.SECONDS, "s")

        data object MINUTES : TimeUnit(DurationUnit.MINUTES, "m")

        data object HOURS : TimeUnit(DurationUnit.HOURS, "h")

        data object DAYS : TimeUnit(DurationUnit.DAYS, "d")

        fun parse(value: String) = value.toLong().toDuration(unit)

        @GenSealedEnum
        companion object {
            fun fromSuffix(suffix: String): TimeUnit {
                return TimeUnit.values.firstOrNull { it.suffix.equals(suffix, ignoreCase = true) }
                    ?: throw IllegalArgumentException("Unknown unit '$suffix'")
            }
        }
    }
}
