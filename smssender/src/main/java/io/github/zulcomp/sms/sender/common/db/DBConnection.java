/**
 * @(#)DBConnection.java
 *
 *
 * @author Faizul Ngsrimin
 * @version 1.00 2012/11/19
 */
package io.github.zulcomp.sms.sender.common.db;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import oracle.jdbc.pool.OracleDataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

public class DBConnection {

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
        p.setProperty("drivertype", "oracle");
        openConnection(p);
    }

    public int scalaQuery(String sql, HashMap param) throws SQLException {
        
        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.clearParameters();
            if(param!=null) {
                Set keys = param.keySet();
                Iterator i = keys.iterator();
                int idx;
                while (i.hasNext()) {
                    String key = (String) i.next();
                    idx = Integer.parseInt(key);
                    stmt.setObject(idx, param.get(key));
                }
                
                stmt.executeUpdate();
                return stmt.getUpdateCount();
            }
        }
        return -1;
    }

    public ArrayList<HashMap> query(String sql, HashMap param) throws SQLException {

        ArrayList<HashMap> lstResult = new ArrayList<>();
        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.clearParameters();
            if(param != null) {
                Set keys = param.keySet();
                Iterator iter = keys.iterator();
                int idx;
                String key;
                while (iter.hasNext()) {
                    key = (String) iter.next();
                    idx = Integer.parseInt(key);
                    stmt.setObject(idx, param.get(key));
                }
                
                try(ResultSet rset = stmt.executeQuery()) {
                    ResultSetMetaData rsmd = rset.getMetaData();
                    int colcount = rsmd.getColumnCount();
                    while(rset.next()) {
                        HashMap hm = new HashMap();
                        for(int c = 1; c <= colcount;c++)
                        {
                            Object obj = rset.getObject(c);
                            hm.put(c, obj);
                        }
                        lstResult.add(hm);
                    }
                }
            }
        }
        return lstResult;
    }

    public void closeConnection() throws SQLException {
        if (conn != null) {
            conn.close();
            conn = null;
        }
    }

    private void openConnection(Properties connProp) throws SQLException, ClassNotFoundException {
        String connstr, username, password, drivertype;

        connstr = connProp.getProperty("jdbc_url", "jdbc:oracle:thin:@");
        username = connProp.getProperty("dbuser","");
        password = connProp.getProperty("dbpasswd","");
        drivertype = connProp.getProperty("drivertype", "oracle");

        drivertype = drivertype.toLowerCase();
        if (conn != null) {
            return;
        }

        if (null == drivertype) {
            //this will use Jdbc-Odbc bridge
            Properties p = new Properties();
            p.put("user", username);
            p.put("password", password);
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            conn = DriverManager.getConnection(connstr, p);
        } else switch (drivertype) {
            case "oracle":
                OracleDataSource ods = new OracleDataSource();
                ods.setURL(connstr);
                ods.setUser(username);
                ods.setPassword(password);
                ods.setDriverType("thin");
                conn = ods.getConnection();
                break;
            case "mysql":
                MysqlDataSource mcpds = new MysqlDataSource();
                mcpds.setURL(connstr);
                mcpds.setUser(username);
                mcpds.setPassword(password);
                conn = mcpds.getConnection();
                break;
            default:
                //this will use Jdbc-Odbc bridge
                Properties p = new Properties();
                p.put("user", username);
                p.put("password", password);
                Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
                conn = DriverManager.getConnection(connstr, p);
                break;
        }
    }

    public boolean isClosed() throws SQLException {
        return (conn != null ? conn.isClosed() : true);
    }
}