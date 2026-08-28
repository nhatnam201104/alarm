package com.risealarm.core.navigation

interface RiseNavigator<Route : Any> {
    fun navigate(route: Route)
    fun back()
}

