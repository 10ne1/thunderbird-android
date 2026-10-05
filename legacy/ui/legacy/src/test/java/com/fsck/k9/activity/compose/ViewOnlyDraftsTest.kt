package com.fsck.k9.activity.compose

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.internet.AddressHeaderBuilder
import com.fsck.k9.mail.internet.MimeMessage
import java.util.UUID
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.common.mail.Flag
import org.junit.Test

class ViewOnlyDraftsTest : RobolectricTest() {
    private val current = account(CURRENT_ADDRESS)
    private val owner = account(OWNER_ADDRESS)
    private val lookups = mutableListOf<Pair<LegacyAccountDto, String?>>()

    private fun findDraftId(found: Long?): (LegacyAccountDto, String?) -> Long? = { account, messageId ->
        lookups.add(account to messageId)
        found
    }

    @Test
    fun `a draft its own account has here is edited there`() {
        val result = ViewOnlyDrafts.resume(listOf(current, owner), current, draft(), findDraftId(found = 42L))

        assertThat(result.owner?.account).isEqualTo(owner)
        assertThat(result.owner?.identity?.email).isEqualTo(OWNER_ADDRESS)
        assertThat(result.draftId).isEqualTo(42L)
        assertThat(result.deleteViewOnlyCopy).isFalse()
        assertThat(lookups).containsExactly(owner to MESSAGE_ID)
    }

    @Test
    fun `a draft its own account does not have here is saved anew there and the view copy deleted`() {
        val result = ViewOnlyDrafts.resume(listOf(current, owner), current, draft(), findDraftId(found = null))

        assertThat(result.owner?.account).isEqualTo(owner)
        assertThat(result.draftId).isNull()
        assertThat(result.deleteViewOnlyCopy).isTrue()
    }

    @Test
    fun `a message that is not a draft becomes a new draft and stays where it is`() {
        val result = ViewOnlyDrafts.resume(
            listOf(current, owner),
            current,
            draft(isDraft = false),
            findDraftId(found = 42L),
        )

        assertThat(result.owner?.account).isEqualTo(owner)
        assertThat(result.draftId).isNull()
        assertThat(result.deleteViewOnlyCopy).isFalse()
        assertThat(lookups).isEqualTo(emptyList<Pair<LegacyAccountDto, String?>>())
    }

    @Test
    fun `a draft of none of the accounts is looked for in the current account`() {
        val result = ViewOnlyDrafts.resume(
            listOf(current, owner),
            current,
            draft(from = "stranger@example.com", to = "someone@example.com"),
            findDraftId(found = 7L),
        )

        assertThat(result.owner).isNull()
        assertThat(result.draftId).isEqualTo(7L)
        assertThat(lookups).containsExactly(current to MESSAGE_ID)
    }

    private fun account(address: String) = LegacyAccountDto(UUID.randomUUID().toString()).apply {
        replaceIdentities(listOf(Identity(name = address, email = address)))
    }

    private fun draft(
        from: String = OWNER_ADDRESS,
        to: String = CURRENT_ADDRESS,
        isDraft: Boolean = true,
    ) = MimeMessage().apply {
        addHeader("From", AddressHeaderBuilder.createHeaderValue(arrayOf(Address(from))))
        addHeader("To", AddressHeaderBuilder.createHeaderValue(arrayOf(Address(to))))
        addHeader("Message-ID", MESSAGE_ID)
        setFlag(Flag.DRAFT, isDraft)
    }

    companion object {
        const val CURRENT_ADDRESS = "current@example.org"
        const val OWNER_ADDRESS = "owner@example.org"
        const val MESSAGE_ID = "<draft@example.org>"
    }
}
