# Implementation Plan
Plano de implementação técnica.

## 1. Arquitetura de Software
* **Padrão:** MVVM (Model-View-ViewModel) com princípios de Clean Architecture.
* **Linguagem:** Kotlin.
* **Assincronismo e Reatividade:** Kotlin Coroutines e Flows para operações de I/O em background, reatividade de dados do banco (Room) e emissão de estados de UI.
* **Injeção de Dependência:** Hilt/Dagger para provisão de instâncias de Repositórios, DAOs, Database, Gerenciadores de Áudio/Mídia e Casos de Uso.
* **Camadas da Aplicação:**
  * **Data:** Room Database, DAOs, Entidades, Gerenciador de Arquivos (`LocalFileManager`), Cliente Retrofit da API ARASAAC, `PreferencesManager` e Implementações de Repositórios.
  * **Domain:** Modelos de Domínio, Interfaces de Repositórios e Casos de Uso (Use Cases) encapsulando as regras de negócio.
  * **UI:** Activities, Adapters (`RecyclerView`), ViewModels, Diálogos modais e UI States gerenciados via `StateFlow` com suporte ao Material Design 3 e Temas Claro/Escuro.

## 2. Modelagem de Dados e Persistência (Room Database)
A modelagem atende à relação N:M entre Placas e Perfis/Categorias, telemetria em segundo plano e à restrição da Constituição de isolamento de mídias (apenas Strings com URIs locais, zero BLOBs).

### 2.1 Entidades
* **`PlateEntity` (`plates`)**:
  * `id`: `String` (PK, UUID gerado na criação).
  * `name`: `String` (Not Null).
  * `imageUri`: `String?` (Caminho/URI do arquivo local, Nullable).
  * `audioUri`: `String?` (Caminho/URI do arquivo local de áudio gravado, Nullable).
* **`ProfileEntity` (`profiles`)**:
  * `id`: `String` (PK, UUID gerado na criação).
  * `name`: `String` (Not Null, Unique Index para garantir unicidade no banco).
* **`PlateProfileCrossRef` (`plate_profile_cross_ref`)**:
  * `plateId`: `String` (FK referenciando `PlateEntity.id` com `onDelete = ForeignKey.CASCADE`).
  * `profileId`: `String` (FK referenciando `ProfileEntity.id` com `onDelete = ForeignKey.CASCADE`).
  * Chave Primária Composta: `[plateId, profileId]`.
  * **Regra de Isolamento de Deleção:** A exclusão de um perfil remove automaticamente os registros associados em `PlateProfileCrossRef`, mas **não** remove as `PlateEntity` associadas (sem exclusão em cascata das placas, cumprindo as Seções 1.2 e 2.5 da spec).
* **`TelemetryEntity` (`telemetry`)**:
  * `id`: `Long` (PK, AutoGenerate).
  * `plateId`: `String` (FK referenciando `PlateEntity.id`).
  * `timestamp`: `Long` (Epoch timestamp em milissegundos).

### 2.2 Modelos de Relacionamento (Room)
* **`ProfileWithPlates`**: Mapeia a relação de um perfil para suas respectivas placas através da `@Junction(PlateProfileCrossRef::class)`.
* **`PlateWithProfiles`**: Mapeia as categorias às quais uma placa pertence.

### 2.3 DAOs (Data Access Objects)
* **`PlateDao`**:
  * `insert(plate: PlateEntity): Long`
  * `update(plate: PlateEntity): Int`
  * `delete(plate: PlateEntity): Int`
  * `getAllPlates(): Flow<List<PlateEntity>>`
  * `searchPlatesByName(query: String): Flow<List<PlateEntity>>` (Filtro inteligente via `WHERE name LIKE '%' || :query || '%'`).
  * `getPlateById(id: String): Flow<PlateEntity?>`
  * `getPlateByIdSync(id: String): PlateEntity?`
* **`ProfileDao`**:
  * `insert(profile: ProfileEntity): Long` (Tratamento de `SQLiteConstraintException` para violação de unicidade de nome).
  * `update(profile: ProfileEntity): Int`
  * `delete(profile: ProfileEntity): Int`
  * `getAllProfiles(): Flow<List<ProfileEntity>>`
  * `getProfileById(id: String): Flow<ProfileEntity?>`
  * `getProfileByName(name: String): ProfileEntity?`
  * `getProfileByNameSync(name: String): ProfileEntity?`
