/**********************************************************************
 * PROJECT IDENTITY ==========> .......................................
 * . (a) INTERACT WITH FINGERPRINT SCANNERS HAMSTER DX and HAMSTER III.
 * . (b) READ FINGERPRINT DATA STORED IN A DATABASE ...................
 * . (c) COMPARE FINGERPRINT DATA .....................................
 * . (d) SCAN FINGERPRINTS ............................................
 * . (e) STORE FINGERPRINT DATA IN A DATABASE .........................
 * . (f) CLIENT-USER INTERACTION THROUGH USER INTERFACE ...............
 * DRIVER DEPENDENCY ==========> ......................................
 * . (a), (c), (d) [NBioBSPJNI.jar] ...................................
 * . (b), (e) [Java ActionEvent], [Java SQL], [Java ActionListener] ...
 * . (f) [Java Swing] .................................................
 * [...]
 * PROBLEM-SOLUTION APPROACH AS FOLLOWS ==========>
 * ********************************************************************
 */

package com.Fingertech.Misael.HamsterDX;

import com.nitgen.SDK.BSP.NBioBSPJNI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.sql.*;

public class Device {

    /*.............................................................
      ..... DEFAULT DECLARATIONS ..................................
      .............................................................
     */
    //number of messages counter
    private static int outCounter = 0;
    //integer which represents the currently opened device as an array index
    private int deviceIdInt;

    //instantiates the user interface class
    private DeviceUI UI = new DeviceUI();

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
        private boolean access() {
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
        private void leave() {
            try {
                connection.close();
                out(Msg.DB.CLOSED);
            } catch (SQLException e) {
                System.out.println("/!\\ CANNOT CLOSE DATABASE: " + e);
            }
        }

        //returns user fingerprint data from the database
        private String select(String id) {
            try {

                //checks if the currently prompted id is equal to the last prompted id
                if (Self.lastPromptedId.equals(id)) {
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
        private int returnID() {
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
        private void save(String fir) {
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

    private Device.Database database = new Device.Database();

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

    //connects to a SELECTED device
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

    /*  . captures a fingerprint
        . refers to the collected fingerprint data to a string
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

    // . captures a new fingerprint and compares it against the previously captured fingerprint
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

    /*  . parameter: fingerprint data from a database
        . captures a new fingerprint and compares it against the parsed parameter
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

        //tries capture method and catches resulted exception
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

    //adds devices to the device list
    private void addToDeviceList(int i) {
        UI.device_list.addItem(deviceEnumInfo.DeviceInfo[i].Name +
                "-" + deviceEnumInfo.DeviceInfo[i].Instance);
    }


    /*  checks firRecord availability in order to enable a button
     */
    private boolean canSaveFirToDB() {
        if (Self.firRecord != null) {
            UI.saveString_button.setEnabled(true);
            return true;
        } else {
            UI.saveString_button.setEnabled(false);
            return false;
        }
    }

    //  assembles buttons upon call
    private void assemble() {
        UI.match_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doMatch();
            }
        });

        UI.capture_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //executes capture method and checks if it was successful
                if (doCapture()) {

                    //enable match button
                    UI.match_button.setEnabled(true);

                    //enables store in database button if the client is connected to a database
                    if (Self.dbConnected) {
                        UI.saveString_button.setEnabled(true);
                    }
                }
            }
        });

        UI.lookUp_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                // asserts if the device look up button is checked to open a device
                if (!Self.behaveOpenButton) {
                    out(Msg.LOOKING_UP_DEVICES);
                    // closes device even if no devices are connected
                    closeDevice();
                    // disables capture and match buttons
                    UI.captureAndMatchEnabled(false);
                    UI.enableDBCompare(false);
                    UI.enableDBButtons(false);

                    // hides showSavedID label
                    UI.showSavedId_label.setVisible(false);

                    // looks up for connected devices and returns as a boolean if any was found
                    if (doLookUpConnected()) {
                        UI.lookUp_button.setText(Msg.Button.OPEN);
                        Self.behaveOpenButton = true;
                    }
                } else { //<= executes code if the look up button is set to open a device
                    
                    // attempts to connect to a selected device and returns boolean as result
                    if (connectTo()) {
                        out(Msg.DEVICE_OPENED);
                        UI.lookUp_button.setText(Msg.Button.FIND);
                        
                        //empties device list in order to prepare it for further detections
                        UI.device_list.removeAllItems();
                        
                        //enables capture and match buttons
                        UI.captureAndMatchEnabled(true);

                        UI.accessDB_button.setEnabled(true);
                        UI.accessDB_button.setText("Conectar ao banco..");

                        UI.match_button.setEnabled(false);

                        /* now connection was established to the attached fingerprint scanner,
                           the find device button is set to operate by its default purpose, which
                           is to find attached devices
                         */
                        Self.behaveOpenButton = false;
                    } else { //<= executes if connection wasn't established
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

                // attempts connection to a user defined database, returns boolean
                if (database.access()) {

                    //enable functionality for database buttons
                    UI.accessDB_button.setText("CONECTADO");
                    UI.accessDB_button.setEnabled(false);
                    UI.enableDBCompare(true);

                    /* checks if any fingerprint was captured and enables store in database
                        button
                     */
                    if (canSaveFirToDB()) {
                        System.out.println("/:-)\\ CAN SAVE TO DB");
                    }

                    //asserts connectivity to a established database
                    Self.dbConnected = true;
                } else {//<= if connectivity fails
                    out(Msg.DB.NO);
                }

            }
        });

        UI.saveString_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // stores fingerprint data in the database
                database.save(Self.firRecord);

                // returns the id of the stored database for further access
                int id = database.returnID();
                String displaySavedID = "ID PARA CADASTRO: " + id;


                UI.showSavedId_label.setVisible(true);
                UI.showSavedId_label.setText(displaySavedID);
                out(displaySavedID);
            }
        });

        UI.compare_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UI.ifInvalidID_label.setVisible(false);
                String id = UI.userDbID_text.getText();
                // checks if id is null. If so, sets its value to 1
                id = (id != null) ? id : "1";

                // queries fingerprint data from the database
                String fingerprint = database.select(id);

                // asserts if the returned data is not an empty value
                if (!fingerprint.equals("")) {
                    compareToDb(fingerprint);
                } else {
                    UI.ifInvalidID_label.setVisible(true);
                }


            }
        });
    }

    // is called upon every attempt to connect to a new device
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
        //attributes a content panel to the jframe
        //UI was already instantiated//
        frame.setContentPane(UI.home_panel);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }


    public Device() {
        // prepares UI
        startUI();

        // greets user
        out(Msg.INIT);
        out(Msg.GREET);

        //assemble program functionality
        assemble();
    }
}
