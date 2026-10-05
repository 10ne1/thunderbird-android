package com.fsck.k9.activity.compose

import com.fsck.k9.helper.IdentityHelper
import com.fsck.k9.helper.IdentityHelper.AccountIdentity
import com.fsck.k9.mail.Message
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.mail.Flag

/**
 * How to resume a draft opened in a view-only account, which sends nothing itself, in the account it belongs to.
 *
 * @property owner The account and identity the draft is written as, or `null` to stay in the current account.
 * @property draftId The draft's id in the store of the account written in, if its Drafts folder has the draft:
 *   saving or sending replaces that one. Never the id the view-only account's own store gives the draft.
 * @property deleteViewOnlyCopy Whether the copy seen through the view-only account is deleted once the draft is
 *   saved or sent: the draft is saved as a new one, since the account written in does not have it here.
 */
data class ViewOnlyDraftResume(
    val owner: AccountIdentity?,
    val draftId: Long?,
    val deleteViewOnlyCopy: Boolean,
)

object ViewOnlyDrafts {
    /**
     * Decide how to resume [message], opened as a draft in a view-only account.
     *
     * @param accounts The accounts that can send, to find the draft's own among.
     * @param currentAccount The account written in when none of [accounts] has the draft's identity.
     * @param findDraftId The id of the message with the given Message-ID in an account's Drafts folder, if any.
     */
    @JvmStatic
    fun resume(
        accounts: List<LegacyAccountDto>,
        currentAccount: LegacyAccountDto,
        message: Message,
        findDraftId: (LegacyAccountDto, String?) -> Long?,
    ): ViewOnlyDraftResume {
        val owner = IdentityHelper.findAccountIdentityOfDraft(accounts, message)

        // "Edit as new message" on mail that is not a draft: a new draft, and the message stays where it is.
        if (!message.isSet(Flag.DRAFT)) {
            return ViewOnlyDraftResume(owner, draftId = null, deleteViewOnlyCopy = false)
        }

        val draftId = findDraftId(owner?.account ?: currentAccount, message.messageId)
        return ViewOnlyDraftResume(owner, draftId, deleteViewOnlyCopy = draftId == null)
    }
}