* **`CrossRefDao`**:
  * `insert(crossRef: PlateProfileCrossRef): Long`
  * `insertAll(crossRefs: List<PlateProfileCrossRef>)`
  * `delete(crossRef: PlateProfileCrossRef): Int`
  * `deleteByProfileId(profileId: String): Int`
  * `deleteAllPlatesFromProfile(profileId: String): Int`
  * `deleteRelation(profileId: String, plateId: String): Int`
  * `getPlatesForProfile(profileId: String): Flow<List<PlateEntity>>`
  * `getProfileWithPlates(profileId: String): Flow<ProfileWithPlates?>`
  * `getAllProfilesWithPlates(): Flow<List<ProfileWithPlates>>`
* **`TelemetryDao`**:
  * `insert(telemetry: TelemetryEntity): Long`
  * `getTelemetryByPlate(plateId: String): Flow<List<TelemetryEntity>>`

## 3. Gerenciamento de Sistema de Arquivos (I/O, Isolamento e Ciclo de Vida de Mídias)
* **Diretório de Armazenamento:** Diretório interno privado do aplicativo (`Context.filesDir/media` ou `Context.filesDir`).
* **Convenção de Nomenclatura:** UUID único com extensão adequada (ex: `img_<uuid>.png`, `audio_<uuid>.mp4`/`.3gp`) para prevenir qualquer conflito ou colisão de nomes.
* **Pipeline de Persistência e Limpeza de Mídias (Spec 2.4):**
  1. **Cadastro de Placa:**
     * A interface captura a URI temporária da fonte (Galeria, Câmera, Microfone ou Download da API ARASAAC).
     * O Repositório aciona o `LocalFileManager`, que copia/grava fisicamente os bytes no diretório privado do aplicativo.
     * O DAO insere a string da URI no Room após a conclusão do I/O físico.
  2. **Edição de Placa (Substituição de Mídia):**
     * Caso o usuário altere a imagem ou áudio de uma placa já cadastrada, o repositório identifica o arquivo físico antigo e o remove do armazenamento interno via `LocalFileManager.deleteFile(oldPath)`.
     * O novo arquivo de mídia é então copiado para o diretório interno privado e a nova URI é persistida no banco Room.
  3. **Remoção de Placa (Limpeza Completa):**
     * Ao remover uma placa, o repositório executa a exclusão da entidade no banco de dados e aciona o `LocalFileManager` para deletar obrigatoriamente todos os arquivos físicos associados (`imageUri` e `audioUri`), garantindo ausência de arquivos órfãos no armazenamento do dispositivo.

## 4. Componentes de Hardware, Áudio, Gravação e Síntese de Voz (TTS)
* **Motor de Baixa Latência (`AudioPlayerManager`):**
  * Implementado com a API `SoundPool` / `MediaPlayer` para manter áudios curtos decodificados na memória RAM e emitir callbacks de finalização de reprodução.
  * Garante que o tempo de resposta entre o toque na placa e a emissão sonora seja inferior a 200ms (Constitution - Seção 4).
* **Motor de Gravação de Áudio (`AudioRecorderManager`):**
  * Responsável pela gravação de áudio de voz do usuário via microfone durante o cadastro e edição de placas.
  * **Limite Estrito de Duração:** Duração limitada a no máximo 5 segundos. O sistema interrompe e finaliza a gravação automaticamente ao atingir 5 segundos, além de validar o tempo máximo da mídia gravada.
* **Motor de Síntese de Voz (`TtsManager`):**
  * Inicialização e configuração do `android.speech.tts.TextToSpeech` nativo com suporte ao idioma pt-BR.
  * Implementação de `UtteranceProgressListener` para emitir eventos assíncronos de início, conclusão e erro na síntese de voz.
  * **Otimização Fonética / Prevenção de Soletração:** Para evitar que o TTS soletre acrônimos ou palavras inseridas totalmente em caixa alta (ex: "ÁGUA", "BOLA"), os nomes das placas são normalizados antes da persistência para formato em minúsculo com apenas a primeira letra capitalizada (`name.trim().lowercase().replaceFirstChar { it.titlecase() }`).
