package com.example.elosonoro.data.local

import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.local.entity.PlateProfileCrossRef
import com.example.elosonoro.data.local.entity.ProfileEntity
import com.example.elosonoro.data.local.entity.TelemetryEntity
import com.example.elosonoro.data.local.model.PlateWithProfiles
import com.example.elosonoro.data.local.model.ProfileWithPlates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class EntityModelUnitTest {

    @Test
    fun testPlateEntityCreationAndDefaults() {
        val plate = PlateEntity(name = "Comer")
        assertNotNull(plate.id)
        assertEquals("Comer", plate.name)
        assertNull(plate.imageUri)
        assertNull(plate.audioUri)
    }

    @Test
    fun testPlateEntityWithMedia() {
        val imagePath = "file:///data/user/0/com.example.elosonoro/files/img_123.png"
        val audioPath = "file:///data/user/0/com.example.elosonoro/files/audio_123.mp3"
        val plate = PlateEntity(
            id = "custom-id",
            name = "Beber",
            imageUri = imagePath,
            audioUri = audioPath
        )
        assertEquals("custom-id", plate.id)
        assertEquals("Beber", plate.name)
        assertEquals(imagePath, plate.imageUri)
        assertEquals(audioPath, plate.audioUri)
    }

    @Test
    fun testProfileEntityCreation() {
        val profile = ProfileEntity(name = "Ações")
        assertNotNull(profile.id)
        assertEquals("Ações", profile.name)
    }

    @Test
    fun testPlateProfileCrossRefCompositeKey() {
        val plateId = UUID.randomUUID().toString()
        val profileId = UUID.randomUUID().toString()
        val crossRef = PlateProfileCrossRef(plateId = plateId, profileId = profileId)

        assertEquals(plateId, crossRef.plateId)
        assertEquals(profileId, crossRef.profileId)
    }

    @Test
    fun testTelemetryEntityCreation() {
        val plateId = UUID.randomUUID().toString()
        val before = System.currentTimeMillis()
        val telemetry = TelemetryEntity(plateId = plateId)
        val after = System.currentTimeMillis()

        assertEquals(0L, telemetry.id)
        assertEquals(plateId, telemetry.plateId)
        assert(telemetry.timestamp in before..after)
    }

    @Test
    fun testProfileWithPlatesModel() {
        val profile = ProfileEntity(id = "prof-1", name = "Rotina")
        val plate1 = PlateEntity(id = "p-1", name = "Acordar")
        val plate2 = PlateEntity(id = "p-2", name = "Escovar os dentes")
        val model = ProfileWithPlates(profile = profile, plates = listOf(plate1, plate2))

        assertEquals("Rotina", model.profile.name)
        assertEquals(2, model.plates.size)
        assertEquals("Acordar", model.plates[0].name)
    }

    @Test
    fun testPlateWithProfilesModel() {
        val plate = PlateEntity(id = "p-1", name = "Água")
        val profile1 = ProfileEntity(id = "prof-1", name = "Bebidas")
        val profile2 = ProfileEntity(id = "prof-2", name = "Necessidades")
        val model = PlateWithProfiles(plate = plate, profiles = listOf(profile1, profile2))

        assertEquals("Água", model.plate.name)
        assertEquals(2, model.profiles.size)
    }
}
