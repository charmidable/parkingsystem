package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;


@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT
{
    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private static String vehicleRegistrationNumber = "ABCDEF";

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception
    {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception
    {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegistrationNumber);
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown()
    {

    }

    //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability
    // vérifiez qu'un ticket est effectivement enregistré dans la base de données
    // et que la table des parkings est mise à jour avec la disponibilité.
    @Test
    public void testParkingACar()
    {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // on obtient la prochaine place disponible pour un CAR.
        int slotNumber = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);

        // on s'assure que la place est bien disponible dans la base de donnée
        assertEquals(true, parkingSpotDAO.isItAnAvailableSlot(slotNumber));

        // on introduit le véhicule d'immatriculation "ABCDEF" dans le parking
        parkingService.processIncomingVehicle();

        // on s'assure que la place attribuée est bien la même que celle proposée au départ.
        long parkingSpotId = ticketDAO.getTicket(vehicleRegistrationNumber).getParkingSpot().getId();
        assertEquals(slotNumber, parkingSpotId);

        // on s'assure qu'à présent la place attribuée n'est plus disponible dans la DB.
        assertFalse(parkingSpotDAO.isItAnAvailableSlot(parkingSpotId));
    }

    //TODO: check that the fare generated and out time are populated correctly in the database
    //TODO: vérifier que le tarif généré et l'heure de sortie sont correctement renseignés dans la base de données.
    @Test
    public void testParkingLotExit()
    {
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // On garde une trace de l'heure juste avant la sortie
        Instant justBeforeOutTime = new Date().toInstant();

        // On procède à la sortie
        parkingService.processExitingVehicle();

        // On récupère le ticket enregistré dans la DB
        Ticket ticketFromDBAfterExit = ticketDAO.getTicket(vehicleRegistrationNumber);

        // On récupère la date de sortie enregistrée dans la DB
        Instant outTimeFromDB = ticketFromDBAfterExit.getOutTime().toInstant();

        // On s'assure que la date de sortie enregistrée dans la base de donnée n'est pas NULL.
        assertNotNull(outTimeFromDB, "Pas de date de sortie dans la DB");

        // On s'assure que la date de sortie est cohérente
        assertTrue((ChronoUnit.SECONDS.between(justBeforeOutTime, outTimeFromDB)  <= 1), "Date de sortie dans la DB incohérente");

        // On s'assure que le prix du ticket enregistré dans la base de donnée n'est pas NULL.
        assertNotNull(ticketFromDBAfterExit.getPrice(), "Pas de prix de ticket dans la DB");

        // On s'assure que le prix du ticket enregistré dans la DB est cohérent (0 parce que moins de 30 minutes)
        assertEquals(ticketFromDBAfterExit.getPrice(), 0);
    }

    @Test
    public void testSaveNewTicket()
    {
        // on crée et sauvegarde un ticket avec le vehicleRegistrationNumber "ABCDEF"
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // on recrée un ticket avec le même vehicleRegistrationNumber "ABCDEF"
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date());
        ticket.setParkingSpot(parkingService.getNextParkingNumberIfAvailable());
        ticket.setVehicleRegNumber(vehicleRegistrationNumber);

        // On s'assure que la tentative de sauvegarde du ticket doublon provoque une "IllegalStateException"
        assertThrows(IllegalStateException.class, () ->  parkingService.saveNewTicket(ticket), "2 tickets créés en même temps pour le même véhicule");
    }
}