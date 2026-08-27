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
  * **UI:** Activities, Adapters (`RecyclerView`), ViewModels e UI States gerenciados via `StateFlow`.

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
* **Fluxo de Busca, Download e Sugestão Gramatical:**
  1. O usuário digita o termo de busca na tela de pesquisa de pictogramas.
  2. O `ArasaacApiService` realiza a consulta GET ao endpoint público de busca (`/pictograms/{locale}/search/{term}`).
  3. A lista de resultados é exibida em grade no diálogo de seleção ARASAAC contendo os pictogramas e seus respectivos termos/palavras-chave em português.
  4. Ao selecionar um pictograma, o repositório baixa assincronamente o stream de bytes da imagem (`/pictograms/{id}`) e o salva no armazenamento interno via `LocalFileManager`.
  5. **Sugestão de Nome:** A URI local resultante e o nome/termo do pictograma sugerido pela API são retornados ao formulário de cadastro/edição da placa, preenchendo automaticamente o campo "Nome" com a primeira letra capitalizada para maior corretude gramatical e agilidade.

## 6. Sincronização, Empacotamento e Backup
* **Exportação (`BackupManager`):**
  * Utiliza `ZipOutputStream` para consolidar o banco de dados Room (arquivos `.db`, `.db-wal`, `.db-shm`) e os arquivos físicos de mídia do diretório privado em um arquivo `.zip` único e estruturado.
  * Compartilhamento via `Storage Access Framework` (SAF) ou `Intent.ACTION_SEND` para transferência pedagógica (professor-aluno) e backup offline.
* **Importação (`BackupManager`):**
  * Utiliza `ZipInputStream` para descompactar o arquivo `.zip`.
  * Valida a estrutura do pacote, substitui os arquivos de mídia no diretório interno e restaura os arquivos do banco de dados Room após fechar conexões ativas.
  * Reabre e reinicializa a instância do `EloSonoroDatabase` garantindo a integridade dos dados.

## 7. Controle de UI, Estados e Acessibilidade Motora

### 7.1 Gestão de Estados de Operação (`ModeState`)
* **Modo Edição (`isEditMode = true`):**
  * Habilita na UI botões e fluxos para criar, editar e excluir placas e categorias, além de vincular/desvincular placas.
* **Modo de Uso (`isEditMode = false`):**
  * Bloqueia alterações acidentais pelo usuário final. Elementos de edição, exclusão e adição têm visibilidade alterada (`View.GONE`) e interações desabilitadas.
* O estado é mantido e emitido via `isEditModeFlow: StateFlow<Boolean>` através do `PreferencesManager`.

### 7.2 Grade de Acessibilidade Motora (Seção 3.2 da Spec)
* A prancha principal utiliza `RecyclerView` com `GridLayoutManager`.
* **Layouts Permitidos:** A grade de exibição flexível suporta estritamente **2 colunas ou 4 colunas**, com suporte à rolagem vertical.
* A densidade da grade é configurável e alternável pelo usuário através de ação rápida na interface, sendo persistida via `PreferencesManager` (`SharedPreferences`) e observada via `spanCountFlow: StateFlow<Int>`.

### 7.3 Pesquisa Inteligente
* Barra de busca na prancha principal filtrando a lista de placas em tempo real pelo atributo `name`.
* O `BoardViewModel` recebe as alterações do campo de busca e consulta o `PlateDao` via Flow, atualizando a exibição sem recarregar a tela inteira.

### 7.4 Gestão de Placas e Perfis: Telas, Validações e Ciclo de Vida (Seções 2.4 e 2.5 da Spec)

#### 7.4.1 Gestão de Placas
* **Cadastro de Placas:**
  * Validação síncrona obrigatória do campo `name` (bloqueia submissão e emite alerta visual se vazio).
  * Formatação automática do nome (primeira letra maiúscula, demais minúsculas) para garantir leitura natural pelo TTS.
  * Seleção de imagem (Galeria, Câmera, ARASAAC com preenchimento automático de termo) e áudio (gravação limitada a no máximo 5 segundos ou seleção), salvando fisicamente no armazenamento interno antes de persistir no banco.
* **Edição de Placas:**
  * Permite alteração do nome (com sanitização e capitalização), substituição ou remoção de imagem e áudio (com gravação limitada a 5 segundos).
  * Se a mídia for substituída ou removida, o sistema deleta fisicamente o arquivo anterior do dispositivo e grava a nova mídia internamente antes de atualizar o Room.
* **Remoção de Placas:**
  * Diálogo de confirmação para exclusão da placa.
  * Ao confirmar, exclui o registro no banco Room e apaga obrigatoriamente todos os arquivos físicos de mídia (`imageUri` e `audioUri`) vinculados.

#### 7.4.2 Gestão de Perfis (Categorias)
* **Cadastro de Perfis:**
  * Validação do campo `name` (obrigatório, não vazio e único no banco). Bloqueio e mensagem explícita de erro em caso de duplicidade (`SQLiteConstraintException`).
  * Listagem de seleção alimentada pela totalidade das placas cadastradas no sistema (`PlateRepository.getAllPlates()`), independente de filtros ativos na tela principal.
* **Edição de Perfis:**
  * Permite renomear a categoria (mantendo as regras de validação e unicidade) e gerenciar os vínculos (adicionar ou remover placas associadas).
  * Exibe todas as placas cadastradas no sistema, com pré-seleção marcada para as placas atualmente vinculadas ao perfil.
* **Filtro de Busca na Seleção de Placas:**
  * Campo de busca integrado ao diálogo de criação/edição de perfil permitindo filtrar placas em tempo real pelo atributo `name`.
  * O `PlateSelectionAdapter` gerencia a filtragem da lista visível mantendo íntegro o conjunto de IDs selecionados (`selectedIds: MutableSet<String>`), garantindo que itens marcados não sejam desmarcados ao serem ocultados pelo filtro.
* **Atalho para Criação Rápida de Placa:**
  * Botão de atalho no diálogo de criação/edição de perfil ("Nova Placa") acionando o formulário de cadastro de placa.
  * Ao concluir o cadastro, a nova placa é persistida fisicamente e inserida no Room, sendo imediatamente inserida e marcada como selecionada na listagem de vínculos do perfil.
* **Deleção de Perfis:**
  * Diálogo de confirmação de exclusão da categoria.
  * Ao deletar o perfil, todos os vínculos com placas são desfeitos via `CrossRefDao` / `ForeignKey.CASCADE` na tabela associativa, **sem deletar as placas associadas do banco de dados** (proibição estrita de efeito cascata nas placas).