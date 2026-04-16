package com.owlitech.owli.assist.ui.screens

internal fun canSubmitFollowUp(
    question: String,
    attachmentCount: Int,
    isBusy: Boolean
): Boolean {
    return !isBusy && (question.isNotBlank() || attachmentCount > 0)
}
