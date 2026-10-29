package com.example.elosonoro.ui.arasaac

import com.example.elosonoro.domain.model.ArasaacPictogram
import com.example.elosonoro.domain.repository.ArasaacRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ArasaacSearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeArasaacRepository: FakeArasaacRepository
    private lateinit var viewModel: ArasaacSearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeArasaacRepository = FakeArasaacRepository()
        viewModel = ArasaacSearchViewModel(arasaacRepository = fakeArasaacRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSearchImmediatelySuccessPopulatesPictograms() = runTest(testDispatcher) {
        fakeArasaacRepository.searchResult = listOf(
            ArasaacPictogram(id = 1, name = "comer", imageUrl = "https://static.arasaac.org/1.png")
        )

        viewModel.searchImmediately("comer")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.pictograms.size)
        assertEquals("comer", state.pictograms.first().name)
    }

    @Test
    fun testSearchBlankQueryClearsPictograms() = runTest(testDispatcher) {
        viewModel.searchWithDebounce("   ")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.pictograms.isEmpty())
    }

    @Test
    fun testDownloadAndSelectPictogramSuccessInvokesCallback() = runTest(testDispatcher) {
        fakeArasaacRepository.downloadResult = "/files/images/img_saved.png"
        var returnedPath: String? = null

        viewModel.downloadAndSelectPictogram(123) { path ->
            returnedPath = path
        }
        advanceUntilIdle()

        assertEquals("/files/images/img_saved.png", returnedPath)
        assertEquals("/files/images/img_saved.png", viewModel.uiState.value.selectedImagePath)
    }

    @Test
    fun testDownloadAndSelectPictogramWithModelReturnsSuggestedName() = runTest(testDispatcher) {
        fakeArasaacRepository.downloadResult = "/files/images/img_agua.png"
        val pictogram = ArasaacPictogram(id = 500, name = "água mineral", imageUrl = "https://static.arasaac.org/500.png")
        var returnedPath: String? = null
        var returnedSuggestedName: String? = null

        viewModel.downloadAndSelectPictogram(pictogram) { path, name ->
            returnedPath = path
            returnedSuggestedName = name
        }
        advanceUntilIdle()

        assertEquals("/files/images/img_agua.png", returnedPath)
        assertEquals("água mineral", returnedSuggestedName)
    }

    @Test
    fun testPreFeedSearchImmediatelyFromPlateNameTriggersQuery() = runTest(testDispatcher) {
        fakeArasaacRepository.searchResult = listOf(
            ArasaacPictogram(id = 10, name = "água", imageUrl = "https://static.arasaac.org/10.png")
        )

        // Simula o pré-preenchimento automático originado do campo "Nome da Placa" (Spec 2.2 e Plan 5)
        viewModel.searchImmediately("Água")
        advanceUntilIdle()

        assertEquals("Água", fakeArasaacRepository.lastSearchedTerm)
        val state = viewModel.uiState.value
        assertEquals("Água", state.query)
        assertEquals(1, state.pictograms.size)
        assertEquals("água", state.pictograms.first().name)
        assertFalse(state.isLoading)
    }

    private class FakeArasaacRepository : ArasaacRepository {
        var searchResult = emptyList<ArasaacPictogram>()
        var downloadResult = "/files/images/img_arasaac.png"
        var lastSearchedTerm: String? = null

        override suspend fun searchPictograms(term: String, locale: String): Result<List<ArasaacPictogram>> {
            lastSearchedTerm = term
            return Result.success(searchResult)
        }

        override suspend fun downloadAndSavePictogram(pictogramId: Int, extension: String): Result<String> {
            return Result.success(downloadResult)
        }
    }
}
