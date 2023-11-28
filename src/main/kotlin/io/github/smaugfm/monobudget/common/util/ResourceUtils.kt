package io.github.smaugfm.monobudget.common.util

import java.net.URL

fun resource(path: String): URL = MCCRegistry.javaClass.classLoader.getResource(path)!!

fun resourceAsString(path: String): String = resource(path).readText()
