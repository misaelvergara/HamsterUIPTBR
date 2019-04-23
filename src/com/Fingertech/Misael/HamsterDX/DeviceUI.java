package com.Fingertech.Misael.HamsterDX;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class DeviceUI {
    public JPanel home_panel;
    public JButton lookUp_button;
    public JComboBox device_list;
    public JPanel method_panel;
    public JButton capture_button;
    public JButton match_button;
    public JScrollPane scrollable_textinput;
    public JTextArea log_textarea;
    private JPanel advanced_panel;
    public JButton saveString_button;
    public JButton accessDB_button;
    private JPanel separator;
    private JPanel dbInsert_panel;
    public JButton compare_button;
    public JTextField userDbID_text;
    private JLabel compare_label;

    public void log(String input) {
        log_textarea.setText(input);
    }


    public void buttonsEnabled (boolean value) {
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

        log_textarea.setLineWrap(true);
        log_textarea.setWrapStyleWord(true);
        buttonsEnabled(false);
        enableDBButtons(false);
        enableDBCompare(false);

    }
}
