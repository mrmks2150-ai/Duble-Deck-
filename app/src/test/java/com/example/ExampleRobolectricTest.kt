package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.browser.SearchEngine
import com.example.ui.browser.UrlUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DoubleDeck", appName)
  }

  @Test
  fun `test url resolution with domain`() {
    val resolved = UrlUtils.resolveUrl("wikipedia.org", SearchEngine.GOOGLE)
    assertEquals("https://wikipedia.org", resolved)
  }

  @Test
  fun `test url resolution with search query`() {
    val resolved = UrlUtils.resolveUrl("android split screen browser", SearchEngine.DUCKDUCKGO)
    assertTrue(resolved.startsWith("https://duckduckgo.com/?q="))
  }
}
