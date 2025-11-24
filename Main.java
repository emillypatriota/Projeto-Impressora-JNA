import com.sun.jna.Library; // Permite ao Java mapear e carregar funções de bibliotecas nativas (DLLs no Windows).
import com.sun.jna.Native; // Essencial para carregar a DLL na memória da JVM (Java Virtual Machine).
import com.sun.security.jgss.InquireSecContextPermission; // Importação não utilizada/necessária, mas mantida no código original.

import java.util.Scanner; // Para capturar entrada do usuário (menu e parâmetros).
import javax.swing.JFileChooser; // Para abrir janela de seleção de arquivos (não utilizado ativamente no menu principal).
import java.io.File; // Para manipulação de arquivos (útil para caminhos de XML, por exemplo).
import java.io.IOException; // Para tratar erros de I/O (Input/Output).
import java.nio.charset.StandardCharsets; // Para leitura de arquivos UTF-8.
import java.io.FileInputStream; // Para ler arquivos byte a byte.

/**
 * Classe principal do sistema de controle de impressora.
 * Utiliza JNA para interagir com a DLL de comunicação com o hardware.
 */
public class Main {

    // Interface que representa a DLL (E1_Impressora01.dll), usando JNA.
    // O JNA usa esta interface como um 'contrato' para saber quais funções nativas
    // podem ser chamadas pelo código Java.
    public interface ImpressoraDLL extends Library {

        // Carrega a DLL na memória da JVM. Este é o ponto mais importante de comunicação nativa.
        // ATENÇÃO: O caminho deve ser ajustado se o arquivo for movido!
        ImpressoraDLL INSTANCE = (ImpressoraDLL) Native.load(
                "C:\\Users\\juan_cintra\\Downloads\\Java-Aluno Graduacao\\Java-Aluno Graduacao\\E1_Impressora01.dll",
                ImpressoraDLL.class
        );

        // Função auxiliar (privada e estática) para ler o conteúdo de um arquivo em formato String.
        // Usada principalmente para carregar o conteúdo de XMLs SAT.
        private static String lerArquivoComoString(String path) throws IOException {
            FileInputStream fis = new FileInputStream(path); // Abre o arquivo
            byte[] data = fis.readAllBytes(); // Lê todos os bytes do arquivo
            fis.close(); // Fecha o arquivo
            return new String(data, StandardCharsets.UTF_8); // Converte bytes em String UTF-8
        }

        // =========================================================================
        // DECLARAÇÃO DAS FUNÇÕES NATIVAS (MAPEAMENTO DA DLL)
        // O JNA associa esses métodos às funções exportadas pela DLL.
        // =========================================================================

        // Função principal para iniciar a comunicação com a impressora.
        // Recebe o tipo de conexão (USB, TCP/IP), modelo, valor da conexão (porta, IP) e parâmetro.
        int AbreConexaoImpressora(int tipo, String modelo, String conexao, int param);

        // Encerra a comunicação e libera a porta.
        int FechaConexaoImpressora();

        // Envia dados de texto para impressão, com formatação (posição, estilo, tamanho).
        int ImpressaoTexto(String dados, int posicao, int estilo, int tamanho);

        // Realiza o corte do papel (parcial ou total), com avanço opcional.
        int Corte(int avanco);

        // Imprime um QR Code com base nos dados fornecidos.
        int ImpressaoQRCode(String dados, int tamanho, int nivelCorrecao);

        // Imprime um código de barras de um tipo específico (ex: EAN, CODE128).
        int ImpressaoCodigoBarras(int tipo, String dados, int altura, int largura, int HRI);

        // Avança o papel em um número de linhas especificado.
        int AvancaPapel(int linhas);

        // Consulta o status atual da impressora (ex: papel acabando, tampa aberta).
        int StatusImpressora(int param);

        // Abre a gaveta de dinheiro, específica para o modelo Elgin.
        int AbreGavetaElgin();

        // Abre a gaveta de dinheiro em portas e tempos genéricos.
        int AbreGaveta(int pino, int ti, int tf);

        // Emite um sinal sonoro (bip) na impressora.
        int SinalSonoro(int qtd, int tempoInicio, int tempoFim);

        // Funções para Modo Página (permitem posicionar elementos de forma precisa na página).
        int ModoPagina();
        int LimpaBufferModoPagina();
        int ImprimeModoPagina();
        int ModoPadrao();

        // Funções de posicionamento no Modo Página.
        int PosicaoImpressaoHorizontal(int posicao);
        int PosicaoImpressaoVertical(int posicao);

        // Imprime o Extrato Eletrônico do SAT (Sistema Autenticador e Transmissor).
        int ImprimeXMLSAT(String dados, int param);

