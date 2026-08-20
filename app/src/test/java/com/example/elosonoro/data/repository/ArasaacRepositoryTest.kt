package com.example.elosonoro.data.repository

import android.net.Uri
import com.example.elosonoro.data.media.FileManager
import com.example.elosonoro.data.remote.api.ArasaacApiService
import com.example.elosonoro.data.remote.dto.ArasaacKeywordDto
import com.example.elosonoro.data.remote.dto.ArasaacPictogramDto
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.io.InputStream

class ArasaacRepositoryTest {

    private lateinit var fakeApiService: FakeArasaacApiService
    private lateinit var fakeFileManager: FakeFileManager
    private lateinit var repository: ArasaacRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        fakeApiService = FakeArasaacApiService()
        fakeFileManager = FakeFileManager()
        repository = ArasaacRepositoryImpl(
            apiService = fakeApiService,
            fileManager = fakeFileManager,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testSearchPictogramsSuccessMapsToDomain() = runTest(testDispatcher) {
        fakeApiService.searchResult = listOf(
            ArasaacPictogramDto(
                id = 2345,
                keywords = listOf(ArasaacKeywordDto(keyword = "água", type = 1))
            ),
            ArasaacPictogramDto(
                id = 6789,
                keywords = listOf(ArasaacKeywordDto(keyword = "água mineral", type = 1))
            )
        )

        val result = repository.searchPictograms(term = "água", locale = "pt")

        assertTrue(result.isSuccess)
        val pictograms = result.getOrNull()
        assertEquals(2, pictograms?.size)
        assertEquals(2345, pictograms?.get(0)?.id)
        assertEquals("água", pictograms?.get(0)?.name)
        assertEquals("https://static.arasaac.org/pictograms/2345/2345_500.png", pictograms?.get(0)?.imageUrl)
    }

    @Test
    fun testSearchPictogramsBlankTermReturnsEmptyListWithoutNetwork() = runTest(testDispatcher) {
        val result = repository.searchPictograms(term = "   ", locale = "pt")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
        assertEquals(0, fakeApiService.searchCallsCount)
    }

    @Test
    fun testSearchPictogramsNetworkErrorReturnsFailure() = runTest(testDispatcher) {
        fakeApiService.shouldThrowNetworkError = true

        val result = repository.searchPictograms(term = "comida", locale = "pt")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun testDownloadAndSavePictogramSuccessStreamsToFileManager() = runTest(testDispatcher) {
        val sampleImageData = "fake-png-binary-stream".toByteArray()
        val responseBody = sampleImageData.toResponseBody("image/png".toMediaTypeOrNull())
        fakeApiService.downloadResponse = Response.success(responseBody)
        fakeFileManager.savedStreamPath = "/data/user/0/com.example.elosonoro/files/images/img_test.png"

        val result = repository.downloadAndSavePictogram(pictogramId = 2345, extension = "png")

        assertTrue(result.isSuccess)
        assertEquals("/data/user/0/com.example.elosonoro/files/images/img_test.png", result.getOrNull())
        assertEquals(1, fakeFileManager.savedStreamsCount)
    }

    @Test
    fun testDownloadAndSavePictogramHttpErrorReturnsFailure() = runTest(testDispatcher) {
        fakeApiService.downloadResponse = Response.error(404, "Not Found".toResponseBody(null))

        val result = repository.downloadAndSavePictogram(pictogramId = 99999)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(0, fakeFileManager.savedStreamsCount)
    }

    // Fakes para teste de rede e I/O isolados
    private class FakeArasaacApiService : ArasaacApiService {
        var searchResult: List<ArasaacPictogramDto> = emptyList()
        var searchCallsCount = 0
        var shouldThrowNetworkError = false
        var downloadResponse: Response<ResponseBody>? = null

        override suspend fun searchPictograms(locale: String, term: String): List<ArasaacPictogramDto> {
            searchCallsCount++
            if (shouldThrowNetworkError) {
                throw IOException("Simulated network connection failure")
            }
            return searchResult
        }

        override suspend fun downloadPictogram(id: Int): Response<ResponseBody> {
            return downloadResponse ?: Response.error(404, "Not found".toResponseBody(null))
        }
    }

    private class FakeFileManager : FileManager {
        var savedStreamPath = "/fake/images/img_123.png"
        var savedStreamsCount = 0

        override fun getImagesDirectory(): File = File("/tmp/images")
        override fun getAudioDirectory(): File = File("/tmp/audio")
        override fun saveImage(sourceUri: Uri): String = "img_saved.png"
        override fun saveAudio(sourceUri: Uri): String = "audio_saved.mp3"
        override fun saveImageStream(inputStream: InputStream, extension: String): String {
            savedStreamsCount++
            inputStream.readBytes() // Consome stream
            return savedStreamPath
        }
        override fun saveAudioStream(inputStream: InputStream, extension: String): String = "audio_stream.mp3"
        override fun fileExists(pathOrUri: String?): Boolean = true
        override fun getFile(pathOrUri: String?): File? = null
        override fun deleteFile(pathOrUri: String?): Boolean = true
    }
}
