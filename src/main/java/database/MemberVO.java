package database;

import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;

@ToString
public class MemberVO {

    private final String MID;
    private final String PASSWORD;
    private final String NAME;
    private final String EMAIL;

    public MemberVO(ResultSet resultSet) throws SQLException {
        this.MID = resultSet.getString("MID");
        this.PASSWORD = resultSet.getString("PASSWORD");
        this.NAME = resultSet.getString("NAME");
        this.EMAIL = resultSet.getString("EMAIL");
    }

    public String getPassword() {
        return PASSWORD;
    }
}