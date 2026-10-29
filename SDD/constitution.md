# Constitution - EloSonoro
Definições fundamentais e regras do projeto.

## 1. Propósito e Escopo
Aplicativo de comunicação alternativa (CAA) para usuários não verbais e com dificuldades de comunicação, voltado para ambientes escolares e uso por cuidadores.

## 2. Stack Tecnológico Base
* **Plataforma:** Android Nativo.
* **SDK Mínimo:** API 24 (Android 7.0).
* **Interface:** Material Design (com suporte nativo a Temas Claro e Escuro acessíveis).
* **Banco de Dados:** Room Database.
* **Text-to-Speech:** Sintetizador de voz nativo do Android (TTS).

## 3. Restrições Arquiteturais e Persistência
* **Isolamento de Mídia:** Arquivos de imagem e áudio devem ser gravados fisicamente no diretório do sistema do aplicativo.
* **Regra de Banco de Dados:** O Room deve armazenar estritamente as strings com os caminhos (URIs) dos arquivos. O armazenamento de BLOBs de mídia no banco é terminantemente proibido.
* **Offline-First:** O núcleo do aplicativo deve operar 100% offline. Conexões de rede são permitidas apenas para funções pontuais, como o consumo da API ARASAAC.
* **Portabilidade de Dados:** A estrutura e a persistência devem suportar o empacotamento completo (dados do Room + arquivos físicos) para viabilizar exportação/importação pedagógica e rotinas de backup estruturado.

## 4. Requisitos Críticos de Performance
* **Latência de Áudio:** O tempo de resposta entre o toque na placa (input) e o início da reprodução sonora (output) é estritamente limitado a um máximo de 200ms.

## 5. Diretrizes Fundamentais de Acessibilidade e Redução de Sobrecarga Cognitiva
* **Redução de Sobrecarga Cognitiva no Modo de Uso:** A tela principal da prancha de comunicação, durante o Modo de Uso, deve priorizar os elementos essenciais para a comunicação do usuário não verbal (placas) e o acesso controlado via menu centralizado. Ferramentas de gestão e edição permanecem restritas ao Modo Edição. Por padrão, a barra de pesquisa e a listagem de categorias na prancha principal são ocultadas no Modo de Uso para evitar ruído visual e dispersão de atenção, podendo sua exibição na tela inicial ser habilitada opcionalmente pelo cuidador através de controles dedicados no Menu Centralizado.
* **Acessibilidade Motora e Zonas Seguras de Toque (Touch Targets & Margens):** Todos os botões e elementos interativos da interface devem possuir dimensões adequadas e margens de segurança ampliadas (paddings internos e margins externas generosos) no topo e na base da tela. Os controles não devem ficar posicionados rente às bordas físicas do dispositivo nem sobrepor as áreas de interferência da barra de status e da barra de navegação/gestos do sistema operacional Android.
* **Densidade Visual Adaptativa:** A prancha principal deve permitir a comutação estrita entre 2 colunas (acessibilidade motora ampla e maior destaque visual), 4 colunas (densidade padrão) e 6 colunas (densidade máxima).
* **Proteção e Sinalização de Modos de Operação:** O estado de operação deve ser estritamente sinalizado pela metáfora visual de cadeado (cadeado fechado para Modo de Uso e cadeado aberto para Modo Edição), garantindo que a comunicação não seja interrompida por edições acidentais.

## 6. Telemetria
* **Coleta Silenciosa:** O sistema deve registrar em segundo plano o histórico de placas acionadas, horários e frequência. Estes dados formam a base para métricas de evolução.