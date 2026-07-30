package com.metrolist.music.ui.screens

import android.app.Application
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.toRoute
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Serializable
internal data object NavigationTestStartDestination

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class NavigationDestinationsTest {
    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        navController =
            NavHostController(ApplicationProvider.getApplicationContext()).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
                graph =
                    createGraph(startDestination = NavigationTestStartDestination) {
                        composable<NavigationTestStartDestination> {}
                        composable<ArtistItemsDestination> {}
                        composable<YouTubeBrowseDestination> {}
                    }
            }
    }

    @Test
    fun `artist items preserves reserved characters in route arguments`() {
        val destination =
            ArtistItemsDestination(
                artistId = "artist/with?reserved&characters",
                browseId = "browse?section=albums&sort=newest",
                params = "EgWKAQI%2F+value=#fragment",
            )

        navController.navigate(destination)

        assertEquals(
            destination,
            navController.currentBackStackEntry?.toRoute<ArtistItemsDestination>(),
        )
    }

    @Test
    fun `youtube browse preserves optional parameters`() {
        val destination =
            YouTubeBrowseDestination(
                browseId = "FEmusic_moods_and_genres",
                params = "params/with?query=one&next=two#section",
            )

        navController.navigate(destination)

        assertEquals(
            destination,
            navController.currentBackStackEntry?.toRoute<YouTubeBrowseDestination>(),
        )
    }

    @Test
    fun `youtube browse supports an absent params value`() {
        val destination = YouTubeBrowseDestination(browseId = "FEmusic_library_non_music_audio_list")

        navController.navigate(destination)

        assertEquals(
            destination,
            navController.currentBackStackEntry?.toRoute<YouTubeBrowseDestination>(),
        )
    }
}
