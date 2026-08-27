package com.metrolist.music.photo

import android.app.Application
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.test.core.app.ApplicationProvider
import com.metrolist.music.ui.screens.Screens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class PhotoFrameNavigationTest {
    @Test
    fun `photo frame occupies third main menu slot`() {
        assertEquals(listOf("home", "search_input", "photo_frame", "library"), Screens.MainScreens.map { it.route })
    }

    @Test
    fun `fullscreen route keeps original tab for back and avoids duplicate entries`() {
        val controller = NavHostController(ApplicationProvider.getApplicationContext()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            graph = createGraph(startDestination = Screens.Home.route) {
                Screens.MainScreens.forEach { screen -> composable(screen.route) {} }
            }
        }
        controller.navigate(Screens.Library.route)
        repeat(2) { controller.navigate(Screens.PhotoFrame.route) { launchSingleTop = true } }
        assertEquals(Screens.PhotoFrame.route, controller.currentDestination?.route)
        assertTrue(controller.popBackStack())
        assertEquals(Screens.Library.route, controller.currentDestination?.route)
    }
}
