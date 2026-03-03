package com.punitkumar.gruhkharch.domain.model

data class Category(
    val id: String = "",
    val name: String = "",
    val emoji: String = "",
    val subCategories: List<String> = emptyList(),
    val isCustom: Boolean = false
)

object DefaultCategories {
    val materials = Category(
        id = "materials",
        name = "Materials",
        emoji = "📋",
        subCategories = listOf(
            "Cement", "Bricks / Blocks", "Sand (River Sand / M-Sand / P-Sand)",
            "Gravel / Aggregate (Jelly / Metal)", "Steel / TMT Bars / Binding Wire",
            "Wood / Timber", "Bamboo", "Roofing Sheets (Tata / GI / Polycarbonate)",
            "Tiles (Floor / Wall / Vitrified / Ceramic)", "Marble / Granite",
            "Paint (Interior / Exterior / Primer / Putty)",
            "Plumbing Materials (Pipes / Fittings / Taps)",
            "Electrical Materials (Wires / Switches / MCBs / Panels)",
            "Doors (Wood / Steel / Aluminium / UPVC)",
            "Windows (Wood / Aluminium / UPVC / Glass)",
            "Hardware (Hinges / Locks / Handles / Bolts)",
            "Waterproofing Materials",
            "RCC Materials (Centering Plates / Props / Shuttering)",
            "Kitchen Fittings (Sink / Chimney / Hob)",
            "Bathroom Fittings (WC / Wash Basin / Shower / Geyser)",
            "Fabrication Materials (MS Angles / Channels)",
            "Miscellaneous Materials"
        )
    )

    val labour = Category(
        id = "labour",
        name = "Labour",
        emoji = "👷",
        subCategories = listOf(
            "Mason / Bricklayer", "Carpenter", "Plumber", "Electrician",
            "Painter", "Tile / Flooring Worker", "Fabrication / Welding",
            "Earth Work / Excavation Labour", "Centering / Shuttering Labour",
            "Bar Bending Labour", "General Labour / Helpers",
            "Waterproofing Labour", "Cleaning Labour"
        )
    )

    val contractor = Category(
        id = "contractor",
        name = "Contractor Charges",
        emoji = "🏗️",
        subCategories = listOf(
            "Building Contractor (Main)", "Electrical Contractor",
            "Plumbing Contractor", "Painting Contractor",
            "Flooring Contractor", "Fabrication Contractor",
            "Interior Contractor"
        )
    )

    val professional = Category(
        id = "professional",
        name = "Professional Services",
        emoji = "📐",
        subCategories = listOf(
            "Architect Fees", "Structural Engineer Fees",
            "Layout Plan / Site Engineer Visit", "Vastu Consultant",
            "Interior Designer", "Surveyor", "Soil Testing",
            "Approval / Sanction Fees (Panchayat / Municipal)",
            "Legal / Documentation", "Liaison / Agent Fees"
        )
    )

    val transport = Category(
        id = "transport",
        name = "Transport & Machinery",
        emoji = "🚛",
        subCategories = listOf(
            "Material Transport / Delivery Charges", "JCB / Excavator Hire",
            "Concrete Mixer Hire", "Crane Hire", "Water Tanker",
            "Generator Hire", "Scaffolding Hire"
        )
    )

    val utilities = Category(
        id = "utilities",
        name = "Utilities & Connections",
        emoji = "🔌",
        subCategories = listOf(
            "Temporary Electricity Connection", "Permanent Electricity Connection",
            "Water Connection", "Borewell Drilling",
            "Septic Tank Construction", "Drainage / Sewage",
            "Compound / Boundary Wall"
        )
    )

    val ceremonies = Category(
        id = "ceremonies",
        name = "Ceremonies & Rituals",
        emoji = "🙏",
        subCategories = listOf(
            "Bhoomi Puja / Stone Laying Ceremony", "Foundation Ceremony",
            "Griha Pravesh / Housewarming", "Vastu Puja",
            "Other Religious Ceremonies"
        )
    )

    val miscellaneous = Category(
        id = "miscellaneous",
        name = "Miscellaneous",
        emoji = "📦",
        subCategories = listOf(
            "Site Security / Watchman", "Site Storage / Temporary Shed",
            "Food / Tea for Workers", "Tips / Gifts", "Insurance",
            "Loan Processing / Interest", "Contingency / Unforeseen", "Other"
        )
    )

    val all = listOf(materials, labour, contractor, professional, transport, utilities, ceremonies, miscellaneous)
}