        // Imprime o Extrato de Cancelamento do SAT.
        int ImprimeXMLCancelamentoSAT(String dados, String assQRCode, int param);
    }

    // =========================================================================
    // VARIÁVEIS DE ESTADO GLOBAIS
    // =========================================================================
    private static boolean conexaoAberta = false; // Flag para rastrear o estado da conexão.
    private static int tipo; // Armazena o tipo de conexão (1=USB, 3=TCP/IP, etc.).
    private static String modelo; // Armazena o modelo da impressora.
    private static String conexao; // Armazena o valor da conexão (ex: IP, 'USB', nome da porta).
    private static int parametro; // Armazena parâmetro extra (ex: porta TCP/IP ou baudrate).

    private static final Scanner scanner = new Scanner(System.in); // Scanner global para leitura de input.

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    // Função para capturar entrada do usuário (input de linha).
    private static String capturarEntrada(String mensagem) {
        System.out.print(mensagem); // Mostra mensagem para o usuário
        return scanner.nextLine(); // Captura a entrada
    }

    // Permite ao usuário definir os parâmetros de conexão.
    public static void configurarConexao() {
        Scanner scanner = new Scanner(System.in); 

        if (!conexaoAberta) { // Só permite configurar se não houver conexão ativa.

            System.out.println("Digite o tipo de conexão (1 - USB, 2 - RS232, 3 - TCP/IP, 4 - Bluetooth, 5 - Impressoras acopladas\n(Android))");
            tipo = scanner.nextInt();
            scanner.nextLine(); // Limpa buffer

            if (tipo == 5) { // Se for Android, outros parâmetros não são necessários
                modelo = "";
                conexao = "";
                parametro = 0;
                System.out.println("Tipo 5 selecionado. Os outros parâmetros foram deixados vazios.");
            } else {
                System.out.println("Digite o modelo da impressora (ex: i9, MP-4200):");
                modelo = scanner.nextLine();

                System.out.println("Digite o valor da conexão (ex: USB, COM2, 192.168.0.20, AA:BB:CC:DD:EE:FF):");
                conexao = scanner.nextLine();

                System.out.println("Digite o parâmetro (ex: 0, 9100, 9600):");
                parametro = scanner.nextInt();
                scanner.nextLine(); // Limpa buffer
            }

            // Resumo da configuração
            System.out.println("\nConfiguração salva com sucesso!");
            System.out.println("Tipo: " + tipo);
            System.out.println("Modelo: " + modelo);
            System.out.println("Conexão: " + conexao);
            System.out.println("Parâmetro: " + parametro);

        } else {
            System.out.println("Já existe uma conexão aberta! Feche antes de configurar uma nova.");
        }
    }

    // Chama a função nativa para abrir a comunicação com a impressora.
    public static void abrirConexao() {
        if (!conexaoAberta) { // Verifica se não há conexão aberta

            // Chama a função da DLL, passando os parâmetros globais configurados.
            int retorno = ImpressoraDLL.INSTANCE.AbreConexaoImpressora(tipo, modelo, conexao, parametro);
            if (retorno == 0) { // O código 0 geralmente indica sucesso em APIs nativas.
                conexaoAberta = true;
                System.out.println("Conexão Aberta com Sucesso!");
            } else { // Se o retorno for diferente de 0, ocorreu um erro.
                System.out.println("Erro ao abrir conexão. Código de erro: " + retorno);
            }
        } else {
            System.out.println("Já existe uma conexão aberta!");
        }
    }

    // Chama a função nativa para fechar a comunicação com a impressora.
    public static void fecharConexao() {
        if (conexaoAberta) { // Só tenta fechar se houver conexão aberta.
            int retorno = ImpressoraDLL.INSTANCE.FechaConexaoImpressora();
            if (retorno == 0) {
                conexaoAberta = false;
                System.out.println("Conexão fechada com sucesso!");
            } else {
                System.out.println("Erro ao fechar conexão. Código de erro: " + retorno);
            }
        } else {
            System.out.println("Nenhuma conexão aberta para fechar!");
        }
    }

