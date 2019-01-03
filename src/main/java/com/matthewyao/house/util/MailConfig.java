package com.matthewyao.house.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @Author: yaokuan
 * @Date: 2019/1/1 下午10:14
 */
public class MailConfig {
    private static final String PROPERTIES_DEFAULT = "mailConfig.properties";
    public static String host;
    public static Integer port;
    public static String userName;
    public static String passWord;
    public static String emailForm;
    public static String emailTo;
    public static String timeout;
    public static String personal;
    public static Properties properties;
    static{
        init();
    }

    /**
     * 初始化
     */
    private static void init() {
        properties = new Properties();
        try{
            InputStream inputStream = MailConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_DEFAULT);
            properties.load(inputStream);
            inputStream.close();
            host = properties.getProperty("mailHost");
            port = Integer.parseInt(properties.getProperty("mailPort"));
            userName = properties.getProperty("mailUsername");
            passWord = properties.getProperty("mailPassword");
            emailForm = properties.getProperty("mailUsername");
            timeout = properties.getProperty("mailTimeout");
            emailTo = properties.getProperty("emailTo");
            personal = "莫失莫忘";//发送人
        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
