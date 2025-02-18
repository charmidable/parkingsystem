package com.parkit.parkingsystem.dao;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ParkingSpotDAO
{
    private static final Logger logger = LogManager.getLogger("ParkingSpotDAO");

    public DataBaseConfig dataBaseConfig = new DataBaseConfig();

    public int getNextAvailableSlot(ParkingType parkingType)
    {
        Connection con = null;
        int result = -1;
        try
        {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.GET_NEXT_PARKING_SPOT);
            ps.setString(1, parkingType.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next())
            {
                result = rs.getInt(1);
            }
            dataBaseConfig.closeResultSet(rs);
            dataBaseConfig.closePreparedStatement(ps);
        }
        catch (Exception ex)
        {
            logger.error("Error fetching next available slot", ex);
        }
        finally
        {
            dataBaseConfig.closeConnection(con);
        }
        return result;
    }

    public boolean updateParking(ParkingSpot parkingSpot)
    {
        //update the availability fo that parking slot
        Connection con = null;
        try
        {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.UPDATE_PARKING_SPOT);
            ps.setBoolean(1, parkingSpot.isAvailable());
            ps.setInt(2, parkingSpot.getId());
            int updateRowCount = ps.executeUpdate();
            dataBaseConfig.closePreparedStatement(ps);
            return (updateRowCount == 1);
        }
        catch (Exception ex)
        {
            logger.error("Error updating parking info", ex);
            return false;
        }
        finally
        {
            dataBaseConfig.closeConnection(con);
        }
    }

    public boolean isItAnAvailableSlot(long parkingNumber)
    {
        Boolean isAvaible = null;

        try
        (
            Connection          con = dataBaseConfig.getConnection();
            PreparedStatement   ps  = con.prepareStatement(DBConstants.IS_PARKING_SPOT_AVAILABLE);
        )
        {
            ps.setLong(1, parkingNumber);

            try (ResultSet rs = ps.executeQuery())
            {
                rs.next();
                isAvaible = rs.getBoolean(1);
            }
        }
        catch (SQLException | ClassNotFoundException ex)
        {
            logger.error("Error in available slot", ex);
        }
        return isAvaible;
    }

    public int countSlotByType(ParkingType parkingType)
    {
        Integer numberOfSlot = null;

        try
        (
            Connection          con = dataBaseConfig.getConnection();
            PreparedStatement   ps  = con.prepareStatement(DBConstants.COUNT_SLOT_BY_TYPE);
        )
        {
            ps.setString(1, parkingType.name());

            try (ResultSet rs = ps.executeQuery())
            {
                rs.next();
                numberOfSlot = rs.getInt(1);
            }
        }
        catch (SQLException | ClassNotFoundException ex)
        {
            logger.error("Error in  countSlotByType", ex);
        }
        return numberOfSlot;
    }
}