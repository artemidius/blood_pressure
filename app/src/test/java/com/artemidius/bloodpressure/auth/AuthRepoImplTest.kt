package com.artemidius.bloodpressure.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Test

class AuthRepoImplTest {

    private val firebaseAuth = mockk<FirebaseAuth>()
    private val firebaseUser = mockk<FirebaseUser>()
    private val repo = AuthRepoImpl(firebaseAuth)

    @Test
    fun `isUserAuthorised user is logged in`() {
        every { firebaseAuth.currentUser } returns firebaseUser
        assert(repo.isUserAuthorised())
    }

    @Test
    fun `isUserAuthorised user is not logged in`() {
        every { firebaseAuth.currentUser } returns null
        assert(!repo.isUserAuthorised())
    }

    @Test
    fun `getUserId user is logged in`() {
        every { firebaseAuth.currentUser } returns mockk {
            every { uid } returns "test_uid"
        }
        assertEquals("test_uid", repo.getUserId())
    }

    @Test
    fun `getUserId user is not logged in`() {
        every { firebaseAuth.currentUser } returns null
        assertEquals(null, repo.getUserId())
    }
}