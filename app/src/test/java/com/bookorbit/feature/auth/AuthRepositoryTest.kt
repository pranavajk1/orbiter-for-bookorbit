package com.bookorbit.feature.auth

import com.bookorbit.core.auth.SessionManager
import com.bookorbit.core.model.AuthUser
import com.bookorbit.core.model.LoginOptions
import com.bookorbit.core.model.LoginResponse
import com.bookorbit.core.model.OidcProviderPublic
import com.bookorbit.core.model.ServerIdentity
import com.bookorbit.core.model.SetupStatus
import com.bookorbit.core.network.ApiService
import com.bookorbit.core.sync.SyncScheduler
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AuthRepositoryTest {

    private lateinit var api: ApiService
    private lateinit var repo: AuthRepository

    private val authentik = OidcProviderPublic(
        slug = "authentik",
        displayName = "Authentik",
        enabled = true,
        clientId = "client",
        scopes = "openid",
    )

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    @Before
    fun setUp() {
        api = mockk()
        repo = AuthRepository(api, mockk<SessionManager>(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
    }

    @Test
    fun `login options pass DISABLE_LOCAL_AUTH through`() = runTest {
        coEvery { api.getLoginOptions() } returns LoginOptions(passwordLoginEnabled = false, oidcProviders = listOf(authentik))

        val options = repo.loginOptions()

        assertFalse(options.passwordLoginEnabled)
        assertEquals(listOf(authentik), options.oidcProviders)
    }

    @Test
    fun `a server without login-options gets password sign-in and its public providers`() = runTest {
        coEvery { api.getLoginOptions() } throws httpError(404)
        coEvery { api.getPublicOidcProviders() } returns listOf(authentik)

        val options = repo.loginOptions()

        assertTrue(options.passwordLoginEnabled)
        assertEquals(listOf(authentik), options.oidcProviders)
    }

    @Test(expected = IOException::class)
    fun `an unreachable server is an error, not a guess`() = runTest {
        coEvery { api.getLoginOptions() } throws IOException("offline")
        repo.loginOptions()
    }

    @Test
    fun `a BookOrbit server passes the check`() = runTest {
        coEvery { api.getServerIdentity() } returns ServerIdentity(product = "bookorbit", protocolVersion = 1)
        repo.checkServer()
    }

    @Test
    fun `an older BookOrbit server passes on setup-status`() = runTest {
        coEvery { api.getServerIdentity() } throws httpError(404)
        coEvery { api.getSetupStatus() } returns SetupStatus(needsSetup = false)
        repo.checkServer()
    }

    @Test(expected = NotBookOrbitServerException::class)
    fun `a web page at the address fails the check`() = runTest {
        coEvery { api.getServerIdentity() } throws SerializationException("not json")
        repo.checkServer()
    }

    @Test(expected = NotBookOrbitServerException::class)
    fun `another product fails the check`() = runTest {
        coEvery { api.getServerIdentity() } returns ServerIdentity(product = "something-else")
        repo.checkServer()
    }

    @Test(expected = NotBookOrbitServerException::class)
    fun `a 404 everywhere fails the check`() = runTest {
        coEvery { api.getServerIdentity() } throws httpError(404)
        coEvery { api.getSetupStatus() } throws httpError(404)
        repo.checkServer()
    }

    @Test
    fun `signing in sends what was queued while signed out`() = runTest {
        val user = AuthUser(id = 1, username = "me")
        val api = mockk<ApiService>()
        coEvery { api.login(any()) } returns LoginResponse("token", user)
        val session = mockk<SessionManager>(relaxed = true)
        every { session.dataScopeChanged() } returns false
        val scheduler = mockk<SyncScheduler>(relaxed = true)

        AuthRepository(api, session, mockk(relaxed = true), scheduler).login("me", "secret")

        verify { session.signIn("token", user) }
        verify(exactly = 1) { scheduler.schedule() }
    }
}