    // Captura o texto e os parâmetros de formatação e envia para a impressão.
    public static void impressaoTexto() {
        if (!conexaoAberta) { // Pré-requisito: conexão deve estar aberta.
            System.out.println("Abra a conexão primeiro!");
            return;
        }

        // Captura de parâmetros de impressão (texto, posição, estilo, tamanho).
        System.out.print("Digite o texto a ser impresso: ");
        String texto = scanner.nextLine(); 

        System.out.print("Digite a posição (0 = esquerda, 1 = centralizado, 2 = direita): ");
        int posicao = Integer.parseInt(scanner.nextLine()); 

        System.out.print("Digite o estilo (0 = normal, 1 = negrito, 2 = itálico): ");
        int estilo = Integer.parseInt(scanner.nextLine()); 

        System.out.print("Digite o tamanho (0 = normal, 1 = grande): ");
        int tamanho = Integer.parseInt(scanner.nextLine()); 

        System.out.print("Digite quantas linhas deseja avançar o papel: ");
        int linhas = Integer.parseInt(scanner.nextLine()); 

        System.out.print("Digite o tipo de corte (1 a 3): ");
        int corte = Integer.parseInt(scanner.nextLine()); 

        int retorno = ImpressoraDLL.INSTANCE.ImpressaoTexto(texto, posicao, estilo, tamanho);
        
        // Comandos de finalização:
        ImpressoraDLL.INSTANCE.AvancaPapel(linhas); // Garante que o texto seja visível
        ImpressoraDLL.INSTANCE.Corte(corte); // Realiza o corte físico do papel.
    }

    // Envia dados para impressão de QR Code.
    public static void impressaoQRCode() {
        if (!conexaoAberta) {
            System.out.println("Abra a conexão primeiro!");
            return;
        }
        // Valores fixos de teste: "teste de impressao", tamanho 6, nível de correção 4.
        int retorno = ImpressoraDLL.INSTANCE.ImpressaoQRCode("teste de impressao",6,4);
        
        if (retorno == 0) {
            System.out.println("Impressao bem sucedida");
        } else {
            System.out.println("Erro ao Imprimir" + retorno);
        }
        ImpressoraDLL.INSTANCE.Corte(3); 
    }

    // Envia dados para impressão de Código de Barras (ex: CODE128).
    public static void impressaoCodigoBarras() {
        if (!conexaoAberta) {
            System.out.println("Abra a conexão primeiro!");
            return;
        }
        if (conexaoAberta) {
            // Tipo 8 = CODE 128 (exemplo), altura 100, largura 2, HRI (posição do texto) 3.
            int retorno = ImpressoraDLL.INSTANCE.ImpressaoCodigoBarras(8, "{A012345678912", 100, 2, 3);
            if (retorno == 0) {
                System.out.println("Impressão bem sucedida");
            } else {
                System.out.println("Erro" + retorno);
            }
        } else {
            System.out.println("Precisa abrir conexao primeiro");
        }

        ImpressoraDLL.INSTANCE.Corte(3); 
    }

    // Imprime o XML do Extrato SAT. A função da DLL recebe o caminho do XML.
    public static void impressaoXMLSAT() {
        if (!conexaoAberta) {
            System.out.println("Abra a conexão primeiro!");
            return;
        }
        // Define o caminho do arquivo XML a ser impresso.
        String dados = "path=C:\\Users\\juan_cintra\\Downloads\\Java-Aluno Graduacao\\Java-Aluno Graduacao\\XMLSAT.xml";
        int retorno = ImpressoraDLL.INSTANCE.ImprimeXMLSAT(dados, 0);
        if (retorno == 0) {
            System.out.println("Impressao realizada!");
        } else {
            System.out.println("Erro imprimir xml sat. Retorno " + retorno);
        }
    }

    // Imprime o XML de Cancelamento SAT. A função da DLL exige o caminho do XML e a assinatura do QR Code.
    public static void impressaoXMLCancelamentoSAT() {
        if (!conexaoAberta) {
            System.out.println("Abra a conexão primeiro!");
            return;
        }
        // Define o caminho do arquivo XML de cancelamento.
        String dados = "path=" + "C:\\Users\\juan_cintra\\Downloads\\Java-Aluno Graduacao\\Java-Aluno Graduacao\\CANC_SAT.xml";
        // Assinatura QRCode de teste (longa).
        String assQRCode = "Q5DLkpdRijIRGY6YSSNsTWK1TztHL1vD0V1Jc4spo/CEUqICEb9SFy82ym8EhBRZjbh3btsZhF+sjHqEMR159i4agru9x6KsepK/q0E2e5xlU5cv3m1woYfgHyOkWDNcSdMsS6bBh2Bpq6s89yJ9Q6qh/J8YHi306ce9Tqb/drKvN2XdE5noRSS32TAWuaQEVd7u+TrvXlOQsE3fHR1D5f1saUwQLPSdIv01NF6Ny7jZwjCwv1uNDgGZONJdlTJ6p0ccqnZvuE70aHOI09elpjEO6Cd+orI7XHHrFCwhFhAcbalc+ZfO5b/+vkyAHS6CYVFCDtYR9Hi5qgdk31v23w==";
        
        int retorno = ImpressoraDLL.INSTANCE.ImprimeXMLCancelamentoSAT(dados, assQRCode, 0);
        if (retorno == 0) {
            System.out.println("Impressao realizada!");
        } else {
            System.out.println("Erro " + retorno);
        }
    }

