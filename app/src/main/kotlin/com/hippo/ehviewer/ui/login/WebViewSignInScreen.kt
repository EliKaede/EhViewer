package com.hippo.ehviewer.ui.login

import android.webkit.CookieManager
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.ui.destinations.FinishDestination
import com.hippo.ehviewer.ui.destinations.SelectSiteScreenDestination
import com.hippo.ehviewer.util.setDefaultSettings
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withNonCancellableContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import okhttp3.HttpUrl.Companion.toHttpUrl

@Destination
@Composable
fun WebViewSignInScreen(navigator: DestinationsNavigator) {
    val coroutineScope = rememberCoroutineScope()
    val state = rememberWebViewState(url = EhUrl.URL_SIGN_IN)
    val client = remember {
        object : AccompanistWebViewClient() {
            private var present = false
            override fun onPageFinished(view: WebView, url: String?) {
                if (present) {
                    view.destroy()
                    return
                }
                var getId = false
                var getHash = false
                EhCookieStore.loadForRequest(EhUrl.HOST_E.toHttpUrl()).forEach {
                    if (EhCookieStore.KEY_IPB_MEMBER_ID == it.name) {
                        getId = true
                    } else if (EhCookieStore.KEY_IPB_PASS_HASH == it.name) {
                        getHash = true
                    }
                }
                if (getId && getHash) {
                    present = true
                    coroutineScope.launchIO {
                        val canEx = withNonCancellableContext { postLogin() }
                        withUIContext { navigator.navigate(if (canEx) SelectSiteScreenDestination else FinishDestination) }
                    }
                }
            }
        }
    }
    SideEffect {
        EhUtils.signOut()
        CookieManager.getInstance().apply {
            flush()
            removeAllCookies(null)
            removeSessionCookies(null)
        }
    }
    WebView(
        state = state,
        modifier = Modifier.fillMaxSize(),
        onCreated = {
            it.setDefaultSettings()
        },
        client = client,
    )
}
