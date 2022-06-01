package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    public void calculateFareCarWithLessThan30Minutes()
    {
        Instant outTime = Instant.now();
        Instant inTime = outTime.minus(28, ChronoUnit.MINUTES);

        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

        ticket.setInTime(Date.from(inTime));
        ticket.setOutTime(Date.from(outTime));
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(ticket.getPrice(), 0);
    }

    @Test
    public void calculateFareBikeWithLessThan30Minutes()
    {
        Instant outTime = Instant.now();
        Instant inTime = outTime.minus(28, ChronoUnit.MINUTES);

        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);

        ticket.setInTime(Date.from(inTime));
        ticket.setOutTime(Date.from(outTime));
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(ticket.getPrice(), 0);
    }

    @Test
    public void calculateFareCarWithMoreThan30Minutes()
    {
        Instant outTime = Instant.now();
        Instant inTime = outTime.minus(32, ChronoUnit.MINUTES);

        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

        ticket.setInTime(Date.from(inTime));
        ticket.setOutTime(Date.from(outTime));
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertTrue(ticket.getPrice() > 0);
    }

    @Test
    public void calculateFareBikeWithMoreThan30Minutes()
    {
        Instant outTime = Instant.now();
        Instant inTime = outTime.minus(32, ChronoUnit.MINUTES);

        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);

        ticket.setInTime(Date.from(inTime));
        ticket.setOutTime(Date.from(outTime));
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertTrue(ticket.getPrice() > 0);
    }



    @Test
    public void calculateRecurrentUserFareCar()
    {
        Instant outTime = Instant.now();
        Instant inTime = outTime.minus(10, ChronoUnit.DAYS);

        ParkingSpot parkingSpot1 = new ParkingSpot(1, ParkingType.CAR, false);
        ParkingSpot parkingSpot2 = new ParkingSpot(2, ParkingType.CAR, false);

        // non-recurrent user to compare
        ticket.setInTime(Date.from(inTime));
        ticket.setOutTime(Date.from(outTime));
        ticket.setParkingSpot(parkingSpot1);
        // isRecurrent is false
        fareCalculatorService.calculateFare(ticket, false);

        // recurrent user
        Ticket ticketRecurrentUser = new Ticket();
        ticketRecurrentUser.setInTime(Date.from(inTime));
        ticketRecurrentUser.setOutTime(Date.from(outTime));
        ticketRecurrentUser.setParkingSpot(parkingSpot2);
        // isRecurrent is true
        fareCalculatorService.calculateFare(ticketRecurrentUser, true);


        assertEquals(ticketRecurrentUser.getPrice(), ticket.getPrice() * 0.95);
    }

    @Test
    public void calculateRecurrentUserFareBike()
    {
        Instant outTime = Instant.now();
        Instant inTime = outTime.minus(10, ChronoUnit.DAYS);

        ParkingSpot parkingSpot1 = new ParkingSpot(4, ParkingType.BIKE, false);
        ParkingSpot parkingSpot2 = new ParkingSpot(5, ParkingType.BIKE, false);

        // non-recurrent user to compare
        ticket.setInTime(Date.from(inTime));
        ticket.setOutTime(Date.from(outTime));
        ticket.setParkingSpot(parkingSpot1);
        // isRecurrent is false
        fareCalculatorService.calculateFare(ticket, false);

        // recurrent user
        Ticket ticketRecurrentUser = new Ticket();
        ticketRecurrentUser.setInTime(Date.from(inTime));
        ticketRecurrentUser.setOutTime(Date.from(outTime));
        ticketRecurrentUser.setParkingSpot(parkingSpot2);
        // isRecurrent is true
        fareCalculatorService.calculateFare(ticketRecurrentUser, true);

        // compare recurrent and non-recurrent ticket price
        assertEquals(ticketRecurrentUser.getPrice(), ticket.getPrice() * 0.95);
    }
}