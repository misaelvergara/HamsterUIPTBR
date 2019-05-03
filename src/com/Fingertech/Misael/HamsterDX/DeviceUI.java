package com.Fingertech.Misael.HamsterDX;
import javax.swing.*;

public class DeviceUI {
    public JPanel home_panel;
    public JButton lookUp_button;
    public JComboBox device_list;
    public JPanel method_panel;
    public JButton capture_button;
    public JButton match_button;
    public JScrollPane scrollable_textinput;
    public JTextArea log_textarea;
    public JPanel advanced_panel;
    public JButton saveString_button;
    public JButton accessDB_button;
    public JPanel separator;
    public JPanel dbInsert_panel;
    public JButton compare_button;
    public JTextField userDbID_text;
    public JLabel compare_label;
    public JLabel showSavedId_label;
    public JLabel ifInvalidID_label;
    public JLabel db_title;
    public JLabel basicFunc_title;
    public JComboBox lang_list;

    public void log(String input) {
        log_textarea.setText(input);
    }


    public void captureAndMatchEnabled (boolean value) {
        capture_button.setEnabled(value);
        match_button.setEnabled(value);
    }

    public void enableDBButtons (boolean value) {
        accessDB_button.setEnabled(value);
        compare_button.setEnabled(value);
        saveString_button.setEnabled(value);
    }

    public void enableDBCompare (boolean value) {
        compare_button.setEnabled(value);
        userDbID_text.setEnabled(value);
    }

    public DeviceUI() {
        lang_list.addItem("EN_US");
        lang_list.addItem("PT_BR");

        enableDBButtons(false);
        enableDBCompare(false);
        showSavedId_label.setVisible(false);
        ifInvalidID_label.setVisible(false);
        captureAndMatchEnabled(false);

    }
}
