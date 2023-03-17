package io.github.smaugfm.monobudget.util

import java.net.URL

fun resource(path: String): URL = MCC.javaClass.classLoader.getResource(path)!!

fun resourceAsString(path: String): String = resource(path).readText()
