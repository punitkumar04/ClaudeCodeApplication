package com.punitkumar.gruhkharch.domain.model

data class ConstructionStage(
    val id: String = "",
    val name: String = "",
    val emoji: String = "",
    val order: Int = 0,
    val isCustom: Boolean = false
)

object DefaultStages {
    val all = listOf(
        ConstructionStage("pre_construction", "Pre-Construction", "📝", 1),
        ConstructionStage("site_preparation", "Site Preparation", "🏗️", 2),
        ConstructionStage("foundation", "Foundation", "🧱", 3),
        ConstructionStage("plinth_superstructure", "Plinth & Superstructure", "🔼", 4),
        ConstructionStage("lintel_superstructure", "Lintel & Superstructure", "🪟", 5),
        ConstructionStage("roof_slab", "Roof / Slab", "🏠", 6),
        ConstructionStage("brickwork_plastering", "Brickwork & Plastering", "🧱", 7),
        ConstructionStage("plumbing_sanitary", "Plumbing & Sanitary", "🚰", 8),
        ConstructionStage("electrical", "Electrical Wiring & Fittings", "⚡", 9),
        ConstructionStage("doors_windows", "Doors & Windows Installation", "🪟", 10),
        ConstructionStage("flooring_tiling", "Flooring & Tiling", "🏗️", 11),
        ConstructionStage("painting", "Painting", "🎨", 12),
        ConstructionStage("fabrication", "Fabrication", "🔧", 13),
        ConstructionStage("external_works", "External Works", "🌿", 14),
        ConstructionStage("finishing", "Finishing & Handover", "🏠", 15)
    )
}
