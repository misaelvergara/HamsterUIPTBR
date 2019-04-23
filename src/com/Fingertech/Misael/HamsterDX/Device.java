/************************************************************
 * onEventCloseWindow = terminate all pending connections   *
 *
 *
 *
 * **********************************************************
 */

package com.Fingertech.Misael.HamsterDX;

import com.nitgen.SDK.BSP.NBioBSPJNI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

import com.mysql.jdbc.Driver;

import java.util.concurrent.TimeUnit;

import java.sql.*;

public class Device {

    /*.............................................................
      ..... DEFAULT DECLARATIONS ..................................
      .............................................................
     */
    //number of messages counter
    private static int outCounter = 0;
    //integer which represents the currently opened device in an array list
    private int deviceIdInt;

    //instantiates the user interface class
    DeviceUI UI = new DeviceUI();

    //logging method
    private void out(String msg) {
        UI.log(UI.log_textarea.getText() + "[" + outCounter++ + "] " + msg + "\n");
        System.out.println(msg);
    }

    //default sdk declarations
    private NBioBSPJNI bsp = new NBioBSPJNI();
    private NBioBSPJNI.DEVICE_ENUM_INFO deviceEnumInfo;
    private NBioBSPJNI.FIR_HANDLE hSavedFIR;
    private NBioBSPJNI.FIR_TEXTENCODE textSavedFIR;
    private NBioBSPJNI.FIR_TEXTENCODE dbReturnedTXTFIR;
    private NBioBSPJNI.INPUT_FIR inputFIR;
    private NBioBSPJNI.INPUT_FIR inputFIR2;
    private NBioBSPJNI.FIR_HANDLE captureHandle;
    private NBioBSPJNI.IndexSearch indexSearch;
    private NBioBSPJNI.IndexSearch.SAMPLE_INFO sampleInfo;
    //private NBioBSPJNI.Export objExport;
    //private NBioBSPJNI.Export.DATA objExportDATA;
    //private byte[] firByteTemplate;

    //static value that allows cross-method connectivity without declaring it multiple times
    private static Connection connection;


    /*.............................................................
      ..... NESTED CLASSES ........................................
      .............................................................
     */

    //program status information class
    private static class Self {
        private static boolean deviceConnected = false;
        private static boolean behaveOpenButton = false;
        private static boolean dbConnected = false;
        private static String firRecord;
        private static String remoteFirFromDB;
        private static String lastPromptedId = "0";

        private static void init() {
            deviceConnected = false;
        }
    }

    //database access class
    private class Database {
        String db = "hamster";
        String user = "test";
        String pw = "9996";
        String locale = "localhost:3306";

        String url = "jdbc:mysql://" + locale + "/" +
                db + "?user=" + user + "&password=" +
                pw + "&useTimezone=true&serverTimezone=UTC";

        //accesses database and returns whether the connection was successful or not
        public boolean access() {
            try {
                //verifies if the driver class is present
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                System.out.println(e);
                return false;
            }

            try {
                //connects to the database
                out(Msg.DB.YES);
                connection = DriverManager.getConnection(url);
                Self.dbConnected = true;
                return true;

            } catch (SQLException e) {
                System.out.println("/!\\ CANNOT ACCESS DATABASE: " + e);
                return false;
            }
        }

        //leaves connection
        public void leave() {
            try {
                connection.close();
                out(Msg.DB.CLOSED);
            } catch (SQLException e) {
                System.out.println("/!\\ CANNOT CLOSE DATABASE: " + e);
            }
        }

        //returns user fingerprint data from the database
        public String select(String id) {
            try {

                //checks if the currently prompted id is equal to the last prompted id
                if (Self.lastPromptedId == id) {
                    return Self.remoteFirFromDB;

                } else {

                    Self.lastPromptedId = id;
                    String query = "SELECT * FROM user_data WHERE id = " + id + ";";
                    PreparedStatement newStatement = connection.prepareStatement(query);
                    ResultSet resSet = newStatement.executeQuery();

                    String data = "";
                    while (resSet.next()) {
                        data = resSet.getString("fir_data");
                    }

                    Self.remoteFirFromDB = data;
                    return data;
                }

            } catch (SQLSyntaxErrorException e) {
                System.out.println("/!\\ ITEM NOT FOUND: " + e);

            } catch (SQLException e) {
                System.out.println("/!\\ CANNOT LOOK UP DATA STRUCTURE: " + e);
            }
            return "";
        }