* **Coordenador de Reprodução e Fila Sequencial (`SoundPlaybackCoordinator` / `PlaybackQueueManager`):**
  * Gerencia uma fila de execução FIFO (First-In, First-Out) para reprodução sequencial de múltiplas placas acionadas em sequência ou via barra de frases (Spec 2.1).
  * Executa cada item da fila sequencialmente, aguardando o callback de conclusão do áudio anterior (via `AudioPlayerManager` ou `UtteranceProgressListener` do `TtsManager`) antes de iniciar o próximo item da fila, impedindo cortes sonoros ou sobreposições.
  * Fluxo condicional de cada item:
    1. Verifica se a placa possui `audioUri` configurada e se o arquivo físico correspondente existe e é válido.
    2. Existindo áudio válido, executa via `AudioPlayerManager`.
    3. Caso não exista áudio ou a execução falhe/arquivo seja inválido, aciona imediatamente o `TtsManager` utilizando o campo `name` da placa como texto de entrada.
  * Dispara a gravação assíncrona de telemetria (`TelemetryEntity`) em background sem bloquear o fluxo sonoro.

## 5. Integração com API Externa (ARASAAC)
* **Cliente de Rede:** Retrofit 2 + OkHttp + Gson/Moshi Converter.
* **Fluxo de Busca com Parâmetro Inicial, Download e Sugestão Gramatical (Spec 2.2):**
  1. O diálogo de busca ARASAAC recebe um parâmetro opcional `initialQuery: String?`.
  2. Ao ser acionado a partir do diálogo de cadastro/edição de placa, se o campo `edtPlateName` contiver texto, este valor é passado como `initialQuery`, preenchendo automaticamente o campo de busca `edtArasaacQuery` e acionando de imediato a consulta com debounce.
  3. O `ArasaacApiService` realiza a consulta GET ao endpoint público de busca (`/pictograms/{locale}/search/{term}`).
  4. A lista de resultados é exibida em grade no diálogo de seleção ARASAAC contendo os pictogramas e seus respectivos termos/palavras-chave em português.
  5. Ao selecionar um pictograma, o repositório baixa assincronamente o stream de bytes da imagem (`/pictograms/{id}`) e o salva no armazenamento interno via `LocalFileManager`.
  6. **Sugestão de Nome:** A URI local resultante e o nome/termo do pictograma sugerido pela API são retornados ao formulário de cadastro/edição da placa, preenchendo automaticamente o campo "Nome" com a primeira letra capitalizada para maior corretude gramatical e agilidade.

## 6. Sincronização, Empacotamento e Backup
* **Exportação (`BackupManager`):**
  * Utiliza `ZipOutputStream` para consolidar o banco de dados Room (arquivos `.db`, `.db-wal`, `.db-shm`) e os arquivos físicos de mídia do diretório privado em um arquivo `.zip` único e estruturado.
  * Compartilhamento via `Storage Access Framework` (SAF) ou `Intent.ACTION_SEND` para transferência pedagógica (professor-aluno) e backup offline.
* **Importação (`BackupManager`):**
  * Utiliza `ZipInputStream` para descompactar o arquivo `.zip`.
  * Valida a estrutura do pacote, substitui os arquivos de mídia no diretório interno e restaura os arquivos do banco de dados Room após fechar conexões ativas.
  * Reabre e reinicializa a instância do `EloSonoroDatabase` garantindo a integridade dos dados.

## 7. Controle de UI, Estados e Acessibilidade Motora

### 7.1 Gestão de Estados de Operação (`ModeState`), Ícones e Preferências do Usuário
* **Estados de Operação e Metáfora Visual:**
  * **Modo de Uso (`isEditMode = false`):** Interface focada prioritariamente na prancha de comunicação. Controles administrativos, criação e edição de placas e categorias permanecem desabilitados/ocultos. Representado pelo ícone de **cadeado fechado** (`ic_lock_closed`).
  * **Modo Edição (`isEditMode = true`):** Habilita ferramentas de edição, exibindo a barra de busca inteligente, o botão flutuante de adicionar placas (`fabAddPlate`), gerenciamento de categorias e controles de edição/exclusão de itens. Representado pelo ícone de **cadeado aberto** (`ic_lock_open`).
