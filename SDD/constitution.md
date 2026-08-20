# Constitution - EloSonoro
Definições fundamentais e regras do projeto.

## 1. Propósito e Escopo
Aplicativo de comunicação alternativa (CAA) para usuários não verbais e com dificuldades de comunicação, voltado para ambientes escolares e uso por cuidadores.

## 2. Stack Tecnológico Base
* **Plataforma:** Android Nativo.
* **SDK Mínimo:** API 24 (Android 7.0).
* **Interface:** Material Design.
* **Banco de Dados:** Room Database.
* **Text-to-Speech:** Sintetizador de voz nativo do Android (TTS).

## 3. Restrições Arquiteturais e Persistência
* **Isolamento de Mídia:** Arquivos de imagem e áudio devem ser gravados fisicamente no diretório do sistema do aplicativo.
* **Regra de Banco de Dados:** O Room deve armazenar estritamente as strings com os caminhos (URIs) dos arquivos. O armazenamento de BLOBs de mídia no banco é terminantemente proibido.
* **Offline-First:** O núcleo do aplicativo deve operar 100% offline. Conexões de rede são permitidas apenas para funções pontuais, como o consumo da API ARASAAC.
* **Portabilidade de Dados:** A estrutura e a persistência devem suportar o empacotamento completo (dados do Room + arquivos físicos) para viabilizar exportação/importação pedagógica e rotinas de backup estruturado.

## 4. Requisitos Críticos de Performance
* **Latência de Áudio:** O tempo de resposta entre o toque na placa (input) e o início da reprodução sonora (output) é estritamente limitado a um máximo de 200ms.

## 5. Telemetria
* **Coleta Silenciosa:** O sistema deve registrar em segundo plano o histórico de placas acionadas, horários e frequência. Estes dados formam a base para métricas de evolução.