        /* Returns the selected database table number of rows.
           This number represents the id of the last registered user.
        */
        public int returnID() {
            try {
                String query = "SELECT count(*) FROM user_data;";
                Statement newStatement = connection.createStatement();
                ResultSet resSet = newStatement.executeQuery(query);

                resSet.next();
                int count = resSet.getInt(1);
                return count;
            } catch (Exception e) {
                System.out.println(e);
                return 0;
            }
        }

        /* Utility: stores fingerprint data in the Mysql database
           Parameter: fingerprint database
         */
        public void save(String fir) {
            try {
                String query = "INSERT INTO user_data (fir_data) VALUES (?);";
                PreparedStatement newStatement = connection.prepareStatement(query);
                newStatement.setString(1, fir);
                newStatement.execute();
                out(Msg.DB.SAVED);

            } catch (SQLSyntaxErrorException e) {
                System.out.println("/!\\ ITEM NOT FOUND: " + e);
            } catch (SQLException e) {
                System.out.println("/!\\ CANNOT LOOK UP DATA STRUCTURE: " + e);
            }
        }
    }

    Device.Database database = new Device.Database();

    /*.............................................................
      ..... CLASS METHODS .........................................
      .............................................................
     */

    //looks up for connected devices
    private boolean doLookUpConnected() {
        //nº of connected devices
        int connectedDevices;
        Self.deviceConnected = false;

        deviceEnumInfo = bsp.new DEVICE_ENUM_INFO();
        bsp.EnumerateDevice(deviceEnumInfo);
        //instantiates DEVICE_ENUM_INFO and passes its values onto EnumerateDevice for
        //a quick check up

        if (bsp.IsErrorOccured()) {
            out("[REASON] SDK Triggered Error");
            return false;
        }

        connectedDevices = deviceEnumInfo.DeviceCount;

        if (connectedDevices == 0) {
            out(Msg.NO_DEVICES_WERE_FOUND);
            return false;
        }

        out(Msg.DEVICES_WERE_FOUND);

        for (int i = 0; i < connectedDevices; i++) {

            out("DISPOSITIVO: " +
                    deviceEnumInfo.DeviceInfo[i].Name + " - " +
                    deviceEnumInfo.DeviceInfo[i].Instance);

            //adds them to the UI list of devices
            addToDeviceList(i);
        }
        return true;

    }

    //connects to a selected device
    private boolean connectTo() {
        //number representing the list item selected
        deviceIdInt = UI.device_list.getSelectedIndex();

        if (!Self.deviceConnected) {
            /*DeviceInfo is an array that contains the number of connected devices. Thus, its order
             is related to the position that a device was placed in the UI list.
             */
            bsp.OpenDevice(deviceEnumInfo.DeviceInfo[deviceIdInt].NameID,
                    deviceEnumInfo.DeviceInfo[deviceIdInt].Instance);
        }

        if (bsp.IsErrorOccured()) {
            return false;
        }

        whenReconnect();
        Self.deviceConnected = true;
        return true;
    }

    private void closeDevice() {
        bsp.CloseDevice();
    }

