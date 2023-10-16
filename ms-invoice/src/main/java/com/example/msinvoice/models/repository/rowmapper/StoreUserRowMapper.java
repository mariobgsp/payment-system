package com.example.msinvoice.models.repository.rowmapper;

import com.example.msinvoice.models.repository.StoreUser;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class StoreUserRowMapper implements RowMapper<StoreUser> {
    @Override
    public StoreUser mapRow(ResultSet rs , int rowNum) throws SQLException {
        StoreUser storeUser = new StoreUser();
        storeUser.setId(rs.getInt("ID"));
        storeUser.setUserId(rs.getString("USERID"));
        storeUser.setUserName(rs.getString("USERNAME"));
        storeUser.setFirstName(rs.getString("FIRSTNAME"));
        storeUser.setLastName(rs.getString("LASTNAME"));
        storeUser.setEmail(rs.getString("EMAIL"));
        storeUser.setHashPassword(rs.getString("PASSWORD"));
        storeUser.setSpecialProduct(Boolean.parseBoolean(rs.getString("SPECIALPRODUCT")));
        storeUser.setRecurring(Boolean.parseBoolean(rs.getString("RECURRING")));
        storeUser.setToken(rs.getString("TOKEN"));
        return storeUser;
    }
}
