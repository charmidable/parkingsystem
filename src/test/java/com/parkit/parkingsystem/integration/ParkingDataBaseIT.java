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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
    ParkingService parkingService;

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
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegistrationNumber);
        dataBasePrepareService.clearDataBaseEntries();
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    }

    @AfterAll
    private static void tearDown()
    {

    }


    // TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability

    @ParameterizedTest
    @EnumSource(ParkingType.class)
    public void testParking(ParkingType parkingType)
    {
        when(inputReaderUtil.readSelection()).thenReturn(parkingType.ordinal() + 1);

        // on obtient la prochaine place disponible.
        int slotNumber = parkingSpotDAO.getNextAvailableSlot(parkingType);

        // on s'assure que la place prévue est bien disponible dans la base de donnée
        assertEquals(true, parkingSpotDAO.isItAnAvailableSlot(slotNumber));

        // on introduit le véhicule d'immatriculation "ABCDEF" dans le parking
        parkingService.processIncomingVehicle();

        // on récupère le ticket enregistré dans la DB
        Ticket ticket = ticketDAO.getTicket(vehicleRegistrationNumber);

        // on s'assure qu'on a bien pu récupérer le ticket.
        assertNotNull(ticket, "Pas de ticket dans la DB");

        // on récupère le numéro de la place attribuée dans le ticket
        long parkingSpotId = ticket.getParkingSpot().getId();

        // on s'assure que la place attribuée est bien celle prévue
        assertEquals(slotNumber, parkingSpotId, "l'emplacement attribué n'est pas celui prévu");

        // on s'assure qu'à présent la place attribuée n'est plus disponible dans la DB.
        assertFalse(parkingSpotDAO.isItAnAvailableSlot(parkingSpotId));

        // on récupère la date d'entrée du ticket
        Date inTimeFromDB = ticket.getInTime();

        // on s'assure que la date d'entrée du ticket n'est pas NULL.
        assertNotNull(inTimeFromDB, "Pas de date d’entrée dans le ticket");

        // on s'assure que la date est cohérente
        Instant inTime = ticket.getInTime().toInstant();
        Instant now = Instant.now();
        assertTrue( (ChronoUnit.SECONDS.between( now , inTime )  <= 1 ), "Date d’entrée incohérente");
    }

    //TODO: check that the fare generated and out time are populated correctly in the database

    @ParameterizedTest
    @EnumSource(ParkingType.class)
    public void testParkingLotExit(ParkingType parkingType)
    {
        testParking(parkingType);

        try
        {
            Thread.sleep(1000);
        }
        catch (Exception e)
        {
            System.out.println("Exception during Thread.sleep in ParkingDataBaseIT.testParkingLotExit TEST");
        }

        // On procède à la sortie
        parkingService.processExitingVehicle();

        // On récupère le ticket enregistré dans la DB
        Ticket ticket = ticketDAO.getTicket(vehicleRegistrationNumber);

        // On s'assure qu'on a bien pu récupérer le ticket.
        assertNotNull(ticket, "Pas de ticket dans la DB");

        // On récupère la date de sortie enregistrée dans la DB
        Date outTimeFromDB = ticket.getOutTime();

        // On s'assure que la date de sortie enregistrée dans la base de donnée n'est pas NULL.
        assertNotNull(outTimeFromDB, "Pas de date de sortie dans la DB");
        
        // On s'assure que la date de sortie est cohérente
        Instant outTime = ticket.getOutTime().toInstant();
        Instant now = Instant.now();
        assertTrue((ChronoUnit.SECONDS.between(now, outTime)  <= 1), "Date de sortie dans la DB incohérente");

        // On s'assure que le prix du ticket enregistré dans la base de donnée n'est pas NULL.
        assertNotNull(ticket.getPrice(), "Pas de prix de ticket dans la DB");

        // On s'assure que le prix du ticket enregistré dans la DB est cohérent (0 parce que moins de 30 minutes)
        assertEquals(ticket.getPrice(), 0);
    }

    @ParameterizedTest
    @EnumSource(ParkingType.class)
    public void testSaveNewTicket(ParkingType parkingType)
    {
        // on crée et sauvegarde un ticket avec le vehicleRegistrationNumber "ABCDEF"
        testParking(parkingType);

        when(inputReaderUtil.readSelection()).thenReturn(parkingType.ordinal() + 1);

        // on recrée un ticket avec le même vehicleRegistrationNumber "ABCDEF"
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date());
        ticket.setParkingSpot(parkingService.getNextParkingNumberIfAvailable());
        ticket.setVehicleRegNumber(vehicleRegistrationNumber);

        // On s'assure que la tentative de sauvegarde du ticket doublon provoque une "IllegalStateException"
        assertThrows(IllegalStateException.class, () ->  ticketDAO.saveNewTicket(ticket), "2 tickets créés en même temps pour le même véhicule");
    }


    @ParameterizedTest
    @EnumSource(ParkingType.class)
    public void testFillingAllSlotSButNotMore(ParkingType parkingType)
    {
        when(inputReaderUtil.readSelection()).thenReturn(parkingType.ordinal() + 1);

        int numberOfSlot = parkingSpotDAO.countSlotByType(parkingType);

        vehicleRegistrationNumber = "";

        for (int i = 1; i <= numberOfSlot; i++)
        {
            vehicleRegistrationNumber = "" + i + parkingType;
            try
            {
                when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegistrationNumber);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            parkingService.processIncomingVehicle();
            try
            {
                Thread.sleep(1000);
            }
            catch (Exception e)
            {
                System.out.println("Exception during Thread.sleep in ParkingDataBaseIT.testFillingOfSlot TEST");
            }

            assertNotNull(ticketDAO.getTicket(vehicleRegistrationNumber), "All " + parkingType.name() + "slots cannot be filled.");

            vehicleRegistrationNumber = "";
        }
        assertNull( parkingService.getNextParkingNumberIfAvailable() );
    }
}