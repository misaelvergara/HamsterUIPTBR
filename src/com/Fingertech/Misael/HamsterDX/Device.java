/**********************************************************************
 * @author: github @misaelvergara
 * IDENTIDADE DO PROJETO ==========> ..................................
 * . (a) INTERAGIR COM OS LEITORES DE IMPRESSÃO DIGITAL HAMSTER DX ...
 * . E HAMSTER III ....................................................
 * . (b) LER DADOS BIOMÉTRICOS DE UM BANCO DE DADOS ...................
 * . (c) COMPARAR DADOS BIOMÉTRICOS ...................................
 * . (d) CAPTURAR IMPRESSÕES DIGITAIS .................................
 * . (e) ARMAZENAR IMPRESSÕES DIGITAIS EM UM BANCO DE DADOS ...........
 * . (f) INTERAÇÃO CLIENTE-USUÁRIO ATRAVÉS DE UMA INTERFACE GRÁFICA ...
 * DEPENDÊNCIAS DE DRIVERS ==========> ......................................
 * . (a), (c), (d) [NBioBSPJNI.jar] ...................................
 * . (b), (e) [Java ActionEvent], [Java SQL], [Java ActionListener] ...
 * . (f) [Java Swing] .................................................
 * [...]
 * SEGUINDO A APRESENTAÇÃO DA SOLUÇÃO ==========>
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
      ..... DECLARAÇÕES PADRÃO .............;;.....................
      .............................................................
     */

    //DECLARAÇÕES DO SDK
    private NBioBSPJNI bsp = new NBioBSPJNI();
    private NBioBSPJNI.DEVICE_ENUM_INFO deviceEnumInfo;
    private NBioBSPJNI.FIR_HANDLE hSavedFIR;
    private NBioBSPJNI.FIR_TEXTENCODE textSavedFIR;
    private NBioBSPJNI.FIR_TEXTENCODE dbReturnedTXTFIR;
    private NBioBSPJNI.INPUT_FIR inputFIR;
    private NBioBSPJNI.INPUT_FIR inputFIR2;
    private NBioBSPJNI.FIR_HANDLE captureHandle;
    private NBioBSPJNI.IndexSearch.SAMPLE_INFO sampleInfo;

    //VARIÁVEL QUE CONTA MENSAGENS
    private static int outCounter = 0;

    //INTEIRO QUE REPREESENTA O DISPOSITIVO ABERTO COMO UM ÍNDICE DE UMA ARRAY
    private int deviceIdInt;

    /* INSTANCIA A CLASSE DE INTERFACE DO USUÁRIO
        @motivo: acessar métodos de visualização e alterar a visibilidade dos
        componentes da interface de usuário
    */
    private static DeviceUI UI = new DeviceUI();

    /* DECLARA O MÉTODO DE LOG ' out() '
        @motivo: envia mensagens internass para a área de log na interface de
        usuário
     */
    private void out(String msg) {
        UI.log(UI.log_textarea.getText() + "[" + outCounter++ + "] " + msg + "\n");
        System.out.println(msg);
    }

    /* DECLARA UMA VARIÁVEL ESTÁTICA PARA CONEXÃO COM O BANCO DE DADOS
        @motivo: prevenir que uma nova variável seja declarada toda vez que a
        conexão for requirida
     */
    private static Connection connection;

    /* DECLARA UMA INSTÂNCIA ESTÁTICA DE ' MessageContainer '
    */
    public static MessageContainer msg = new MessageContainer("EN_US");



    /*.............................................................
      ..... CLASSES ANINHADAS .....................................
      .............................................................
     */

    // CLASSE DE INFORMAÇÃO DE STATUS DO PROGRAMA
    private static class Self {
        private static boolean deviceConnected = false;
        private static boolean behaveOpenButton = false;
        private static boolean dbConnected = false;
        private static String firRecord;
        private static String remoteFirFromDB;
        private static String lastPromptedId = "0";
    }

    // CLASSE DE ACESSO AO BANCO DE DADOS
    private class Database {
        String db = "hamster";
        String user = "test";
        String pw = "9996";
        String locale = "localhost:3306";

        String url = "jdbc:mysql://" + locale + "/" +
                db + "?user=" + user + "&password=" +
                pw + "&useTimezone=true&serverTimezone=UTC";

        // ACESSA O BANCO DE DADOS E RETORNA UM VALOR LÓGICO REFERENTE (V OU F)
        // AO SUCESSO DA CONEXÃO
        private boolean access() {
            try {
                // VERIFICA SE A CLASSE DO DRIVER ESTÁ PRESENTE
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                System.out.println(e);
                return false;
            }

            try {
                // SE CONECTA A BANCO DE DADOS
                out(msg.DB_CLOSED);
                connection = DriverManager.getConnection(url);
                Self.dbConnected = true;
                return true;

            } catch (SQLException e) {
                System.out.println("/!\\ CANNOT ACCESS DATABASE: " + e);
                return false;
            }
        }

        // DEIXA A CONEXÃO
        private void leave() {
            try {
                connection.close();
                out(msg.DB_CLOSED);
            } catch (SQLException e) {
                System.out.println("/!\\ CANNOT CLOSE DATABASE: " + e);
            }
        }

        // RETORNA OS DADOS BIOMÉTRICOS DE UM USUÁRIO DO BANCO DE DADOS
        private String select(String id) {
            try {

                // VERIFICA SE O ID ATUAL PASSADO COMO PARÂMETRO É IGUAL AO ÚLTIMO ID SOLICITADO
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

        /* RETORNA O NÚMERO DE ROWS DA TABELA SELECIONADA DO BANCO DE DADOS
           . esse número representa o id do último usuário registrado no banco
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

        /* UTILIDADE: ARMAZENA DADOS BIOMÉTRICOS NO BANCO DE DADOS
           PARÂMETRO: DADOS BIOMÉTRICOS
         */
        private void save(String fir) {
            try {
                String query = "INSERT INTO user_data (fir_data) VALUES (?);";
                PreparedStatement newStatement = connection.prepareStatement(query);
                newStatement.setString(1, fir);
                newStatement.execute();
                out(msg.DB_SAVED);

            } catch (SQLSyntaxErrorException e) {
                System.out.println("/!\\ ITEM NOT FOUND: " + e);
            } catch (SQLException e) {
                System.out.println("/!\\ CANNOT LOOK UP DATA STRUCTURE: " + e);
            }
        }
    }

    private Device.Database database = new Device.Database();

    /*.............................................................
      ..... MÉTODOS DA CLASSE ' Device ' ..........................
      .............................................................
     */

    //PROCURA DISPOSITIVOS CONECTADOS AO PC
    private boolean doLookUpConnected() {
        //Nº DE DISPOSITIVOS CONECTADOS
        int connectedDevices;
        Self.deviceConnected = false;

        deviceEnumInfo = bsp.new DEVICE_ENUM_INFO();
        bsp.EnumerateDevice(deviceEnumInfo);
        // INSTANCIA ' DEVICE_ENUM_INFO ' E O PASSE COMO PARÂMETRO P/ EnumerateDevice
        // ISSO POSSIBILITA VERIFICAR O Nº DE DISPOSITIVOS CONECTADOS

        if (bsp.IsErrorOccured()) {
            out("[REASON] SDK Triggered Error");
            return false;
        }

        connectedDevices = deviceEnumInfo.DeviceCount;

        if (connectedDevices == 0) {
            out(msg.NO_DEVICES_WERE_FOUND);
            return false;
        }

        out(msg.DEVICES_WERE_FOUND);

        for (int i = 0; i < connectedDevices; i++) {

            out("DISPOSITIVO: " +
                    deviceEnumInfo.DeviceInfo[i].Name + " - " +
                    deviceEnumInfo.DeviceInfo[i].Instance);

            // ADICIONA OS DISPOSITIVOS À LISTA DA INTERFACE DE USUÁRIO
            addToDeviceList(i);
        }
        return true;

    }

    // CONECTA AO DISPOSITIVO SELECIONADO
    private boolean connectTo() {
        //ENXERGA QUAL DISPOSITIVO FOI SELECIONADO NA LISTA
        deviceIdInt = UI.device_list.getSelectedIndex();

        if (!Self.deviceConnected) {
            /*DeviceInfo é um array que contém o número de disp. conectados. Assim, a sua ordem
             de índice está relacionada à posição que o dispositivo foi enquadrado na lista da
             interface de usuário.
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

    /*  . CAPTURA UMA DIGITAL
        . RELACIONA OS DADOS BIOMÉTRICOS DA DIGITAL À UMA STRING
     */
    private boolean doCapture() {
        /*  APAGA OS DADOS DA ÚLTIMA IMPRESSÃO DIGITAL CAPTURADAS.
         */
        if (hSavedFIR != null) {
            hSavedFIR.dispose();

        }
        hSavedFIR = null;
        Self.firRecord = null;

        /*  INSTANCIA A CLASSE ' HANDLER '.
         */
        hSavedFIR = bsp.new FIR_HANDLE();

        if (bsp.IsErrorOccured()) {
            out(msg.CAPTURE_ERROR_TRIGGERED);
            return false;
        }

        /*  Passa o objeto da classe HANDLER como parâmetro para o método de captura.
            O método retorna ' hSavedFir ' como um condutor dos dados biométricos da impressão capturada.
         */
        try {
            bsp.Capture(0, hSavedFIR, 10000, null, null);
            out(msg.CAPTURE_SUCCESS);
        } catch (Exception e) {
            System.out.println(e);
        }

        /*  Instancia a classe que transforma os dados biom. em texto extenso
         */
        textSavedFIR = bsp.new FIR_TEXTENCODE();

        /*  Salva os dados biométricos capturados e o traduz para formato de texto
         */
        bsp.GetTextFIRFromHandle(hSavedFIR, textSavedFIR);

        // Variável que contém os dados biométricos em forma de texto
        Self.firRecord = textSavedFIR.TextFIR;

        System.out.println("Tamanho da string: " + Self.firRecord.length());
        return true;
    }

    // . captura uma nova impressão e a compara com a última impressão capturada
    private void doMatch() {
        out(msg.VERIFY_INIT);

        // reseta sampleInfo
        if (sampleInfo != null) {
            sampleInfo = null;
        }

        inputFIR = bsp.new INPUT_FIR();
        inputFIR2 = bsp.new INPUT_FIR();
        captureHandle = bsp.new FIR_HANDLE();
        NBioBSPJNI.FIR_PAYLOAD payload = bsp.new FIR_PAYLOAD();

        // é indispensável a declaração de matchSucceeded como ma isntância da classe ' Boolean '
        Boolean matchSucceeded = new Boolean(false);

        // inputFir é uma referência à textSavedFir
        inputFIR.SetTextFIR(textSavedFIR);

        // captura uma nova digital, utiliza captureHandle como condutor
        try {
            bsp.Capture(0, captureHandle, 10000, null, null);
        } catch (Exception e) {
            System.out.println(e);
        }

        if (bsp.IsErrorOccured()) {
            out(msg.VERIFY_ERROR_NEAR_CAPTURE);
            return;
        }
        // inputFir2 é uma referência à digital capturada recentemente
        inputFIR2.SetFIRHandle(captureHandle);
        out(msg.VERIFY_MATCHING_STARTED);

        if (bsp.IsErrorOccured()) {
            out("err");
            return;
        }

        // retorna se as digitais são idênticas através do valor lógico de matchSucceeded
        bsp.VerifyMatch(inputFIR2, inputFIR, matchSucceeded, payload);

        if (matchSucceeded) {
            out(msg.VERIFY_YES);
        } else {
            out(msg.VERIFY_NO);
        }
    }

    /*  . parâmetro: dados biométricos do banco de dados
        . captura uma nova impressão digital e a compara ao parâmetro passado
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

        /*  Atribui os dados biométricos à uma nova instância de
            FIR_TEXTENCODE. O valor atribuído será lido e relacionado à um
            condutor (handle) que irá renderizar os dados biométricos para que
            possam ser comparados.
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
            out(msg.VERIFY_ERROR_NEAR_CAPTURE);
            return;
        }

        inputFIR2.SetFIRHandle(captureHandle);
        out(msg.VERIFY_MATCHING_STARTED);


        if (bsp.IsErrorOccured()) {
            out("err");
        }
        bsp.VerifyMatch(inputFIR2, inputFIR, matchSucceeded, payload);

        if (matchSucceeded) {
            out(msg.VERIFY_YES);
        } else {
            out(msg.VERIFY_NO);
        }
    }

    // adiciona dispositivos à lista de interface de usuário
    private void addToDeviceList(int i) {
        UI.device_list.addItem(deviceEnumInfo.DeviceInfo[i].Name +
                "-" + deviceEnumInfo.DeviceInfo[i].Instance);
    }
    
    // atualiza os campos da interface de usuário
    public void setTextMessage() {
        UI.lookUp_button.setText(msg.BUTTON_FIND);
        UI.basicFunc_title.setText(msg.UI_BASIC_TITLE);

        UI.capture_button.setText(msg.UI_CAPTURE);
        UI.match_button.setText(msg.UI_MATCH);

        UI.db_title.setText(msg.UI_DBTITLE);
        UI.saveString_button.setText(msg.UI_DBSAVE);
        UI.accessDB_button.setText(msg.UI_DBCONNECT);

        UI.compare_label.setText(msg.UI_COMPARE_TO_REGISTER);
        UI.ifInvalidID_label.setText(msg.UI_INVALID_ID);
        UI.compare_button.setText(msg.UI_COMPARE);


    }

    /*  verifica se uma digital foi capturada para que um botão seja ativado    */
    private boolean canSaveFirToDB() {
        if (Self.firRecord != null) {
            UI.saveString_button.setEnabled(true);
            return true;
        } else {
            UI.saveString_button.setEnabled(false);
            return false;
        }
    }

    //  monta os botões
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
                // executa o método de capturada e verifica se este foi executado com sucesso
                if (doCapture()) {

                    // ativa o botão de comparação
                    UI.match_button.setEnabled(true);

                    // se o cliente já estiver conectado ao banco de dados, ativa o botão para salvar
                    // a impressão capturada
                    if (Self.dbConnected) {
                        UI.saveString_button.setEnabled(true);
                    }
                }
            }
        });

        UI.lookUp_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                // verifica se o botão de pesquisa está checado para se comportar como um botão
                // para iniciar dispositivos
                if (!Self.behaveOpenButton) {
                    out(msg.LOOKING_UP_DEVICES);
                    // fecha a conexão com os dispositivos conectados
                    closeDevice();
                    // desativa os botões da interface
                    UI.captureAndMatchEnabled(false);
                    UI.enableDBCompare(false);
                    UI.enableDBButtons(false);

                    // esconde o label que mostra a id da impressão salva no banco
                    UI.showSavedId_label.setVisible(false);

                    // enxerga dispositivos conectados e retorna se algum foi detectado
                    if (doLookUpConnected()) {
                        UI.lookUp_button.setText(msg.BUTTON_OPEN);
                        Self.behaveOpenButton = true;
                    }
                } else { //<= executa o código a seguir se o botão estava checado para abrir um dispositivo

                    // tenta conexão com o dispositivo selecionado e retorna seu sucesso
                    if (connectTo()) {
                        out(msg.DEVICE_OPENED);
                        UI.lookUp_button.setText(msg.BUTTON_FIND);

                        // limpa a lista de disp. conectados
                        UI.device_list.removeAllItems();

                        // habilita os botões de captura e comparação
                        UI.captureAndMatchEnabled(true);

                        UI.accessDB_button.setEnabled(true);
                        UI.accessDB_button.setText("Conectar ao banco..");

                        UI.match_button.setEnabled(false);

                        /* agora que a conexão foi estabelecida com o leitor biométrico conectado ao pc,
                           o botão ' procurar dispositivos ' irá operar normalmente
                         */
                        Self.behaveOpenButton = false;
                    } else { //<= executa se a conexão não foi estabelecida
                        out(msg.DEVICE_OPENING_FAILED);

                        UI.lookUp_button.setText(msg.BUTTON_RETRY);
                        UI.device_list.removeAllItems();

                        Self.behaveOpenButton = false;
                    }
                }


            }
        });

        UI.accessDB_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                // tenta conexão com o banco de dados estabelecido no código
                if (database.access()) {

                    // habilita a funcionalidade dos botões do banco de dados
                    UI.accessDB_button.setText(msg.DB_CONNECTED);
                    UI.accessDB_button.setEnabled(false);
                    UI.enableDBCompare(true);

                    /* verifica se alguma digital foi capturada e assim ativa o botão para
                       salvar dados biométricos no banco
                     */
                    if (canSaveFirToDB()) {
                        System.out.println("/:-)\\ CAN SAVE TO DB");
                    }

                    // evidencia que a conexão foi estabelecida
                    Self.dbConnected = true;
                } else {//<= se a conexão falhar
                    out(msg.DB_NO);
                }

            }
        });

        UI.saveString_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // salva os dados biométricos no banco de dados
                database.save(Self.firRecord);

                // retorna o id de usuário para a impressão salva logo acima
                int id = database.returnID();
                String displaySavedID = "ID : " + id;


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
                // verifica se o id é vazio, se não, determina-o como 1
                id = (id != null) ? id : "1";

                // pede os dados ao banco de dados
                String fingerprint = database.select(id);

                // verifica se o id selecionado não retorna um campo vazio
                if (!fingerprint.equals("")) {
                    compareToDb(fingerprint);
                } else {
                    UI.ifInvalidID_label.setVisible(true);
                }


            }
        });

        UI.lang_list.addActionListener(new ActionListener() {{}
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedLang = UI.lang_list.getItemAt(UI.lang_list.getSelectedIndex()).toString();
                System.out.println(selectedLang);

                msg.changeLanguage(selectedLang);
                msg.assertNewLanguage();
                setTextMessage();
            }
        });
    }

    // é chamado toda vez que ocorrer uma tentativa de conexão à um novo leitor biométrico
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
      ..... CONSTRUTOR DA CLASSE ..................................
      .............................................................
     */

    private void startUI() {
        // cria uma instância da classe Jframe
        JFrame frame = new JFrame("Interface de Testes - Hamster DX/III");
        // atribui um painbel de conteúdos ao objeto de Jframe
        frame.setContentPane(UI.home_panel);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        setTextMessage();
    }


    public Device() {
        // prepara a interface de usuário
        startUI();

        // cumprimenta o usuário
        out(msg.INIT);
        out(msg.GREET);

        // monta os botões da interface de usuário
        assemble();
    }

    public static void main(String args[]) {
        new Device();
    }
}
