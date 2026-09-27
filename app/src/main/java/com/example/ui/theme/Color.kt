package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Arena AI Theme Colors
val ArenaMidnight = Color(0xFF090D16)
val ArenaDarkSurface = Color(0xFF111827)
val ArenaDarkSurfaceVariant = Color(0xFF1E293B)
val ArenaDarkBorder = Color(0xFF334155)

val ArenaPrimary = Color(0xFF6366F1) // Indigo/Electric Violet
val ArenaPrimaryVariant = Color(0xFF4F46E5)
val ArenaSecondary = Color(0xFF38BDF8) // Sky Blue
val ArenaTertiary = Color(0xFFA855F7) // Purple Accent

val ArenaSuccess = Color(0xFF10B981) // Emerald Green (Wins)
val ArenaDanger = Color(0xFFF43F5E) // Rose/Coral (Losses/Battles)
val ArenaWarning = Color(0xFFF59E0B) // Amber (Ties)

val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

// Light Theme equivalents
val ArenaLightBg = Color(0xFFF8FAFC)
val ArenaLightSurface = Color(0xFFFFFFFF)
val ArenaLightSurfaceVariant = Color(0xFFF1F5F9)
val ArenaLightBorder = Color(0xFFE2E8F0)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)

// Category Accent Colors
val CategoryReasoning = Color(0xFF8B5CF6)
val CategoryCoding = Color(0xFF10B981)
val CategoryMath = Color(0xFF0EA5E9)
val CategoryCreative = Color(0xFFD946EF)
val CategoryFactuality = Color(0xFFF59E0B)
val CategoryStressTest = Color(0xFFEF4444)
val CategoryGeneral = Color(0xFF64748B)

fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "reasoning" -> CategoryReasoning
        "coding" -> CategoryCoding
        "math" -> CategoryMath
        "creative" -> CategoryCreative
        "factuality" -> CategoryFactuality
        "stress test" -> CategoryStressTest
        else -> CategoryGeneral
    }
}
