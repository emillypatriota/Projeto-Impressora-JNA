Projeto de Controle de Impressora Térmica (Java & JNA/DLL)
Sobre o Projeto
Este projeto é uma aplicação de console em Java desenvolvida para demonstrar a comunicação e o controle de uma impressora térmica de Ponto de Venda (PDV) usando código nativo.
A comunicação é realizada através da biblioteca JNA (Java Native Access), que atua como uma ponte para chamar funções exportadas por uma Dynamic-Link Library (DLL) específica do fabricante (E1_Impressora01.dll). O sistema permite configurar a conexão (USB, IP, Serial) e executar comandos de impressão, códigos de barras, funções fiscais (SAT) e controle de periféricos.
Funcionamento do Sistema (Arquitetura Java-JNA-DLL)
O funcionamento do sistema se baseia na integração nativa proporcionada pelo JNA.
1. Camada Java (Aplicação Main.java): Gerencia o menu, captura a entrada do usuário (Scanner) e mantém o estado da conexão (conexaoAberta). Sua função é chamar os métodos que foram mapeados para a DLL.
2. Camada JNA (Ponte ImpressoraDLL): A interface ImpressoraDLL extends Library mapeia o nome, a ordem e o tipo de dados dos métodos Java para as funções da DLL. O comando Native.load(...) é o responsável por carregar o arquivo E1_Impressora01.dll na memória da Java Virtual Machine (JVM).
3. Camada DLL (Nativa E1_Impressora01.dll): Esta biblioteca é o código proprietário que entende os comandos da impressora (geralmente códigos ESC/POS). A DLL recebe as chamadas do JNA e traduz o comando em uma instrução que a impressora física reconhece (ex: abrir gaveta, cortar papel).
Instalação e Execução
Pré-requisitos
* Java Development Kit (JDK): Versão 8 ou superior.
* Driver da Impressora: O driver oficial da impressora deve estar instalado no Windows e a impressora deve estar conectada (USB, Serial, ou Rede).
* Arquivos: Os arquivos Main.java, a DLL (E1_Impressora01.dll) e o JAR da JNA (jna.jar) devem estar acessíveis.
Passo a Passo para Executar
1. Compile o Main.java e execute a classe principal.
2. No menu, selecione a opção 1 - Configurar Conexao e insira os parâmetros da sua impressora (Tipo, Modelo, Conexão, Parâmetro).
3. Selecione a opção 2 - Abrir Conexao. Aguarde a mensagem de "Conexão Aberta com Sucesso!".
4. Execute qualquer uma das funções de impressão (3, 4, 5, 6 ou 7).
5. Ao finalizar, selecione a opção 0 - Fechar Conexao e Sair para liberar o hardware.
Funções Implementadas (Chamadas da DLL)
As funções a seguir representam os comandos de hardware mapeados na interface ImpressoraDLL:
* Opção 1: N/A (Método Java) - Configura as variáveis globais (tipo, modelo, conexao, parametro) que serão usadas para iniciar a comunicação.
* Opção 2: AbreConexaoImpressora() - CRÍTICO: Inicia a comunicação na porta/IP/USB especificada.
* Opção 3: ImpressaoTexto() - Envia uma string de texto, permitindo formatação (posicao, estilo, tamanho).
* Opção 4: ImpressaoQRCode() - Imprime um QR Code a partir de uma string de dados, com ajuste de tamanho e nível de correção.
* Opção 5: ImpressaoCodigoBarras() - Imprime um código de barras de um tipo específico (ex: CODE128), com ajuste de altura e largura.
* Opção 6: ImprimeXMLSAT() - Função de alto nível que recebe o caminho de um XML SAT e o formata para impressão do extrato.
* Opção 7: ImprimeXMLCancelamentoSAT() - Imprime o Extrato de Cancelamento, exigindo o caminho do XML de cancelamento e a assinatura do QR Code.
* Opção 8: AbreGavetaElgin() - Envia o comando nativo específico para abrir gavetas de dinheiro da marca Elgin (ou compatíveis).
* Opção 9: AbreGaveta() - Tenta abrir a gaveta usando parâmetros genéricos (pino e tempos).
* Opção 10: SinalSonoro() - Aciona o buzzer da impressora para emitir bips, configurando a quantidade e a duração.
* Opção 0: FechaConexaoImpressora() - CRÍTICO: Encerra a comunicação ativa, liberando a porta para outros programas.
