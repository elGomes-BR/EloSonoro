package com.example.elosonoro.data.repository

import android.net.Uri
import com.example.elosonoro.data.local.dao.PlateDao
import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.media.FileManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.InputStream

class PlateRepositoryTest {

    private lateinit var fakePlateDao: FakePlateDao
    private lateinit var fakeFileManager: FakeFileManager
    private lateinit var repository: PlateRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        fakePlateDao = FakePlateDao()
        fakeFileManager = FakeFileManager()
        repository = PlateRepositoryImpl(
            plateDao = fakePlateDao,
            fileManager = fakeFileManager,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testSavePlateBlankNameReturnsFailure() = runTest(testDispatcher) {
        val result = repository.savePlate(name = "   ")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(fakePlateDao.plates.isEmpty())
    }

    @Test
    fun testSavePlateWithMediaCopiesToFilesDirAndSavesInDao() = runTest(testDispatcher) {
        fakeFileManager.savedImagePath = "/data/user/0/com.example.elosonoro/files/images/img_uuid.png"
        fakeFileManager.savedAudioPath = "/data/user/0/com.example.elosonoro/files/audio/audio_uuid.mp3"

        val result = repository.savePlate(
            name = "Água",
            imageUri = "content://media/external/images/media/123",
            audioUri = "content://media/external/audio/media/456"
        )

        assertTrue(result.isSuccess)
        val saved = result.getOrNull()
        assertNotNull(saved)
        assertEquals("Água", saved?.name)
        assertEquals("/data/user/0/com.example.elosonoro/files/images/img_uuid.png", saved?.imageUri)
        assertEquals("/data/user/0/com.example.elosonoro/files/audio/audio_uuid.mp3", saved?.audioUri)

        // Verifica inserção no banco
        assertEquals(1, fakePlateDao.plates.size)
        assertEquals(saved?.id, fakePlateDao.plates.first().id)
    }

    @Test
    fun testDeletePlateRemovesEntityAndDeletesPhysicalMedia() = runTest(testDispatcher) {
        val plate = PlateEntity(
            id = "plate-1",
            name = "Comida",
            imageUri = "/data/user/0/com.example.elosonoro/files/images/img_1.png",
            audioUri = "/data/user/0/com.example.elosonoro/files/audio/audio_1.mp3"
        )
        fakePlateDao.plates.add(plate)
        fakeFileManager.existingFiles.add(plate.imageUri!!)
        fakeFileManager.existingFiles.add(plate.audioUri!!)

        val result = repository.deletePlate(plate)

        assertTrue(result.isSuccess)
        assertTrue(fakePlateDao.plates.isEmpty())
        assertFalse(fakeFileManager.existingFiles.contains(plate.imageUri))
        assertFalse(fakeFileManager.existingFiles.contains(plate.audioUri))
    }

    @Test
    fun testEditPlateReplacingImageDeletesOldImageFileAndSavesNewOne() = runTest(testDispatcher) {
        val oldImagePath = "/data/user/0/com.example.elosonoro/files/images/img_old.png"
        val oldAudioPath = "/data/user/0/com.example.elosonoro/files/audio/audio_old.mp3"
        val newImagePath = "/data/user/0/com.example.elosonoro/files/images/img_new.png"

        val existingPlate = PlateEntity(
            id = "plate-edit-1",
            name = "Bola",
            imageUri = oldImagePath,
            audioUri = oldAudioPath
        )
        fakePlateDao.plates.add(existingPlate)
        fakeFileManager.existingFiles.add(oldImagePath)
        fakeFileManager.existingFiles.add(oldAudioPath)

        fakeFileManager.savedImagePath = newImagePath

        val result = repository.savePlate(
            id = "plate-edit-1",
            name = "Bola Azul",
            imageUri = "content://media/external/images/media/999",
            audioUri = oldAudioPath // Mantém o áudio anterior
        )

        assertTrue(result.isSuccess)
        val updated = result.getOrNull()
        assertNotNull(updated)
        assertEquals("Bola Azul", updated?.name)
        assertEquals(newImagePath, updated?.imageUri)
        assertEquals(oldAudioPath, updated?.audioUri)

        // Verifica que o arquivo antigo de imagem foi deletado fisicamente
        assertFalse(fakeFileManager.existingFiles.contains(oldImagePath))
        // Verifica que o novo arquivo de imagem e o áudio antigo existem
        assertTrue(fakeFileManager.existingFiles.contains(newImagePath))
        assertTrue(fakeFileManager.existingFiles.contains(oldAudioPath))
    }

    @Test
    fun testEditPlateReplacingAudioDeletesOldAudioFileAndSavesNewOne() = runTest(testDispatcher) {
        val oldAudioPath = "/data/user/0/com.example.elosonoro/files/audio/audio_old.mp3"
        val newAudioPath = "/data/user/0/com.example.elosonoro/files/audio/audio_new.mp3"
        val imagePath = "/data/user/0/com.example.elosonoro/files/images/img_1.png"

        val existingPlate = PlateEntity(
            id = "plate-edit-2",
            name = "Brinquedo",
            imageUri = imagePath,
            audioUri = oldAudioPath
        )
        fakePlateDao.plates.add(existingPlate)
        fakeFileManager.existingFiles.add(imagePath)
        fakeFileManager.existingFiles.add(oldAudioPath)

        fakeFileManager.savedAudioPath = newAudioPath

        val result = repository.savePlate(
            id = "plate-edit-2",
            name = "Brinquedo",
            imageUri = imagePath, // Mantém a mesma imagem
            audioUri = "content://media/external/audio/media/888" // Novo áudio
        )

        assertTrue(result.isSuccess)
        val updated = result.getOrNull()
        assertNotNull(updated)
        assertEquals(newAudioPath, updated?.audioUri)

        // Verifica que o arquivo antigo de áudio foi deletado fisicamente
        assertFalse(fakeFileManager.existingFiles.contains(oldAudioPath))
        assertTrue(fakeFileManager.existingFiles.contains(newAudioPath))
        assertTrue(fakeFileManager.existingFiles.contains(imagePath))
    }

    @Test
    fun testEditPlateRemovingMediaDeletesOldFiles() = runTest(testDispatcher) {
        val oldImagePath = "/data/user/0/com.example.elosonoro/files/images/img_to_remove.png"
        val oldAudioPath = "/data/user/0/com.example.elosonoro/files/audio/audio_to_remove.mp3"

        val existingPlate = PlateEntity(
            id = "plate-edit-3",
            name = "Casa",
            imageUri = oldImagePath,
            audioUri = oldAudioPath
        )
        fakePlateDao.plates.add(existingPlate)
        fakeFileManager.existingFiles.add(oldImagePath)
        fakeFileManager.existingFiles.add(oldAudioPath)

        val result = repository.savePlate(
            id = "plate-edit-3",
            name = "Casa Nova",
            imageUri = null,
            audioUri = null
        )

        assertTrue(result.isSuccess)
        val updated = result.getOrNull()
        assertNotNull(updated)
        assertEquals(null, updated?.imageUri)
        assertEquals(null, updated?.audioUri)

        // Verifica que ambos os arquivos antigos foram removidos fisicamente
        assertFalse(fakeFileManager.existingFiles.contains(oldImagePath))
        assertFalse(fakeFileManager.existingFiles.contains(oldAudioPath))
    }

    @Test
    fun testDeletePlateByIdRemovesEntityAndDeletesPhysicalMedia() = runTest(testDispatcher) {
        val plate = PlateEntity(
            id = "plate-del-id",
            name = "Suco de Laranja",
            imageUri = "/data/user/0/com.example.elosonoro/files/images/img_del.png",
            audioUri = "/data/user/0/com.example.elosonoro/files/audio/audio_del.mp3"
        )
        fakePlateDao.plates.add(plate)
        fakeFileManager.existingFiles.add(plate.imageUri!!)
        fakeFileManager.existingFiles.add(plate.audioUri!!)

        val result = repository.deletePlateById("plate-del-id")

        assertTrue(result.isSuccess)
        assertTrue(fakePlateDao.plates.isEmpty())
        assertFalse(fakeFileManager.existingFiles.contains(plate.imageUri))
        assertFalse(fakeFileManager.existingFiles.contains(plate.audioUri))
    }

    @Test
    fun testSearchPlatesReturnsFilteredFlow() = runTest(testDispatcher) {
        val plate1 = PlateEntity(id = "1", name = "Água")
        val plate2 = PlateEntity(id = "2", name = "Suco")
        fakePlateDao.plates.addAll(listOf(plate1, plate2))

        val resultList = repository.searchPlates("ág").first()
        assertEquals(1, resultList.size)
        assertEquals("Água", resultList.first().name)
    }

    // Fakes para teste desacoplado
    private class FakePlateDao : PlateDao {
        val plates = mutableListOf<PlateEntity>()

        override suspend fun insert(plate: PlateEntity) {
            plates.removeAll { it.id == plate.id }
            plates.add(plate)
        }
        override suspend fun insertAll(platesList: List<PlateEntity>) {
            plates.addAll(platesList)
        }
        override suspend fun update(plate: PlateEntity) {
            insert(plate)
        }
        override suspend fun delete(plate: PlateEntity) {
            plates.removeIf { it.id == plate.id }
        }
        override suspend fun deleteById(id: String) {
            plates.removeIf { it.id == id }
        }
        override fun getPlateById(id: String): Flow<PlateEntity?> =
            flowOf(plates.find { it.id == id })
        override suspend fun getPlateByIdSync(id: String): PlateEntity? =
            plates.find { it.id == id }
        override fun getAllPlates(): Flow<List<PlateEntity>> =
            flowOf(plates)
        override fun searchPlatesByName(query: String): Flow<List<PlateEntity>> =
            flowOf(plates.filter { it.name.contains(query, ignoreCase = true) })
        override fun countPlates(): Flow<Int> = flowOf(plates.size)
    }

    private class FakeFileManager : FileManager {
        val existingFiles = mutableSetOf<String>()
        var savedImagePath = "/files/images/img_test.png"
        var savedAudioPath = "/files/audio/audio_test.mp3"

        override fun getImagesDirectory(): File = File("/data/user/0/com.example.elosonoro/files/images")
        override fun getAudioDirectory(): File = File("/data/user/0/com.example.elosonoro/files/audio")
        override fun saveImage(sourceUri: Uri): String {
            existingFiles.add(savedImagePath)
            return savedImagePath
        }
        override fun saveImage(sourceUriString: String): String {
            existingFiles.add(savedImagePath)
            return savedImagePath
        }
        override fun saveAudio(sourceUri: Uri): String {
            existingFiles.add(savedAudioPath)
            return savedAudioPath
        }
        override fun saveAudio(sourceUriString: String): String {
            existingFiles.add(savedAudioPath)
            return savedAudioPath
        }
        override fun saveImageStream(inputStream: InputStream, extension: String): String = savedImagePath
        override fun saveAudioStream(inputStream: InputStream, extension: String): String = savedAudioPath
        override fun fileExists(pathOrUri: String?): Boolean = pathOrUri in existingFiles
        override fun getFile(pathOrUri: String?): File? = pathOrUri?.let { File(it) }
        override fun deleteFile(pathOrUri: String?): Boolean = existingFiles.remove(pathOrUri)
    }
}
