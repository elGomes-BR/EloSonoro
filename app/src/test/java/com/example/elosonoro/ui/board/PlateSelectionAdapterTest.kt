package com.example.elosonoro.ui.board

import com.example.elosonoro.data.local.entity.PlateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Testes unitários para o PlateSelectionAdapter (Refatoração 6, Spec 2.5 e Plan 7.4.2).
 * Valida:
 * 1. Inicialização e preservação de IDs selecionados.
 * 2. Filtragem de busca em tempo real por nome (case-insensitive) sem perda de itens marcados que ficam ocultos.
 * 3. Restauração da lista completa mantendo checkboxes marcados intactos após limpar a busca.
 * 4. Inclusão dinâmica e automática de placas criadas via atalho ("Nova Placa") com marcação imediata.
 */
class PlateSelectionAdapterTest {

    private lateinit var samplePlates: List<PlateEntity>

    @Before
    fun setUp() {
        samplePlates = listOf(
            PlateEntity(id = "p-1", name = "Água", imageUri = null, audioUri = null),
            PlateEntity(id = "p-2", name = "Comida", imageUri = null, audioUri = null),
            PlateEntity(id = "p-3", name = "Banheiro", imageUri = null, audioUri = null),
            PlateEntity(id = "p-4", name = "Quero água de coco", imageUri = null, audioUri = null),
            PlateEntity(id = "p-5", name = "Brincar", imageUri = null, audioUri = null)
        )
    }

    @Test
    fun testInitialSelectionPreserved() {
        val initialSelected = setOf("p-1", "p-3")
        val adapter = PlateSelectionAdapter(samplePlates, initialSelected)

        assertEquals(5, adapter.itemCount)
        assertEquals(5, adapter.getDisplayedPlates().size)
        assertEquals(2, adapter.getSelectedPlateIds().size)
        assertTrue(adapter.isSelected("p-1"))
        assertTrue(adapter.isSelected("p-3"))
        assertFalse(adapter.isSelected("p-2"))
    }

    @Test
    fun testFilterLimitsDisplayedPlatesWithoutLosingSelectedIds() {
        val initialSelected = setOf("p-1", "p-2", "p-3") // "Água", "Comida", "Banheiro"
        val adapter = PlateSelectionAdapter(samplePlates, initialSelected)

        // Filtra por "água"
        adapter.filter("água")

        // Deve exibir apenas "Água" (p-1) e "Quero água de coco" (p-4)
        assertEquals(2, adapter.itemCount)
        val displayedNames = adapter.getDisplayedPlates().map { it.name }
        assertTrue(displayedNames.contains("Água"))
        assertTrue(displayedNames.contains("Quero água de coco"))
        assertFalse(displayedNames.contains("Comida"))
        assertFalse(displayedNames.contains("Banheiro"))

        // Itens que foram ocultados ("Comida" p-2 e "Banheiro" p-3) continuam selecionados
        val selectedIds = adapter.getSelectedPlateIds()
        assertEquals(3, selectedIds.size)
        assertTrue(selectedIds.contains("p-1"))
        assertTrue(selectedIds.contains("p-2"))
        assertTrue(selectedIds.contains("p-3"))
    }

    @Test
    fun testClearingFilterRestoresAllPlatesAndPreservesSelection() {
        val initialSelected = setOf("p-2", "p-5")
        val adapter = PlateSelectionAdapter(samplePlates, initialSelected)

        adapter.filter("Banheiro")
        assertEquals(1, adapter.itemCount)
        assertEquals("Banheiro", adapter.getDisplayedPlates().first().name)

        // Limpa a busca
        adapter.filter("")
        assertEquals(5, adapter.itemCount)
        assertEquals(2, adapter.getSelectedPlateIds().size)
        assertTrue(adapter.isSelected("p-2"))
        assertTrue(adapter.isSelected("p-5"))
        assertFalse(adapter.isSelected("p-3"))
    }

    @Test
    fun testAddPlateInsertsAndSelectsPlateAutomatically() {
        val adapter = PlateSelectionAdapter(samplePlates, setOf("p-1"))

        val newPlate = PlateEntity(id = "p-new", name = "Dormir", imageUri = "file:///img.png", audioUri = null)
        adapter.addPlate(newPlate, select = true)

        // Deve ter adicionado a nova placa
        assertEquals(6, adapter.itemCount)
        assertEquals(6, adapter.getOriginalPlates().size)
        assertTrue(adapter.getOriginalPlates().any { it.id == "p-new" })

        // Deve ter marcado a nova placa como selecionada
        assertTrue(adapter.isSelected("p-new"))
        assertTrue(adapter.isSelected("p-1"))
        assertEquals(2, adapter.getSelectedPlateIds().size)
    }

    @Test
    fun testAddPlateWithActiveFilter() {
        val adapter = PlateSelectionAdapter(samplePlates, emptySet())
        adapter.filter("Suco")
        assertEquals(0, adapter.itemCount)

        val newPlate = PlateEntity(id = "p-suco", name = "Suco de Laranja")
        adapter.addPlate(newPlate, select = true)

        // O novo item satisfaz o filtro ativo "Suco"
        assertEquals(1, adapter.itemCount)
        assertEquals("Suco de Laranja", adapter.getDisplayedPlates().first().name)
        assertTrue(adapter.isSelected("p-suco"))
    }

    @Test
    fun testSetPlatesWithPreservedSelection() {
        val adapter = PlateSelectionAdapter(samplePlates, setOf("p-1", "p-2"))

        val updatedPlates = samplePlates + listOf(
            PlateEntity(id = "p-6", name = "Música")
        )

        // Atualiza a listagem sem alterar os selecionados (newSelectedIds = null)
        adapter.setPlates(updatedPlates, newSelectedIds = null)

        assertEquals(6, adapter.itemCount)
        assertEquals(2, adapter.getSelectedPlateIds().size)
        assertTrue(adapter.isSelected("p-1"))
        assertTrue(adapter.isSelected("p-2"))
        assertFalse(adapter.isSelected("p-6"))
    }

    @Test
    fun testSetPlatesWithNewSelection() {
        val adapter = PlateSelectionAdapter(samplePlates, setOf("p-1"))

        val newSelected = setOf("p-4", "p-5")
        adapter.setPlates(samplePlates, newSelectedIds = newSelected)

        assertEquals(2, adapter.getSelectedPlateIds().size)
        assertFalse(adapter.isSelected("p-1"))
        assertTrue(adapter.isSelected("p-4"))
        assertTrue(adapter.isSelected("p-5"))
    }
}
