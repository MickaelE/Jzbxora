package com.scotttiger.zbxora;

import ca.szc.configparser.Ini;
import ca.szc.configparser.exceptions.IniParserException;

import java.io.File;
import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;


public class MainClient {
    public MainClient() {
        super();
    }

    /**
     * @throws IniParserException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public static void main(String[] args) throws IniParserException, IOException, ClassNotFoundException,
                                                  SQLException {
        // Read from a file
        File cfgPath = new File(System.getProperty("user.dir") + "/etc/");
        File[] listOfFiles = cfgPath.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                System.out.println("File " + listOfFiles[i].getName());
                Path input = Paths.get(listOfFiles[i].getCanonicalFile().toString());
                execute(input);
            } else if (listOfFiles[i].isDirectory()) {
                System.out.println("Directory " + listOfFiles[i].getName());
            }
        }
        ;
    }

    private static void execute(Path input) throws IniParserException, IOException, ClassNotFoundException,
                                                   SQLException {
        Ini ini = new Ini().read(input);
        List<String> delim = Arrays.asList(":");
        ini.setDelimiters(delim);
        Map<String, Map<String, String>> sections = ini.getSections();
        String site_checks = sections.get("zbxora").get("site_checks");

        // Change all options called foo to bar
        if (!site_checks.isEmpty()) {
            MainClient client = new MainClient();
            client.siteChecks(sections, input.getFileName().toString());
        }

    }

    private void siteChecks(Map<String, Map<String, String>> sections, String filename) throws ClassNotFoundException,
                                                                                               SQLException,
                                                                                               IOException,
                                                                                               IniParserException {
        List<String> output = new ArrayList();
        String dbVersion = "", mySID = "", iType = "", iName = "", uName = "", dbRole = "", db_type = "oracle";
        Double mySerial = 0d;
        Boolean i = false;
        String CHECKSFILE = "";
        Float sql_timeouts = 0f;
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String site_checks = System.getProperty("user.dir") + "/" + sections.get("zbxora").get("site_checks");
        output.add(String.format("%s site_checks: %s\n", timestamp.toString(), site_checks).toString());

        String to_zabbix_method = sections.get("zbxora").get("to_zabbix_method");
        String to_zabbix_args = sections.get("zbxora").get("to_zabbix_args");
        output.add(String.format("%s to_zabbix_method: %s %s\n", timestamp.toString(), to_zabbix_method, to_zabbix_args));

        String out_dir = sections.get("zbxora").get("out_dir");
        String db_url = sections.get("zbxora").get("db_url");
        String username = sections.get("zbxora").get("username");
        String password = sections.get("zbxora").get("password");
        String role = sections.get("zbxora").get("role");
        String  hostname = sections.get("zbxora").get("hostname");
        String checks_dir = sections.get("zbxora").get("checks_dir");

        String outfile = out_dir + "/" + FilenameUtils.getBaseName(filename) + ",zbx";
        output.add(String.format("%s out_file:%s\n", timestamp.toString(), outfile).toString());

        Class.forName("oracle.jdbc.driver.OracleDriver");

        Connection connection = null;
        connection = DriverManager.getConnection("jdbc:oracle:thin:@" + db_url, username, password);

        Statement sqlStatement = connection.createStatement();
        String readRecordSQL =
            "select substr(i.version,0,instr(i.version,'.')-1),\n" +
            "                            s.sid, s.serial#, p.value instance_type, i.instance_name\n" +
            "                            , s.username\n" +
            "                            from v$instance i, v$session s, v$parameter p \n" +
            "                            where s.sid = (select sid from v$mystat where rownum = 1)\n" +
            "                            and p.name = 'instance_type'";

        ResultSet myResultSet = sqlStatement.executeQuery(readRecordSQL);
        while (myResultSet.next()) {

            dbVersion = myResultSet.getString(1);
            mySID = myResultSet.getString(2);
            mySerial = myResultSet.getDouble(3);
            iType = myResultSet.getString(4);
            iName = myResultSet.getString(5);
            uName = myResultSet.getString(6);
        }

        //TODO Error handling of connection.
        if (iType.contains("RDBMS")) {
            readRecordSQL = "select database_role from v$database";
            myResultSet = sqlStatement.executeQuery(readRecordSQL);
            while (myResultSet.next()) {
                dbRole = myResultSet.getString(1);
            }
        } else {
            dbRole = "asm";
        }

        output.add("%s connected db_url %s " + db_url + "type %s " + iType + "db_role %s " + dbRole + "version %s" +
                   dbVersion + "\n%s " + timestamp.toString() + "user %s %s " + username + uName + "sid,serial %d,%d " +
                   mySID + mySerial + "instance %s " + iName + "as %s\n" + role + "\n");

        if (iType.contains("asm"))
            CHECKSFILE = checks_dir + "/" + db_type + "/" + dbRole + "." + dbVersion + ".cfg";
        else if (dbRole.contains("PHYSICAL STANDBY"))
            CHECKSFILE = checks_dir + "/" + db_type + "/" + "dbRole" + "." + dbVersion + ".cfg";
        else
            CHECKSFILE = checks_dir + "/" + db_type + "/" + dbRole + "." + dbVersion + ".cfg";

        try {
            sql_timeouts = Float.valueOf(sections.get("zbxora").get("sql_timeout"));
        } catch (Exception ex) {
            sql_timeouts = 60f;
        }
        output.add(String.format("%s using sql_timeout %s \n", timestamp.toString(), sql_timeouts));

        // starting the actual checks.
        File f = new File(CHECKSFILE);
        if (f.exists() && !f.isDirectory()) {
            output.add(String.format("%s using checkfile %s\n", timestamp.toString(), CHECKSFILE));
        } else {
            output.add(String.format("%s checkfile dont exists: %s\n", timestamp.toString(), CHECKSFILE));
        }
        long startTime = System.currentTimeMillis();
        long elapsedTime = 0L;
        Arrays.asList(":");
        List<String> comment = Arrays.asList("#");
        Ini ini = new Ini();
        ini.setCommentPrefixes(comment);

        while (i != true) {
            ini.read(f.toPath());
            Map<String, Map<String, String>> sectionsTest = ini.getSections();
            Set<String> valuesC = sectionsTest.keySet();
            Iterator<String> values = valuesC.iterator();
            while (values.hasNext()) {
                String val = values.next();
            //   System.out.println(val);
                if (val.contains("discover")){
                    Map<String, String> sec =  sectionsTest.get(val);
                    Set<String> secC = sec.keySet();
                    Iterator<String> secCc = secC.iterator();
                    String res =hostname + " ";
                    while (secCc.hasNext()) {
                        String key = secCc.next();
                   // System.out.println( sec.get(key));
                     readRecordSQL =sec.get(key);
                    readRecordSQL = readRecordSQL.replace("$$", "$");
                        if(!sqlStatement.isClosed() && readRecordSQL.contains("select")){
                            //try {
                            ResultSet rs = sqlStatement.executeQuery(readRecordSQL);
                        ResultSetMetaData rsmd = rs.getMetaData();

                            while (rs.next()) {
                            res = res + key + "";
                                
                                int rownum = rsmd.getColumnCount();
                                for(int l=1; l <= rownum ;l++){
                                    res= res + rs.getString(l);
                                }
                                System.out.println("Result " + res);
                         //   }
                        //} catch (SQLException sqle) {
                            // TODO: Add catch code
                          //  System.out.println(sqle);
                        }
                    }
                }
                }
            }
                /*   Iterator<Map<String,String>> values = valuesC.iterator();
                while (values.hasNext()) {
                
                    Map<String,String> value = values.next();
                   Set<String> ke =  values.next().keySet();
                    System.out.println( value.values());
                } */

                while (elapsedTime < 360000) {
                // sections.get("zbxora").get("to_zabbix_method")
                 //  String name= sectionsTest.toString();
                    
            }
            elapsedTime = (new Date()).getTime() - startTime;
        }

        connection.close();
    }

}
