package com.example.data.anatomy

enum class StructureCategory(val label: String, val badgeColorHex: Long) {
    ARTERY("Artery", 0xFFD32F2F),
    VEIN("Vein", 0xFF1976D2),
    NERVE("Nerve", 0xFFF57F17),
    MUSCLE("Muscle", 0xFF8E24AA),
    FASCIA_SHEATH("Fascia & Sheath", 0xFF00897B),
    ANATOMICAL_SPACE("Triangle & Space", 0xFFE65100),
    ORGAN("Organ & Viscera", 0xFF00ACC1),
    BONE("Bone & Skeletal", 0xFF5D4037)
}

data class BranchLink(
    val name: String,
    val targetId: String? = null,
    val description: String = ""
)

data class AnatomyRelations(
    val anterior: List<String> = emptyList(),
    val posterior: List<String> = emptyList(),
    val medial: List<String> = emptyList(),
    val lateral: List<String> = emptyList(),
    val contents: List<String> = emptyList()
)

data class AnatomyImage(
    val title: String,
    val description: String,
    val imageUrl: String,
    val diagramType: String = "medical_schematic"
)

data class AnatomyStructure(
    val id: String,
    val name: String,
    val latinName: String = "",
    val category: StructureCategory,
    val origin: String = "",
    val termination: String = "",
    val definition: String,
    val course: String,
    val relations: AnatomyRelations = AnatomyRelations(),
    val branches: List<BranchLink> = emptyList(),
    val clinicalCorrelations: List<String> = emptyList(),
    val mnemonics: String = "",
    val highYieldSummary: String = "",
    val images: List<AnatomyImage> = emptyList()
)
