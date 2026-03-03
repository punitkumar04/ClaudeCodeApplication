package com.punitkumar.gruhkharch.domain.model

data class Member(
    val userId: String = "",
    val name: String = "",
    val role: MemberRole = MemberRole.FAMILY_MEMBER,
    val color: String = "#8B5E3C",
    val joinedAt: Long = System.currentTimeMillis()
)

enum class MemberRole(val displayName: String) {
    OWNER("Owner"),
    FAMILY_MEMBER("Family Member")
}