    /*  . realiza a captura da uma digital
        . abre a interface do leitor
        . relaciona os dados coletados à uma string
     */
    private boolean doCapture() {
        /*  Erases data of past captured fingerprints.
         */
        if (hSavedFIR != null) {
            hSavedFIR.dispose();

        }
        hSavedFIR = null;
        Self.firRecord = null;

        /*  Instantiates the handler class.
         */
        hSavedFIR = bsp.new FIR_HANDLE();

        if (bsp.IsErrorOccured()) {
            out(Msg.Capture.ERROR_TRIGGERED);
            return false;
        }

        /*  Assigns the handler class as a parameter to the capture method.
            The method returns hSavedFir as a recipient of the fingerprint data
            promptly collected.
         */
        try {
            bsp.Capture(0, hSavedFIR, 10000, null, null);
            out(Msg.Capture.SUCCESS);
        } catch (Exception e) {
            System.out.println(e);
        }

        /*  Instantiates the text encoding class.
         */
        textSavedFIR = bsp.new FIR_TEXTENCODE();

        /*  Saves the captured fingerprint and translates it into
            a string.
         */
        bsp.GetTextFIRFromHandle(hSavedFIR, textSavedFIR);

        //string containing the fingerprint data
        Self.firRecord = textSavedFIR.TextFIR;

        System.out.println("Tamanho da string: " + Self.firRecord.length());
        return true;
    }

    // . captura uma nova digital para comparação à previamente capturada
    private void doMatch() {
        out(Msg.Verify.INIT);

        //resets sampleInfo data
        if (sampleInfo != null) {
            sampleInfo = null;
        }

        inputFIR = bsp.new INPUT_FIR();
        inputFIR2 = bsp.new INPUT_FIR();
        captureHandle = bsp.new FIR_HANDLE();
        NBioBSPJNI.FIR_PAYLOAD payload = bsp.new FIR_PAYLOAD();
        boolean matchSucceeded = false;

        // refers to the text encoded object
        inputFIR.SetTextFIR(textSavedFIR);

        //captures a new fingerprint using captureHandle as a recipient
        try {
            bsp.Capture(0, captureHandle, 10000, null, null);
        } catch (Exception e) {
            System.out.println(e);
        }

        if (bsp.IsErrorOccured()) {
            out(Msg.Verify.ERROR_NEAR_CAPTURE);
            return;
        }
        // refers to the handle object
        inputFIR2.SetFIRHandle(captureHandle);
        out(Msg.Verify.MATCHING_STARTED);
        /*
        indexSearch = bsp.new IndexSearch();
        indexSearch.AddFIR(inputFIR, 0, sampleInfo);
        indexSearch.SaveDB("C:\\Debug\\fingerprint_sample.db");

        objExport = bsp.new Export();
        objExportDATA = objExport.new DATA();

        objExport.ExportFIR(inputFIR, objExportDATA, 34);

        if (firByteTemplate != null) {
            firByteTemplate = null;
        }

        firByteTemplate = objExportDATA.FingerData[0].Template[0].Data;

        System.out.println(firByteTemplate);*/


        if (bsp.IsErrorOccured()) {
            out("err");
            return;
        }

        //returns the verification status through matchSucceeded
        bsp.VerifyMatch(inputFIR2, inputFIR, matchSucceeded, payload);

        if (matchSucceeded) {
            out(Msg.Verify.YES);
        } else {
            out(Msg.Verify.NO);
        }
    }

    /*  . parâmetro: dados biométricos de uma digital do banco
        . captura uma nova digital e a compara ao parâmetro recebido
    */
    private void compareToDb(String fingerprint) {
        if (sampleInfo != null) {
            sampleInfo = null;
        }

        inputFIR = bsp.new INPUT_FIR();
        inputFIR2 = bsp.new INPUT_FIR();
        captureHandle = bsp.new FIR_HANDLE();
        NBioBSPJNI.FIR_PAYLOAD payload = bsp.new FIR_PAYLOAD();

        Boolean matchSucceeded = false;

        /*  Attributes the fingerprint data to a new instance of
            FIR_TEXTENCODE. The attributed string will be read and
            assigned to a handle which will deal with the data, rendering
            it for comparison.
         */
        dbReturnedTXTFIR = bsp.new FIR_TEXTENCODE();
        dbReturnedTXTFIR.TextFIR = fingerprint;
        inputFIR.SetTextFIR(dbReturnedTXTFIR);

        try {
            bsp.Capture(captureHandle);
        } catch (Exception e) {
            System.out.println(e);
        }

        if (bsp.IsErrorOccured()) {
            out(Msg.Verify.ERROR_NEAR_CAPTURE);
            return;
        }

        inputFIR2.SetFIRHandle(captureHandle);
        out(Msg.Verify.MATCHING_STARTED);


        if (bsp.IsErrorOccured()) {
            out("err");
        }
        bsp.VerifyMatch(inputFIR2, inputFIR, matchSucceeded, payload);

        if (matchSucceeded) {
            out(Msg.Verify.YES);
        } else {
            out(Msg.Verify.NO);
        }
    }

