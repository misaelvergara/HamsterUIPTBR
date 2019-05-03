package com.Fingertech.Misael.HamsterDX;

public class MessageContainer {
    private String region;
    
    public String UI_CAPTURE;
    public String UI_MATCH;
    public String UI_DBCONNECT;
    public String UI_DBSAVE;
    public String UI_COMPARE_TO_REGISTER;
    public String UI_COMPARE;
    public String UI_INVALID_ID;
    public String UI_DBTITLE;
    public String UI_BASIC_TITLE;

    public String INIT;
    public String GREET;
    public String LOOKING_UP_DEVICES;
    public String DEVICES_WERE_FOUND;
    public String NO_DEVICES_WERE_FOUND;
    public String DEVICE_OPENED;
    public String DEVICE_OPENING_FAILED;

    public String CAPTURE_SUCCESS;
    public String CAPTURE_ERROR_TRIGGERED;

    public String VERIFY_INIT;
    public String VERIFY_ERROR_NEAR_CAPTURE;
    public String VERIFY_MATCHING_STARTED;
    public String VERIFY_YES;
    public String VERIFY_NO;

    public String BUTTON_OPEN;
    public String BUTTON_FIND;
    public String BUTTON_RETRY;

    public String DB_YES;
    public String DB_NO;
    public String DB_CLOSED;
    public String DB_SAVED;
    public String DB_CONNECTED;

    public MessageContainer(String lang) {
        if (lang == "EN_US" || lang == "PT_BR") {
            this.region = lang;
        } else {
            this.region = "EN_US";
        }

        assertNewLanguage();
    }

    public void changeLanguage(String lang) {
        if (lang == "EN_US" || lang == "PT_BR") {
            this.region = lang;
        } else {
            this.region = "EN_US";
        }
    }

    public void assertNewLanguage() {
        if (this.region == "EN_US") {
            //UI
            this.UI_CAPTURE = "Capture fingerprint..";
            this.UI_MATCH = "Compare to a new fingerprint..";
            this.UI_DBCONNECT = "Connect to database..";
            this.UI_DBSAVE = "Save captured fingerprint..";
            this.UI_COMPARE_TO_REGISTER = "Compare to database registry";
            this.UI_COMPARE = "Compare..";
            this.UI_INVALID_ID = "INVALID ID";
            this.UI_DBTITLE = "Database";
            this.UI_BASIC_TITLE = "Basic Functions";

            //STANDARD
            this.INIT = "Initializing project..";
            this.GREET = "Connect your HamsterDX or HamsterIII device to your computer's USB port.";
            this.LOOKING_UP_DEVICES = "Looking up for connected devices...";
            this.DEVICES_WERE_FOUND = "Successful detection.";
            this.NO_DEVICES_WERE_FOUND = "No devices were found.";
            this.DEVICE_OPENED = "Selected device opened.";
            this.DEVICE_OPENING_FAILED = "Device opening failed.";

            //CAPTURE
            this.CAPTURE_SUCCESS = "Fingerprint captured successfully.";
            this.CAPTURE_ERROR_TRIGGERED = "An error was triggered by a dependency (SDK). Please contact the " +
                    "accountable developer for this application.";

            //VERIFY
            this.VERIFY_INIT = "Capturing a new fingerprint for comparison.";
            this.VERIFY_ERROR_NEAR_CAPTURE = "An error occurred while trying to capture a new fingerprint.";
            this.VERIFY_MATCHING_STARTED = "Checking for a match...";
            this.VERIFY_YES = "THE COMPARED FINGERPRINTS DO MATCH.";
            this.VERIFY_NO = "THE COMPARED FINGERPRINTS DO NOT MATCH.";

            //BUTTON
            this.BUTTON_OPEN = "Initialize device..";
            this.BUTTON_FIND = "Find devices..";
            this.BUTTON_RETRY = "Retry";

            //DB
            this.DB_YES = "Database connection was established successfully.";
            this.DB_NO = "Not able to establish a connection to the database.";
            this.DB_CLOSED = "Database connection closed.";
            this.DB_SAVED = "The fingerprint data was attributed to the database successfully.";
            this.DB_CONNECTED = "CONNECTED!";

        } else
            //if pt_br
            if (this.region == "PT_BR") {
                //UI
                this.UI_CAPTURE = "Capturar uma digital..";
                this.UI_MATCH = "Comparar com uma nova digital..";
                this.UI_DBCONNECT = "Conectar ao banco..";
                this.UI_DBSAVE = "Salvar impressão capturada..";
                this.UI_COMPARE_TO_REGISTER = "Comparar à um cadastro";
                this.UI_COMPARE = "Comparar..";
                this.UI_INVALID_ID = "ID INSERIDO INVÁLIDO!";
                this.UI_DBTITLE = "Banco de Dados";
                this.UI_BASIC_TITLE = "Funções Básicas";

                //STANDARD
                this.INIT = "Inicializando o projeto..";
                this.GREET = "Conecte seu dispositivo HamsterDX ou HamsterIII à porta USB de seu computador.";
                this.LOOKING_UP_DEVICES = "Procurando dispositivos conectados...";
                this.DEVICES_WERE_FOUND = "Dispositivo(s) detectado(s) com sucesso.";
                this.NO_DEVICES_WERE_FOUND = "Nenhum dispositivo foi detectado.";
                this.DEVICE_OPENED = "Dispositivo inicializado.";
                this.DEVICE_OPENING_FAILED = "Não foi possível inicializar o dispositivo.";

                //CAPTURE
                this.CAPTURE_SUCCESS = "Digital capturada com sucesso.";
                this.CAPTURE_ERROR_TRIGGERED = "Um erro foi detectado pelo SDK da aplicação. Favor contatar o setor técnico.";

                //VERIFY
                this.VERIFY_INIT = "Capturando a segunda digital para realizar a comparação.";
                this.VERIFY_ERROR_NEAR_CAPTURE = "Um erro foi encontrado durante a tentativa de captura para comparação.";
                this.VERIFY_MATCHING_STARTED = "Verificando a coincidência entre as impressões coletadas...";
                this.VERIFY_YES = "SUCESSO. As impressões se coincidem.";
                this.VERIFY_NO = "As impressões comparadas não são idênticas.";

                //BUTTON
                this.BUTTON_OPEN = "Iniciar dispositivo..";
                this.BUTTON_FIND = "Encontrar dispositivos..";
                this.BUTTON_RETRY = "Tentar novamente";

                //DB
                this.DB_YES = "A conexão com o banco de dados foi estabelecida.";
                this.DB_NO = "Não foi possível estabelecer uma conexão com o banco de dados.";
                this.DB_CLOSED = "Sessão com o banco de dados encerrada.";
                this.DB_SAVED = "Os dados biométricos foram salvos no banco com sucesso.";
                this.DB_CONNECTED = "CONECTADO!";
            }
    }


}