* **Exibição na Tela Inicial:**
  * A tela principal exibe apenas o ícone representativo do modo atual (cadeado fechado ou cadeado aberto), eliminando rótulos de texto redundantes da tela inicial para evitar poluição visual.
* **Componente Unificado no Menu Centralizado com Toggles Aninhados:**
  * No menu centralizado, o status do modo e o controle de alternância são consolidados em um componente unificado (toggle/switch) com texto descritivo e ícone de cadeado.
  * Aninhados ao toggle de modo, disponibilizam-se dois controles adicionais persistidos em `UserPreferences` (`PreferencesManager`):
    * `showSearchInUseModeFlow: StateFlow<Boolean>` / `setShowSearchInUseMode(Boolean)`
    * `showCategoriesInUseModeFlow: StateFlow<Boolean>` / `setShowCategoriesInUseMode(Boolean)`
* **Emissão e Reatividade:**
  * O `BoardViewModel` expõe um `BoardUiState` consolidado, refletindo reativamente as alterações de modo e visibilidade em tempo real.

### 7.2 Grade de Acessibilidade Motora e Seletor de Densidade
* A prancha principal utiliza `RecyclerView` com `GridLayoutManager`.
* **Layouts Permitidos:** A grade de exibição suporta estritamente **2 colunas** (ampla área de toque para usuários com dificuldades motoras ou acuidade visual reduzida), **4 colunas** (densidade padrão) ou **6 colunas** (densidade máxima), com suporte à rolagem vertical contínua.
* **Ação Rápida no Menu:** A densidade da grade é alternável pelo usuário através de ação rápida no Menu Centralizado, sendo persistida via `PreferencesManager` e observada reativamente via `spanCountFlow: StateFlow<Int>`.

### 7.3 Menu Centralizado de Controle e Navegação (`dialog_main_menu.xml`)
* **Arquitetura e Acionamento:**
  * Janela modal centralizada (Bottom Sheet Dialog ou Dialog modal) acionada por um botão discreto e dedicado de fácil alcance na tela principal (`btnOpenMenu`).
* **Componentes Migrados e Centralizados no Menu:**
  1. **Seletor de Densidade (Colunas):** Ação rápida permitindo ao usuário comutar instantaneamente a grade entre 2, 4 ou 6 colunas via `viewModel.setSpanCount(2|4|6)`.
  2. **Seletor de Modo (Uso/Edição) e Toggles Aninhados:**
     * Toggle principal unificado com texto descritivo ("Modo de Uso" / "Modo de Edição") e ícone de cadeado (fechado / aberto).
     * Sub-container aninhado contendo dois `MaterialSwitch`:
       - `switchShowSearchInUse`: "Exibir barra de pesquisa no Modo de Uso" (chave `key_show_search_in_use_mode`).
       - `switchShowCategoriesInUse`: "Exibir categorias na tela inicial no Modo de Uso" (chave `key_show_categories_in_use_mode`).
  3. **Seletor de Categorias (Perfis):** Listagem interativa contendo a opção padrão "Todas as Placas" e os perfis cadastrados para filtragem da prancha principal. No Modo Edição, disponibiliza atalhos para criação e gerenciamento de categorias.

### 7.4 Ajuste de Layout, Margens de Segurança e Touch Targets
* **Afastamento das Bordas Físicas e Barras de Sistema:**
  * `activity_main.xml` e os layouts principais recebem margens internas (`padding`) e externas (`margins`) significativamente ampliadas nas extremidades superior (topo) e inferior (base).
  * O botão de acesso ao "Menu Centralizado" e o botão "Adicionar Nova Placa" (`fabAddPlate`) são posicionados com recuo de segurança reforçado em relação aos limites da tela.
  * Isolamento contra interferência e acionamentos acidentais decorrentes de gestos ou toques próximos à barra de status (topo) e à barra de navegação/gestos do sistema operacional (base).

