package com.Fingertech.Misael.HamsterDX;

public final class Msg {
    static final String INIT = "Inicializando o projeto..";
    static final String GREET = "Conecte seu dispositivo HamsterDX ou HamsterIII à porta USB do seu computador.";
    static final String LOOKING_UP_DEVICES = "Procurando dispositivos conectados...";
    static final String DEVICES_WERE_FOUND = "Dispositivo(s) detectado(s) com sucesso.";
    static final String NO_DEVICES_WERE_FOUND = "Nenhum dispositivo foi detectado.";
    static final String DEVICE_OPENED = "Dispositivo inicializado.";
    static final String DEVICE_OPENING_FAILED = "Não foi possível inicializar o dispositivo.";

    static final String LOOP_CONCLUDED_OPTIONS = "INSTRUÇÕES:" +
            "\nDigite \"C\" para detectar outro dedo usando este mesmo dispositivo." +
            "\nDigite \"R\" para iniciar o programa novamente." +
            "\nDigite \"X\" para finalizar o programa.";
    static final String LOOP_CONCLUDED = "O programa foi concluído. Siga as instruções a seguir para demandar novas ações.\n";

    static final class Capture {
        static final String SUCCESS = "Digital capturada com sucesso.";
        static final String ERROR_TRIGGERED = "Um erro foi detectado pelo SDK da aplicação. Favor contatar o setor técnico.";
    }
    static final class Verify {
        static final String INIT = "Capturando a segunda digital para realizar a comparação.";
        static final String ERROR_NEAR_CAPTURE = "Um erro foi encontrado durante a tentativa de captura para comparação.";
        static final String MATCHING_STARTED = "Verificando a coincidência entre as impressões coletadas...";
        static final String YES = "SUCESSO. As impressões se coincidem.";
        static final String NO = "As impressões comparadas não são idênticas.";
    }
    static final class Button {
        static final String OPEN = "Iniciar dispositivo..";
        static final String FIND = "Encontrar dispositivos..";
        static final String RETRY = "Tentar novamente";
    }
    static final class DB {
        static final String YES = "A conexão com o banco de dados foi estabelecida.";
        static final String NO = "Não foi possível estabelecer uma conexão com o banco de dados.";
        static final String CLOSED = "Sessão com o banco de dados encerrada.";
        static final String SAVED = "Os dados biométricos foram salvos no banco com sucesso.";
    }
}
