package com.example.assistenteperitomobile

import com.example.assistenteperitomobile.domain.CategoryReferenceFormat
import com.example.assistenteperitomobile.domain.PeritoAiConfig
import com.example.assistenteperitomobile.domain.PeritoCategoryDefinition
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeritoViewModelCategoryTest {

    @Test
    fun testAddEditDeleteCustomCategory() {
        val initialConfig = PeritoAiConfig()
        val catId = "cat_test_sanatoria"
        val newCat = PeritoCategoryDefinition(
            id = catId,
            displayName = "Sanatoria",
            emoji = "📜",
            defaultPrefix = "Pratica Sanatoria",
            keywords = listOf("sanatoria", "condono")
        )
        val withAdded = initialConfig.copy(
            categories = initialConfig.categories + newCat,
            categoryFormats = initialConfig.categoryFormats + (catId to CategoryReferenceFormat(showSubject = true))
        )
        assertTrue(withAdded.categories.any { it.id == catId })
        assertTrue(withAdded.categoryFormats.containsKey(catId))

        // Update category
        val updatedCat = newCat.copy(displayName = "Sanatoria Edilizia", emoji = "📋")
        val withUpdated = withAdded.copy(
            categories = withAdded.categories.map { if (it.id == catId) updatedCat else it }
        )
        assertTrue(withUpdated.categories.any { it.id == catId && it.displayName == "Sanatoria Edilizia" })

        // Delete category
        val withDeleted = withUpdated.copy(
            categories = withUpdated.categories.filter { it.id != catId },
            categoryFormats = withUpdated.categoryFormats - catId
        )
        assertFalse(withDeleted.categories.any { it.id == catId })
        assertFalse(withDeleted.categoryFormats.containsKey(catId))
    }
}