### 7.5 Pesquisa Inteligente e Categorias com Visibilidade Condicional
* **Visibilidade da Barra de Busca (`inputLayoutSearch`):**
  * Visível (`View.VISIBLE`) quando: `isEditMode == true || showSearchInUseMode == true`.
  * Oculta (`View.GONE`) quando: `isEditMode == false && showSearchInUseMode == false`.
* **Visibilidade do Carrossel de Categorias na Tela Inicial:**
  * A tela principal (`activity_main.xml`) possui um contêiner horizontal de chips de categorias abaixo da AppBar.
  * Visível (`View.VISIBLE`) quando: `isEditMode == true || showCategoriesInUseMode == true`.
  * Oculto (`View.GONE`) quando: `isEditMode == false && showCategoriesInUseMode == false`.
* **Filtragem Reativa:** O `BoardViewModel` processa a consulta de busca e a categoria ativa via flows reativos combinados conectados ao `PlateDao` e `CrossRefDao`.

### 7.6 Gestão de Placas e Perfis: Telas, Validações e Ciclo de Vida (Spec 2.4 e 2.5)

#### 7.6.1 Gestão de Placas e Formulário Despoluído
* **Layout Despoluído (`dialog_plate_editor.xml`):**
  * Organização estruturada em blocos visuais (`MaterialCardView`):
    1. *Identificação:* Campo do Nome da Placa com validação obrigatória.
    2. *Mídia Visual:* Pré-visualização com botões compactos de Galeria e ARASAAC (com preenchimento automático de consulta).
    3. *Mídia Sonora:* Gravador com limite de 5s, seletor de arquivo e botão de teste.
    4. *Categorização:* Componente seletor de categoria (`ExposedDropdownMenu` / Chips de categorias cadastradas) permitindo associar a placa a uma ou mais categorias no ato de sua criação.
* **Fluxo de Persistência Transacional:**
  * `SavePlateUseCase` / `PlateRepository` recebem os dados da placa e a lista de IDs de categorias selecionadas (`categoryIds: List<String>`).
  * A operação grava fisicamente a mídia no armazenamento interno, insere a `PlateEntity` no Room e registra os vínculos em `PlateProfileCrossRef` de forma transacional.
* **Edição e Exclusão de Placas:**
  * Na substituição ou exclusão de placa, remove fisicamente todos os arquivos de mídia associados do armazenamento interno.

#### 7.6.2 Gestão de Perfis (Categorias)
* **Cadastro de Perfis:** Validação estrita de unicidade do nome com mensagem amigável para `SQLiteConstraintException`.
* **Listagem Global e Atalho Rápido:** Listagem de seleção com todas as placas do sistema e atalho para criação rápida de placa durante a edição do perfil.
* **Deleção Segura:** A exclusão de perfil remove vínculos na tabela associativa sem deletar as placas (`PlateEntity` permanece intacta).

### 7.7 Identidade Visual e Configuração de Ícone (Spec 3.5)
* **Ativo Mestre Oficial:** Imagem oficial fornecida salva em `app/src/main/res/drawable/app_icon.png` (512x512 px).
* **Configuração no Android:**
  * No `AndroidManifest.xml`, configurar `android:icon="@drawable/app_icon"` e `android:roundIcon="@drawable/app_icon"`.
  * Estruturar o `mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml` utilizando a imagem `app_icon.png` centralizada com camada de fundo correspondente à tonalidade do logotipo (`#F8F7EE`), garantindo renderização nítida sem distorções ou cortes indesejados em qualquer launcher Android.

### 7.8 Temas Claro e Escuro (Material Design 3 - Spec 3.5)
* **Temas em XML:**
  * `app/src/main/res/values/themes.xml` (Tema Claro base).
  * `app/src/main/res/values-night/themes.xml` (Tema Escuro base).
* **Tokens Semânticos de Cor:**
  * Configuração consistente dos tokens de cores (`colorPrimary`, `colorOnPrimary`, `colorSurface`, `colorOnSurface`, `mode_use_badge`, `mode_use_badge_bg`, etc.).
  * Validação de contraste de acordo com os padrões de acessibilidade WCAG AAA/AA, assegurando que pictogramas, textos e botões tenham destaque visual perfeito tanto no modo diurno quanto no modo noturno.
