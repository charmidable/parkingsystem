package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

import java.time.Duration;

public class FareCalculatorService
{
    public void calculateFare(Ticket ticket)
    {
        calculateFare(ticket,  false);
    }

    public void calculateFare(Ticket ticket, boolean isRecurrentUser)
    {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())))
        {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }


        //TODO: Some tests are failing here. Need to check if this logic is correct


        Duration parkDuration = Duration.between(ticket.getInTime().toInstant(), ticket.getOutTime().toInstant());


        double parkDurationInHours = parkDuration.toHours() + ((double)(parkDuration.minusHours(parkDuration.toHours()).toMinutes())/60);

        if(parkDurationInHours > 0.5)
        {
            switch (ticket.getParkingSpot().getParkingType())
            {
                case CAR:
                {
                    ticket.setPrice(parkDurationInHours * Fare.CAR_RATE_PER_HOUR);
                    break;
                }
                case BIKE:
                {
                    ticket.setPrice(parkDurationInHours * Fare.BIKE_RATE_PER_HOUR);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unkown Parking Type");
            }

            if(isRecurrentUser)
            {
                System.out.println("As a recurring user you have just received a discount of 5%.");
                ticket.setPrice(ticket.getPrice()*0.95);
            }
        }
        else
        {
            ticket.setPrice(0);
        }
    }
}