import de.fayard.refreshVersions.bootstrapRefreshVersions
import de.fayard.refreshVersions.migrateRefreshVersionsIfNeeded

buildscript {
    repositories {
        gradlePluginPortal()
        jcenter()
    }
    dependencies.classpath("de.fayard.refreshVersions:refreshVersions:0.9.5")
// //                                                      # available:0.9.6")
// //                                                      # available:0.9.6")
}

migrateRefreshVersionsIfNeeded("0.9.5")
// Will be automatically removed by refreshVersions when upgraded to the latest version.

bootstrapRefreshVersions()

rootProject.name = "ynab-mono"
