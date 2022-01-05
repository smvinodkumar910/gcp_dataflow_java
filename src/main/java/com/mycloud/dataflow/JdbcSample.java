package com.mycloud.dataflow;

import java.sql.*;
import com.exasol.jdbc.*;

public class JdbcSample
{
    
    public static void main(String[] args)
    {
        try { Class.forName("com.exasol.jdbc.EXADriver");
    } catch (ClassNotFoundException e) { 
            e.printStackTrace();
    }
    Connection con=null; 
    Statement stmt=null; 
    try {
        con = DriverManager.getConnection( 
            "jdbc:exa:DEMODB.EXASOL.COM;schema=SYS",
            "PUB4213", 
            "zH25D1ZyL"
            );
        stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM CAT"); 
        System.out.println("Schema SYS contains:"); 
        while(rs.next())
        {
            String str1 = rs.getString("TABLE_NAME"); 
            String str2 = rs.getString("TABLE_TYPE"); 
            System.out.println(str1 + ", " + str2);
        }
    } catch (SQLException e) { 
        e.printStackTrace();
    } finally {
        try {stmt.close();} catch (Exception e) {e.printStackTrace();} 
        try {con.close();} catch (Exception e) {e.printStackTrace();}
        }
    }
}