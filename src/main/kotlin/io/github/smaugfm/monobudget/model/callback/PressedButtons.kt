package io.github.smaugfm.monobudget.model.callback

import kotlin.reflect.KClass

class PressedButtons(initial: CallbackType?) {
    constructor() : this(null)

    private val pressed: MutableSet<KClass<out CallbackType>> =
        if (initial != null) mutableSetOf(initial::class) else mutableSetOf()

    fun isPressed(cls: KClass<out CallbackType>) = cls in pressed

    operator fun invoke(button: KClass<out CallbackType>) {
        pressed.add(button)
    }
}
