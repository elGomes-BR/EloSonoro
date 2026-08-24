package com.example.elosonoro.ui.board

import com.example.elosonoro.data.local.entity.PlateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para o PlateSelectionAdapter cobrindo a Refatoração 6:
 * - Listagem global de placas do sistema
 * - Busca em tempo real preservando seleções de checkboxes ocultos
 * - Adição e seleção dinâmica de novas placas criadas no diálogo
 */
class PlateSelectionAdapterTest {

    @Test
    fun testInitialPlatesAndSelection() {
        val plate1 = PlateEntity(id = "p-1", name = "Água")
        val plate2 = PlateEntity(id = "p-2", name = "Comida")
        val adapter = PlateSelectionAdapter(
            initialPlates = listOf(plate1, plate2),
            initialSelectedIds = setOf("p-1")
        )

        assertEquals(2, adapter.itemCount)
        assertEquals(listOf("p-1"), adapter.getSelectedPlateIds())
        assertTrue(adapter.isPlateSelected("p-1"))
        assertFalse(adapter.isPlateSelected("p-2"))
    }

    @Test
    fun testFilterPreservesSelectionsWhenItemIsHidden() {
        val plate1 = PlateEntity(id = "p-1", name = "Água mineral")
        val plate2 = PlateEntity(id = "p-2", name = "Banana nanica")
        val plate3 = PlateEntity(id = "p-3", name = "Maçã")
        val adapter = PlateSelectionAdapter(
            initialPlates = listOf(plate1, plate2, plate3),
            initialSelectedIds = setOf("p-1", "p-2")
        )

        // Filtra por "Maçã" (ocultando Água e Banana)
        adapter.filter("Maçã")

        assertEquals(1, adapter.itemCount)

        // As seleções de "p-1" e "p-2" devem permanecer intactas no conjunto selectedIds
        val selected = adapter.getSelectedPlateIds()
        assertEquals(2, selected.size)
        assertTrue(selected.contains("p-1"))
        assertTrue(selected.contains("p-2"))
        assertTrue(adapter.isPlateSelected("p-1"))
        assertTrue(adapter.isPlateSelected("p-2"))
    }

    @Test
    fun testFilterRestoresAllItemsWhenCleared() {
        val plate1 = PlateEntity(id = "p-1", name = "Água")
        val plate2 = PlateEntity(id = "p-2", name = "Bola")
        val adapter = PlateSelectionAdapter(
            initialPlates = listOf(plate1, plate2),
            initialSelectedIds = setOf("p-1")
        )

        adapter.filter("Bola")
        assertEquals(1, adapter.itemCount)

        // Limpa o filtro
        adapter.filter("")
        assertEquals(2, adapter.itemCount)
        assertEquals(listOf("p-1"), adapter.getSelectedPlateIds())
    }

    @Test
    fun testAddAndSelectPlateDynamically() {
        val plate1 = PlateEntity(id = "p-1", name = "Água")
        val adapter = PlateSelectionAdapter(
            initialPlates = listOf(plate1),
            initialSelectedIds = setOf("p-1")
        )

        val newPlate = PlateEntity(id = "p-new-1", name = "Suco de uva")
        adapter.addAndSelectPlate(newPlate)

        assertEquals(2, adapter.itemCount)
        val selected = adapter.getSelectedPlateIds()
        assertEquals(2, selected.size)
        assertTrue(selected.contains("p-1"))
        assertTrue(selected.contains("p-new-1"))
        assertTrue(adapter.isPlateSelected("p-new-1"))
    }

    @Test
    fun testSetAllPlatesUpdatesListAndMaintainsFilter() {
        val adapter = PlateSelectionAdapter()
        val plate1 = PlateEntity(id = "p-1", name = "Água")
        val plate2 = PlateEntity(id = "p-2", name = "Brinquedo")

        adapter.setAllPlates(
            plates = listOf(plate1, plate2),
            selectedIdsToSet = setOf("p-2")
        )

        assertEquals(2, adapter.itemCount)
        assertEquals(listOf("p-2"), adapter.getSelectedPlateIds())
    }
}