    private void addToDeviceList(int i) {
        UI.device_list.addItem(deviceEnumInfo.DeviceInfo[i].Name +
                "-" + deviceEnumInfo.DeviceInfo[i].Instance);
    }

    private void initialProcedures() {
        /*  Greets user and asks them to plug in their device.
         */
        out(Msg.GREET);

    }

    private boolean canSaveFirToDB() {
        if (Self.firRecord != null) {
            UI.saveString_button.setEnabled(true);
            return true;
        } else {
            UI.saveString_button.setEnabled(false);
            return false;
        }
    }

    private void initializeButtons() {
        UI.match_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doMatch();
            }
        });

        UI.capture_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (doCapture()) {
                    UI.match_button.setEnabled(true);

                    if (Self.dbConnected) {
                        UI.saveString_button.setEnabled(true);
                    }
                }
            }
        });

        UI.lookUp_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (!Self.behaveOpenButton) {
                    out(Msg.LOOKING_UP_DEVICES);
                    closeDevice();
                    UI.buttonsEnabled(false);
                    if (doLookUpConnected()) {
                        //has devices
                        UI.lookUp_button.setText(Msg.Button.OPEN);
                        UI.buttonsEnabled(false);
                        UI.enableDBCompare(false);
                        UI.enableDBButtons(false);
                        Self.behaveOpenButton = true;
                    }
                } else {
                    if (connectTo()) {
                        out(Msg.DEVICE_OPENED);
                        UI.lookUp_button.setText(Msg.Button.FIND);
                        UI.device_list.removeAllItems();

                        UI.buttonsEnabled(true);
                        UI.accessDB_button.setEnabled(true);
                        UI.accessDB_button.setText("Conectar ao banco..");
                        UI.match_button.setEnabled(false);

                        Self.behaveOpenButton = false;
                    } else {
                        out(Msg.DEVICE_OPENING_FAILED);
                        UI.lookUp_button.setText(Msg.Button.RETRY);
                        UI.device_list.removeAllItems();
                        Self.behaveOpenButton = false;
                    }
                }


            }
        });

        UI.accessDB_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (database.access()) {
                    UI.accessDB_button.setText("CONECTADO");
                    UI.accessDB_button.setEnabled(false);
                    UI.enableDBCompare(true);

                    if (canSaveFirToDB()) {
                        System.out.println("/:-)\\ CAN SAVE TO DB");
                    }
                    Self.dbConnected = true;
                } else {
                    out(Msg.DB.NO);
                }

            }
        });

        UI.saveString_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                database.save(Self.firRecord);
                int id = database.returnID();
                out("Número do ID para o usuário: " + Integer.toString(id));
            }
        });

        UI.compare_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = UI.userDbID_text.getText();
                id = (id != null) ? id : "1";
                String fingerprint = database.select(id);
                System.out.println(fingerprint);
                compareToDb(fingerprint);
            }
        });
    }

    private void whenReconnect() {
        if (hSavedFIR != null) {
            hSavedFIR.dispose();

        }
        hSavedFIR = null;
        Self.firRecord = null;

        if (Self.dbConnected) {
            database.leave();
        }
        Self.dbConnected = false;
    }

    /*.............................................................
      ..... CLASS CONSTRUCTOR .....................................
      .............................................................
     */

    private void startUI() {
        //creates a new instance of JFrame
        JFrame frame = new JFrame("Interface de Testes - Hamster DX/III");

        //instantiates the DeviceUI class

        //attributes a content panel to the jframe
        frame.setContentPane(UI.home_panel);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void constructor() {
        initialProcedures();
        initializeButtons();
    }

    public Device() {
        startUI();
        out(Msg.INIT);

        constructor();
    }
}
