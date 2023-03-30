package io.github.smaugfm.monobudget.common.util

import io.github.smaugfm.monobudget.common.misc.MCC
import java.net.URL

fun resource(path: String): URL = MCC.javaClass.classLoader.getResource(path)!!

fun resourceAsString(path: String): String = resource(path).readText()