    // Tenta abrir a gaveta de dinheiro usando o comando específico da Elgin.
    public static void abrirGavetaElgin() {
        if (!conexaoAberta) {
            System.out.println("Abra a conexão primeiro!");
            return;
        }
        
        int retorno = ImpressoraDLL.INSTANCE.AbreGavetaElgin();
        if (retorno == 0) {
            System.out.println("Gaveta aberta com sucesso");
        } else {
            System.out.println("Erro ao abrir a gaveta" + retorno);
        }
    }

    // Tenta abrir a gaveta de dinheiro usando parâmetros genéricos (pino e tempo).
    public static void abrirGaveta() {
        if (!conexaoAberta) {
            System.out.println("Abra a conexão primeiro!");
            return;
        }
        // Padrão: Pino 1, tempo inicial 5ms, tempo final 10ms.
        int retorno = ImpressoraDLL.INSTANCE.AbreGaveta(1, 5, 10);
        if (retorno == 0) {
            System.out.println("Gaveta aberta com sucesso");
        } else {
            System.out.println("Erro ao abrir gaveta" + retorno);

        }
    }


    // Emite um sinal sonoro (bip) na impressora, configurável pelo usuário.
    public static void sinalSonoro() {
        if (!conexaoAberta) {
            System.out.println("Abra a conexão primeiro!");
            return;
        }
        // Captura a quantidade de bips e os tempos de duração.
        System.out.print("Digite a quantidade de bips: ");
        int qtd = Integer.parseInt(scanner.nextLine());
        System.out.print("Digite o tempo de início (ms): ");
        int ti = Integer.parseInt(scanner.nextLine());
        System.out.print("Digite o tempo de fim (ms): ");
        int tf = Integer.parseInt(scanner.nextLine());
        
        int retorno = ImpressoraDLL.INSTANCE.SinalSonoro(qtd, ti, tf);
        if (retorno == 0) System.out.println("Sinal sonoro emitido!");
        else System.out.println("Erro ao emitir sinal sonoro: " + retorno);
    }


    // =========================================================================
    // MÉTODO PRINCIPAL
    // =========================================================================
    public static void main(String[] args) {
        // Loop principal que mantém o menu ativo até que o usuário escolha sair (opção 0).
        while (true) {
            System.out.println("\n*************************************************");
            System.out.println("**************** MENU IMPRESSORA ****************");
            System.out.println("*************************************************\n");

            System.out.println("1  - Configurar Conexao");
            System.out.println("2  - Abrir Conexao");
            System.out.println("3  - Impressao Texto");
            System.out.println("4  - Impressao QRCode");
            System.out.println("5  - Impressao Cod Barras");
            System.out.println("6  - Impressao XML SAT");
            System.out.println("7  - Impressao XML Canc SAT");
            System.out.println("8  - Abrir Gaveta Elgin");
            System.out.println("9  - Abrir Gaveta");
            System.out.println("10 - Sinal Sonoro");
            System.out.println("0  - Fechar Conexao e Sair");
            System.out.println("--------------------------------------");

            String escolha = capturarEntrada("\nDigite a opção desejada: ");

            if (escolha.equals("0")) { // Opção de saída.
                fecharConexao(); // Garante que a conexão seja fechada antes de sair.
                System.out.println("Saindo do sistema...");
                break;
            }


            // Estrutura switch para direcionar o fluxo do programa baseado na escolha do usuário.
            switch (escolha) {
                case "1": configurarConexao(); break;
                case "2": abrirConexao(); break;
                case "3": impressaoTexto();
                    ImpressoraDLL.INSTANCE.Corte(3); break; // Finaliza o comando com corte.
                case "4": ImpressoraDLL.INSTANCE.AvancaPapel(3); // Avança antes de imprimir o QR code.
                    impressaoQRCode();
                    ImpressoraDLL.INSTANCE.Corte(3);
                    break;
                case "5": impressaoCodigoBarras();
                    ImpressoraDLL.INSTANCE.AvancaPapel(3);
                    ImpressoraDLL.INSTANCE.Corte(3);
                    break;
                case "6": impressaoXMLSAT();
                    ImpressoraDLL.INSTANCE.AvancaPapel(3);
                    ImpressoraDLL.INSTANCE.Corte(3);
                    break;
                case "7": impressaoXMLCancelamentoSAT();
                    ImpressoraDLL.INSTANCE.AvancaPapel(3);
                    ImpressoraDLL.INSTANCE.Corte(3);
                    break;
                case "8": abrirGavetaElgin(); break;
                case "9": abrirGaveta(); break;
                case "10": sinalSonoro(); break;
                default:
                    System.out.println("Número inválido. Digite um número de 0 a 10.");
                            break;
            }
        }
        scanner.close(); // Fecha o objeto Scanner.
    }
}