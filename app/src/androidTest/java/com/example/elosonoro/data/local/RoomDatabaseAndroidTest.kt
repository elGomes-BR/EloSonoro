package com.example.elosonoro.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.elosonoro.data.local.dao.CrossRefDao
import com.example.elosonoro.data.local.dao.PlateDao
import com.example.elosonoro.data.local.dao.ProfileDao
import com.example.elosonoro.data.local.dao.TelemetryDao
import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.local.entity.PlateProfileCrossRef
import com.example.elosonoro.data.local.entity.ProfileEntity
import com.example.elosonoro.data.local.entity.TelemetryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID

/**
 * Testes instrumentados de validação das regras da Camada de Dados (Room).
 */
@RunWith(AndroidJUnit4::class)
class RoomDatabaseAndroidTest {

    private lateinit var db: EloSonoroDatabase
    private lateinit var plateDao: PlateDao
    private lateinit var profileDao: ProfileDao
    private lateinit var crossRefDao: CrossRefDao
    private lateinit var telemetryDao: TelemetryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EloSonoroDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        plateDao = db.plateDao()
        profileDao = db.profileDao()
        crossRefDao = db.crossRefDao()
        telemetryDao = db.telemetryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndSearchPlates() = runTest {
        val plate1 = PlateEntity(
            id = UUID.randomUUID().toString(),
            name = "Água",
            imageUri = "file:///data/user/0/com.example.elosonoro/files/img_agua.png",
            audioUri = "file:///data/user/0/com.example.elosonoro/files/audio_agua.mp3"
        )
        val plate2 = PlateEntity(
            id = UUID.randomUUID().toString(),
            name = "Suco de Laranja",
            imageUri = null,
            audioUri = null
        )
        plateDao.insert(plate1)
        plateDao.insert(plate2)

        val retrieved = plateDao.getPlateByIdSync(plate1.id)
        assertNotNull(retrieved)
        assertEquals("Água", retrieved?.name)
        assertEquals("file:///data/user/0/com.example.elosonoro/files/img_agua.png", retrieved?.imageUri)

        val searchResults = plateDao.searchPlatesByName("Laranja").first()
        assertEquals(1, searchResults.size)
        assertEquals("Suco de Laranja", searchResults[0].name)

        val allPlates = plateDao.getAllPlates().first()
        assertEquals(2, allPlates.size)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun testProfileUniqueNameConstraint() = runTest {
        val profile1 = ProfileEntity(id = UUID.randomUUID().toString(), name = "Alimentação")
        val profile2 = ProfileEntity(id = UUID.randomUUID().toString(), name = "Alimentação")

        profileDao.insert(profile1)
        // Tentativa de duplicar o nome deve lançar SQLiteConstraintException devido ao Unique Index
        profileDao.insert(profile2)
    }

    @Test
    fun testManyToManyRelationAndCompositePrimaryKey() = runTest {
        val profile = ProfileEntity(id = UUID.randomUUID().toString(), name = "Sentimentos")
        profileDao.insert(profile)

        val plateHappy = PlateEntity(id = UUID.randomUUID().toString(), name = "Feliz")
        val plateSad = PlateEntity(id = UUID.randomUUID().toString(), name = "Triste")
        plateDao.insert(plateHappy)
        plateDao.insert(plateSad)

        // Criação de associações com chave composta (plateId, profileId)
        crossRefDao.insert(PlateProfileCrossRef(plateId = plateHappy.id, profileId = profile.id))
        crossRefDao.insert(PlateProfileCrossRef(plateId = plateSad.id, profileId = profile.id))

        // Consulta de Perfil com Placas
        val profileWithPlates = profileDao.getProfileWithPlatesSync(profile.id)
        assertNotNull(profileWithPlates)
        assertEquals(2, profileWithPlates?.plates?.size)

        // Consulta direta via CrossRefDao
        val plates = crossRefDao.getPlatesForProfileSync(profile.id)
        assertEquals(2, plates.size)
        assertTrue(plates.any { it.name == "Feliz" })
        assertTrue(plates.any { it.name == "Triste" })
    }

    @Test
    fun testDeleteProfileDoesNotDeletePlates() = runTest {
        // Regra crítica do spec.md e constitution.md:
        // A exclusão de uma categoria desfaz o vínculo, mas NÃO exclui as placas associadas.
        val profile = ProfileEntity(id = UUID.randomUUID().toString(), name = "Locais")
        profileDao.insert(profile)

        val plate = PlateEntity(id = UUID.randomUUID().toString(), name = "Escola")
        plateDao.insert(plate)

        crossRefDao.insert(PlateProfileCrossRef(plateId = plate.id, profileId = profile.id))

        // Excluir a categoria
        profileDao.delete(profile)

        // Validar que o perfil foi deletado
        val retrievedProfile = profileDao.getProfileByIdSync(profile.id)
        assertNull(retrievedProfile)

        // Validar que o vínculo foi removido
        val crossRefs = crossRefDao.getCrossRefsForProfile(profile.id).first()
        assertTrue(crossRefs.isEmpty())

        // Validar que a Placa PERMANECE INTACTA no banco de dados
        val retrievedPlate = plateDao.getPlateByIdSync(plate.id)
        assertNotNull(retrievedPlate)
        assertEquals("Escola", retrievedPlate?.name)
    }

    @Test
    fun testTelemetryRecording() = runTest {
        val plate = PlateEntity(id = UUID.randomUUID().toString(), name = "Banheiro")
        plateDao.insert(plate)

        val timestamp1 = System.currentTimeMillis()
        telemetryDao.insert(TelemetryEntity(plateId = plate.id, timestamp = timestamp1))
        telemetryDao.insert(TelemetryEntity(plateId = plate.id, timestamp = timestamp1 + 1000))

        val telemetryList = telemetryDao.getTelemetryForPlate(plate.id).first()
        assertEquals(2, telemetryList.size)
        assertEquals(plate.id, telemetryList[0].plateId)

        val count = telemetryDao.getCountForPlate(plate.id).first()
        assertEquals(2, count)
    }
}
