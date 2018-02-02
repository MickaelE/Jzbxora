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
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;


public class MainClient {
    public MainClient() {
        super();
    }
    public static void main(String [ ] args) throws IniParserException, IOException, ClassNotFoundException,
                                                  SQLException {
        // Read from a file
        File cfgPath = new File(System.getProperty("user.dir") +"/etc/");
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
    
    private static void execute(Path input)throws IniParserException, IOException, ClassNotFoundException,
                                                   SQLException {
        Ini ini = new Ini().read(input);
        List<String> delim =  Arrays.asList(":");
        ini.setDelimiters(delim);
        Map<String, Map<String, String>> sections = ini.getSections();
        String site_checks =  sections.get("zbxora").get("site_checks");
                                                                                                             
        // Change all options called foo to bar
        if(!site_checks.isEmpty()) {
            MainClient client = new MainClient();
            client.siteChecks(sections,input.getFileName().toString());
        }

    }
    private void siteChecks(Map<String, Map<String, String>> sections , String filename) throws ClassNotFoundException,
                                                                                               SQLException {
        List output = new ArrayList();
        StringBuilder sbuf = new StringBuilder();
        Formatter fmt = new Formatter(sbuf);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String site_checks =  sections.get("zbxora").get("site_checks");
        output.add(fmt.format("%s site_checks: %s\n", timestamp.toString(), site_checks).toString());
        
        String to_zabbix_method =  sections.get("zbxora").get("to_zabbix_method");
        String to_zabbix_args =  sections.get("zbxora").get("to_zabbix_args");
        output.add(fmt.format("%s to_zabbix_method: %s %s\n",  timestamp.toString(), to_zabbix_method, to_zabbix_args));
        
        String out_dir =  sections.get("zbxora").get("out_dir");
        String db_url=  sections.get("zbxora").get("db_url");
        String username =  sections.get("zbxora").get("username");
        String password =  sections.get("zbxora").get("password");
        String role =  sections.get("zbxora").get("role");
        String hostname =  sections.get("zbxora").get("hostname");
        String checks_dir =  sections.get("zbxora").get("checks_dir");
        
        String outfile = out_dir + "/" +  FilenameUtils.getBaseName(filename) + ",zbx" ;
        output.add(fmt.format("%s out_file:%s\n", timestamp.toString(), outfile).toString());
        
        Class.forName("oracle.jdbc.driver.OracleDriver");
        Connection connection = null;
        connection = DriverManager.getConnection("jdbc:oracle:thin:@" + db_url ,username,password);
       
        Statement sqlStatement = myConnection.createStatement();
                    String readRecordSQL = "select substr(i.version,0,instr(i.version,'.')-1),\n" + 
                    "                            s.sid, s.serial#, p.value instance_type, i.instance_name\n" + 
                    "                            , s.username\n" + 
                    "                            from v$instance i, v$session s, v$parameter p \n" + 
                    "                            where s.sid = (select sid from v$mystat where rownum = 1)\n" + 
                    "                            and p.name = 'instance_type'";  
                    
                    ResultSet myResultSet = sqlStatement.executeQuery(readRecordSQL);
                    while (myResultSet.next()) {
                        System.out.println("Record values: " + myResultSet.getString("WORK_ORDER_NO"));
                    }
                    myResultSet.close();
        
        connection.close();
    }
    
}
