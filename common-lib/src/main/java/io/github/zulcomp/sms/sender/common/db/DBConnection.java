/**
 * @(#)DBConnection.java
 *
 *
 * @author Faizul Ngsrimin
 * @version 1.00 2012/11/19
 */
package io.github.zulcomp.sms.sender.common.db;


import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;
import oracle.jdbc.pool.OracleDataSource;

import java.sql.*;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DBConnection {

    private Logger logger = LogManager.getLogger(getClass());
    private Connection conn;

    public DBConnection() {
    }

    public DBConnection(Properties p) throws SQLException, ClassNotFoundException {
        openConnection(p);
    }

    public DBConnection(String connstr, String username, String password, String drivertype) throws SQLException, ClassNotFoundException {
        Properties p = new Properties();
        p.setProperty("jdbc_url", connstr);
        p.setProperty("dbuser", username);
        p.setProperty("dbpasswd", password);
        p.setProperty("drivertype", drivertype);
        openConnection(p);
    }

    public int scalarQuery(String sql) throws SQLException {
        return scalarQuery(sql, null);
    }

    public int scalarQuery(String sql, Map<String,String> param) throws SQLException {
        
        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.clearParameters();
            if(param!=null && !param.isEmpty()) {
                for (String key : param.keySet()) {
                    stmt.setObject(Integer.parseInt(key), param.get(key));
                }
            }
            stmt.executeUpdate();
            return stmt.getUpdateCount();
        }
    }

    public List<Map<Integer, Object>> query(String sql, Map<String,String> param) throws SQLException {

        List<Map<Integer, Object>> lstResult = new ArrayList<>();
        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.clearParameters();
            if(param != null && !param.isEmpty()) {
                for (String key : param.keySet()) {
                    stmt.setObject(Integer.parseInt(key), param.get(key));
                }
            }
                try(ResultSet resultSet = stmt.executeQuery()) {
                    ResultSetMetaData rsmd = resultSet.getMetaData();
                    int columnCount = rsmd.getColumnCount();
                    while(resultSet.next()) {
                        Map<Integer,Object> hm = new HashMap<>();
                        for(int c = 1; c <= columnCount;c++)
                        {
                            Object obj = resultSet.getObject(c);
                            hm.put(c, obj);
                        }
                        lstResult.add(hm);
                    }
                }

        }
        return lstResult;
    }

    public void closeConnection() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException sqlException) {
                logger.debug("SqlException: ", sqlException);
            }
            conn = null;
        }
    }

    private void openConnection(Properties connProp) throws SQLException, ClassNotFoundException {
        String connectionString;
        String username;
        String password;
        String driverType;

        connectionString = connProp.getProperty("jdbc_url", "jdbc:oracle:thin:@");
        username = connProp.getProperty("dbuser","");
        password = connProp.getProperty("dbpasswd","");
        driverType = connProp.getProperty("drivertype", "jdbc-odbc");

        driverType = driverType.toLowerCase();
        if (conn != null) {
            return;
        }
        String realConnectionURL = validateConnectionString(connectionString, driverType);
        switch (driverType) {
            case "oracle":
                OracleDataSource ods = new OracleDataSource();
                ods.setURL(realConnectionURL);
                ods.setUser(username);
                ods.setPassword(password);
                ods.setDriverType("thin");
                conn = ods.getConnection();
                break;
            case "mysql":
                MysqlDataSource mcpds = new MysqlDataSource();
                mcpds.setURL(realConnectionURL);
                mcpds.setUser(username);
                mcpds.setPassword(password);
                conn = mcpds.getConnection();
                break;
            case "mssql":
                SQLServerDataSource sqlSvrDS = new SQLServerDataSource();
                sqlSvrDS.setURL(realConnectionURL);
                sqlSvrDS.setUser(username);
                sqlSvrDS.setPassword(password);
                conn = sqlSvrDS.getConnection();
                break;
            default:
               throw new SQLException("driverType ["+driverType+"] not supported");
        }
    }

    private String validateConnectionString(String connectionString, String driverType) {

        return connectionString;
    }

    public boolean isClosed() throws SQLException {
        return (conn != null ? conn.isClosed() : true);
    }
}