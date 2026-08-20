# Implementation Plan
Plano de implementação técnica.

## 1. Arquitetura de Software
* **Padrão:** MVVM (Model-View-ViewModel) com Clean Architecture.
* **Linguagem:** Kotlin.
* **Assincronismo e Reatividade:** Kotlin Coroutines e Flows para operações de I/O e observação do estado do banco de dados (Room).
* **Injeção de Dependência:** Hilt/Dagger (ou Koin) para injetar repositórios, gerenciadores de áudio e banco de dados.

## 2. Modelagem de Dados (Room Database)
Para atender à especificação de relação N:M (Placas e Perfis) e telemetria:

### 2.1 Entidades
* **`PlateEntity`**: `id` (PK, UUID), `name` (String, Not Null), `imageUri` (String, Nullable), `audioUri` (String, Nullable).
* **`ProfileEntity`**: `id` (PK, UUID), `name` (String, Not Null, Unique).
* **`PlateProfileCrossRef`**: `plateId` (FK), `profileId` (FK). Chave primária composta `(plateId, profileId)`.
* **`TelemetryEntity`**: `id` (PK), `plateId` (FK), `timestamp` (Long, Epoch).

### 2.2 DAOs (Data Access Objects)
* **`PlateDao`**: Insert, Update, Delete. Busca com filtro `LIKE` no campo `name` para pesquisa inteligente.
* **`ProfileDao`**: Insert, Update, Delete. Tratamento de exceção de violação de Unique Constraint no campo `name`.
* **`CrossRefDao`**: Operações para vincular e desvincular Placas e Perfis. Função para buscar `ProfileWithPlates` (Categorias + suas Placas).

## 3. Gerenciamento de Sistema de Arquivos (I/O)
* **Armazenamento:** Diretório interno do aplicativo (`Context.filesDir`).
* **Regra de Nomenclatura:** Arquivos baixados ou importados serão renomeados com UUIDs (ex: `img_uuid.png`, `audio_uuid.mp3`) para evitar colisão.
* **Fluxo de Cadastro:** O ViewModel recebe as URIs temporárias (Galeria/Câmera/API). O Repositório move o arquivo fisicamente para `Context.filesDir`, gera a URI permanente local e só então o DAO insere o registro no Room.

## 4. Componentes de Hardware e Mídia
* **Motor de Áudio (Baixa Latência):** Uso da classe `SoundPool` ao invés de `MediaPlayer`. O `SoundPool` carrega áudios curtos na memória, garantindo latência inferior a 200ms estipulada na especificação.
* **Motor de TTS:** Inicialização assíncrona do `android.speech.tts.TextToSpeech`. Utilizado como fallback no bloco `try-catch` do reprodutor de áudio local.

## 5. Integração com API (ARASAAC)
* **Cliente HTTP:** Retrofit + OkHttp.
* **Fluxo:** Requisição de busca (GET), conversão do JSON para modelo de domínio, exibição em grid. Ao selecionar, download assíncrono do InputStream da imagem, salvamento no `Context.filesDir` e persistência no Room.

## 6. Sincronização e Backup
* **Empacotamento (Exportação):** Classe utilitária utilizando `java.util.zip.ZipOutputStream`. Coleta o banco de dados Room (arquivos `.db`, `-wal`, `-shm`) e os arquivos de mídia no `Context.filesDir`, compactando-os em um `.zip` exportável via `Intent.ACTION_SEND` ou `Storage Access Framework`.
* **Desempacotamento (Importação):** Extração do `.zip`, substituição dos arquivos físicos e reabertura do banco Room.

## 7. Controle de UI e Estados
* **StateFlow/LiveData:** A UI assinará um `ModeState` (Booleano: `isEditMode`). Componentes de deleção/edição observarão este estado para alterar propriedade `visibility` e desabilitar `onClickListener`.
* **Acessibilidade Motora (Grid):** O `RecyclerView` da prancha utilizará `GridLayoutManager`. O `spanCount` será dinâmico (2 ou 4) gerenciado por uma configuração salva em `SharedPreferences` ou `DataStore`.