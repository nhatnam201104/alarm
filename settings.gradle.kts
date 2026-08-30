pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RiseAlarm"

include(
    ":app",
    ":ui-catalog",
    ":core:common",
    ":core:model",
    ":core:designsystem",
    ":core:navigation",
    ":core:testing",
    ":domain",
    ":data:local",
    ":engine:alarm",
    ":engine:vision",
    ":feature:onboarding",
    ":feature:home",
    ":feature:alarms",
    ":feature:protocol",
    ":feature:wake",
    ":feature:settings",
)
