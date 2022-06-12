package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class FareCalculatorServiceTest
{
    private static FareCalculatorService fareCalculatorService;
    private Ticket ticket;

    @BeforeAll
    private static void setUp()
    {
        fareCalculatorService = new FareCalculatorService();
    }

    @BeforeEach
    private void setUpPerTest()
    {
        ticket = new Ticket();
    }

    @Test
    public void calculateFareCar()
    {
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000));
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(ticket.getPrice(), Fare.CAR_RATE_PER_HOUR);
    }

    @Test
    public void calculateFareBike()
    {
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000));
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(ticket.getPrice(), Fare.BIKE_RATE_PER_HOUR);
    }

    @Test
    public void calculateFareUnknownType()
    {
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000));
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, null, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        assertThrows(NullPointerException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    @Test
    public void calculateFareBikeWithFutureInTime()
    {
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() + (60 * 60 * 1000));
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    @Test
    public void calculateFareBikeWithLessThanOneHourParkingTime()
    {
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (45 * 60 * 1000));//45 minutes parking time should give 3/4th parking fare
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals((0.75 * Fare.BIKE_RATE_PER_HOUR), ticket.getPrice());
    }

    @Test
    public void calculateFareCarWithLessThanOneHourParkingTime()
    {
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (45 * 60 * 1000));//45 minutes parking time should give 3/4th parking fare
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals((0.75 * Fare.CAR_RATE_PER_HOUR), ticket.getPrice());
    }

    @Test
    public void calculateFareCarWithMoreThanADayParkingTime()
    {
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (24 * 60 * 60 * 1000));//24 hours parking time should give 24 * parking fare per hour
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals((24 * Fare.CAR_RATE_PER_HOUR), ticket.getPrice());
    }



    @ParameterizedTest
    @EnumSource(ParkingType.class)
    public void calculateFareWithLessThan30Minutes(ParkingType parkingType)
    {
        Instant outTime = Instant.now();
        Instant inTime = outTime.minus(28, ChronoUnit.MINUTES);
        ticket.setInTime(Date.from(inTime));
        ticket.setOutTime(Date.from(outTime));
        ticket.setParkingSpot(new ParkingSpot(1, parkingType, false));
        fareCalculatorService.calculateFare(ticket);
        assertEquals(ticket.getPrice(), 0, "calculate fare for " + parkingType + " with less than 30 minutes failed");
    }


    @ParameterizedTest
    @EnumSource(ParkingType.class)
    public void calculateFareWithMoreThan30Minutes(ParkingType parkingType)
    {
        Instant outTime = Instant.now();
        Instant inTime = outTime.minus(32, ChronoUnit.MINUTES);

        ParkingSpot parkingSpot = new ParkingSpot(1, parkingType, false);

        ticket.setInTime(Date.from(inTime));
        ticket.setOutTime(Date.from(outTime));
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertTrue(ticket.getPrice() > 0, "calculate fare for " + parkingType + " with more than 30 minutes failed");
    }


    @ParameterizedTest
    @EnumSource(ParkingType.class)
    public void calculateRecurrentUserFare(ParkingType parkingType)
    {
        // création des valeurs d'entrée et sortie du parking correspondant à une durée de 10 jours
        Instant outTime = Instant.now();
        Instant inTime = outTime.minus(10, ChronoUnit.DAYS);

        // fixe les valeurs d'entrée et de sortie du ticket
        ticket.setInTime(Date.from(inTime));
        ticket.setOutTime(Date.from(outTime));
        ticket.setParkingSpot(new ParkingSpot(4, parkingType, false));

        // Calcul et fixe du prix du ticket pour un utilisateur non récurent.
        fareCalculatorService.calculateFare(ticket, false); // isRecurrent is false

        // fixe avec les mêmes valeurs l'entrée et de sortie du ticket
        Ticket ticketRecurrentUser = new Ticket();
        ticketRecurrentUser.setInTime(Date.from(inTime));
        ticketRecurrentUser.setOutTime(Date.from(outTime));
        ticketRecurrentUser.setParkingSpot(new ParkingSpot(5, parkingType, false));

        // isRecurrent is true
        fareCalculatorService.calculateFare(ticketRecurrentUser, true);
        // on s'assure le ticket de l'utilisateur récurent est appliquée
        assertNotEquals(ticketRecurrentUser.getPrice(), ticket.getPrice(), "la réduction utilisateur " +  parkingType +  " n'a pas été appliquée");

        // on s'assure que le calcul de la réduction (moins 5%) est juste
        assertEquals(ticketRecurrentUser.getPrice(), ticket.getPrice() * 0.95, "le calcul de la réduction utilisateur " +  parkingType +  " récurrent est faux");
    }
}