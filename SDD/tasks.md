# Tasks
Lista de tarefas pendentes.

## Fase 1: Configuração Inicial e Infraestrutura
- [x] Configurar projeto Android (Min API 24).
- [x] Adicionar dependências no `build.gradle` (Room, Coroutines, Hilt/Koin, Retrofit, ViewModel).
- [x] Configurar Injeção de Dependência na classe `Application`.
- [x] Implementar classe base do banco de dados `EloSonoroDatabase` (Room).

## Fase 2: Camada de Dados (Room)
- [x] Criar `PlateEntity`, `ProfileEntity`, `TelemetryEntity`.
- [x] Criar entidade associativa `PlateProfileCrossRef` (chave primária composta).
- [x] Implementar `PlateDao` (incluindo busca por nome `LIKE`).
- [x] Implementar `ProfileDao` (com tratamento de conflito `UNIQUE`).
- [x] Implementar `CrossRefDao` (queries de junção N:M).

## Fase 3: Camada de Arquivos (I/O) e Mídia
- [x] Criar `LocalFileManager` para copiar e salvar arquivos no `Context.filesDir` com renomeação via UUID.
- [x] Implementar `AudioPlayerManager` utilizando `SoundPool` para garantia de latência < 200ms.
- [x] Implementar `TtsManager` integrando `TextToSpeech` nativo do Android.
- [x] Integrar `AudioPlayerManager` e `TtsManager` no fluxo condicional (se áudio local falhar, aciona TTS).

## Fase 4: Integração Externa (ARASAAC)
- [x] Definir modelos de dados JSON da API ARASAAC.
- [x] Criar interface Retrofit para os endpoints de busca de pictogramas.
- [x] Implementar repositório e rotina de download do `InputStream` e salvamento via `LocalFileManager`.

## Fase 5: Repositórios e Casos de Uso
- [x] Implementar repositório central unindo Room e LocalFileManager para salvar Placas (garantindo persistência física antes do banco).
- [x] Implementar repositório de Perfis com a lógica de associação de placas no ato da criação.
- [x] Implementar rotina de coleta de métricas ocultas (gravação no `TelemetryEntity`).

## Fase 6: Camada de Interface de Usuário (UI)
- [x] Criar ViewModels para controle de estado via `StateFlow`.
- [x] Implementar `ModeState` (Modo Edição vs Modo de Uso).
- [x] Implementar UI da prancha principal com `RecyclerView`/`GridLayoutManager`.
- [x] Adicionar controle dinâmico de `spanCount` (2, 4, 8, 16) via SharedPreferences/DataStore.
- [x] Implementar telas/diálogos de criação e edição bloqueando submissões com campos vazios (validação estrita do `spec.md`).
- [x] Vincular cliques na placa à execução de áudio/TTS.

## Fase 7: Sistema de Sincronização
- [ ] Criar rotina utilitária `BackupManager` com `ZipOutputStream`.
- [ ] Implementar empacotamento completo (arquivos físicos + `.db`, `-wal`, `-shm`).
- [ ] Implementar desempacotamento e reestabilização do banco de dados Room.
- [ ] Conectar funções de backup à interface do usuário.

## Refatoração 1: Correção da Spec 3.2
- [x] Alterar layout de exibição das placas para 2 colunas ou 4 colunas com rolagem vertical. O usuário deve poder alternar entre os dois modos.

## Refatoração 2: Ciclo de Vida e Limpeza de Mídias em Placas (Spec 2.4, Plan 3 e 7.4.1)
- [x] Implementar e validar via testes a remoção física de arquivos de mídia antigos no `Context.filesDir` ao editar uma placa e substituir imagem ou áudio.
- [x] Implementar e validar via testes a exclusão física obrigatória de todas as mídias associadas (`imageUri` e `audioUri`) ao remover uma placa do sistema.

## Refatoração 3: Edição de Perfis e Gerenciamento de Vínculos (Spec 2.5, Plan 2.3 e 7.4.2)
- [x] Implementar e validar fluxo de edição de categorias/perfis existentes permitindo alteração do nome com validação de unicidade.
- [x] Implementar e validar atualização dinâmica de vínculos (adição e remoção de placas associadas) na edição de um perfil existente.

## Refatoração 4: Validação Estrita e Integridade Referencial (Spec 1.2, 2.4, 2.5)
- [ ] Validar e garantir via testes unitários que a exclusão de categorias/perfis não exclua em cascata as placas associadas (`PlateEntity` permanece intacta).
- [ ] Validar bloqueio de nomes duplicados em categorias com emissão de erro explícito na UI ao capturar violação de unicidade (`SQLiteConstraintException`).
- [ ] Validar bloqueio com alerta visual de submissões de placas ou perfis com campo 'Nome' vazio ou em branco.
- [ ] Garantir o fluxo de cópia física obrigatória de mídias para `Context.filesDir` antes da persistência no Room (zero BLOBs no banco).

## Refatoração 5: Performance de Áudio, Fallback TTS e Telemetria (Spec 2.1, Constitution 4 e 5)
- [ ] Validar latência de áudio inferior a 200ms no acionamento de placas com `SoundPool`.
- [ ] Validar fallback imediato para Text-to-Speech (TTS) nativo caso o áudio local não exista ou seja inválido.
- [ ] Validar coleta silenciosa de telemetria em segundo plano (`TelemetryEntity`) sem impacto na latência sonora.
- [ ] Validar reatividade da pesquisa inteligente por nome na prancha principal.

## Refatoração 6: Listagem Global com Busca e Criação Rápida de Placas na Edição de Perfis (Spec 2.5 e Plan 7.4.2)
- [x] Alimentar a seleção de placas do diálogo de criação/edição de perfil com a totalidade das placas do sistema (independente da categoria ativa).
- [x] Implementar campo de busca em tempo real no diálogo de seleção de perfil com filtragem preservando o estado dos checkboxes marcados.
- [x] Implementar atalho ("Nova Placa") no diálogo de criação/edição de perfil permitindo cadastrar uma placa e vinculá-la imediatamente à seleção.
- [x] Criar testes unitários para a filtragem de busca no adapter de seleção e para a vinculação integrada de novas placas.

## Refatoração 7: Normalização de Nomes para TTS e Sugestão Automática via ARASAAC (Spec 2.2, 2.4 e Plan 4, 5, 7.4.1)
- [x] Implementar formatação e sanitização de nomes de placas (primeira letra maiúscula, demais minúsculas) antes da persistência no banco para evitar soletração pelo TTS.
- [x] Atualizar o diálogo e repositório ARASAAC para retornar o termo/descrição sugerido pela API ao selecionar um pictograma.
- [x] Preencher automaticamente o campo "Nome" no formulário de cadastro/edição de placa com a palavra-chave sugerida pela ARASAAC formatada.
- [x] Criar testes unitários para a rotina de capitalização/sanitização de nomes e para o preenchimento automático por termo ARASAAC.

## Refatoração 8: Fila de Reprodução Sequencial e Limite de Gravação de Áudio (Spec 1.1, 2.1, 2.4 e Plan 4, 7.4.1)
- [x] Implementar `AudioRecorderManager` com limite máximo estrito de 5 segundos de gravação de voz e interrupção automática ao atingir o tempo.
- [x] Integrar `UtteranceProgressListener` no `TtsManager` para escutar e emitir callbacks de conclusão e erro de síntese vocal.
- [x] Implementar estrutura de fila sequencial (FIFO) no `SoundPlaybackCoordinator` / `PlaybackQueueManager` para coordenar a reprodução ordenada de múltiplas placas aguardando o término do áudio/TTS anterior.
- [x] Criar testes unitários para o enfileiramento sequencial de reprodução sonora (FIFO) e validação do limite de 5 segundos na gravação.

## Refatoração 9: Acessibilidade de UI, Menu Centralizado, Visibilidade Condicional e Margens de Segurança (Spec 2.3, 3.1, 3.2, 3.3, 3.4 e Plan 7.1, 7.2, 7.3, 7.4, 7.5)
- [x] Criar recursos visuais vetoriais de ícones para estados de operação (cadeado fechado para Modo de Uso e cadeado aberto para Modo Edição) e layout da janela do Menu Centralizado (`dialog_main_menu.xml` ou `bottom_sheet_main_menu.xml`).
- [x] Implementar o componente de Menu Centralizado (Bottom Sheet / Dialog) contendo o Seletor de Densidade (comutação estrita entre 2, 4 ou 6 colunas), o Toggle Unificado de Modo (texto descritivo + ícone de cadeado) e a Listagem de Categorias (Perfis) para filtragem da prancha principal.
- [x] Atualizar a tela principal (`activity_main.xml` e `MainActivity.kt`) para exibir na top bar apenas o ícone do estado ativo atual (cadeado fechado ou aberto) e o botão dedicado para abertura do Menu Centralizado.
- [x] Implementar a visibilidade condicional da barra de pesquisa inteligente na tela principal, ocultando-a completamente (`View.GONE`) no Modo de Uso (`isEditMode = false`) e reativando-a (`View.VISIBLE`) no Modo Edição (`isEditMode = true`).
- [x] Reestruturar os layouts para aplicar margens internas (`padding`) e externas (`margins`) significativamente ampliadas no topo e na base da prancha principal, afastando os botões de ação ("Adicionar Nova Placa" e "Menu") das bordas físicas e prevenindo interferência com as barras de status e navegação do SO.
- [x] Criar testes unitários e de interface para validar a alternância do toggle unificado, a visibilidade condicional da barra de busca e a comutação estrita de densidade (2, 4 ou 6 colunas).

## Refatoração 10: Temas Claro/Escuro e Identidade Visual (Spec 3.5 e Plan 7.7, 7.8)
- [x] Salvar e registrar a imagem mestre oficial do ícone em `res/drawable/app_icon.png` (512x512 px).
- [x] Configurar a paleta de cores e temas Material 3 nos arquivos `values/themes.xml` e `values-night/themes.xml` assegurando conformidade de contraste para CAA (WCAG AAA/AA).
- [x] Configurar o ícone do aplicativo no `AndroidManifest.xml` e na estrutura de mipmaps/adaptive-icon utilizando o ativo `app_icon.png` e fundo neutro compatível.
- [x] Validar a alternância dinâmica entre tema claro e tema escuro no dispositivo sem perdas de estado ou problemas de contraste nos pictogramas.

## Refatoração 11: Otimização do Cadastro de Placas e Busca na ARASAAC (Spec 2.2, 2.4 e Plan 5, 7.6.1)
- [x] Atualizar o fluxo do botão "Buscar na ARASAAC" no diálogo de placa para pré-preencher o campo de busca e disparar a consulta quando o campo "Nome da Placa" já possuir texto preenchido.
- [x] Redesenhar `dialog_plate_editor.xml` para despoluir a interface em blocos visuais limpos (Identificação, Mídias e Categorização).
- [x] Incluir seletor de categorias no diálogo de cadastro de placas permitindo associar opcionalmente a nova placa a categorias existentes no momento de sua criação.
- [x] Atualizar o fluxo de persistência (`SavePlateUseCase` / `PlateRepository`) para salvar transacionalmente os vínculos da placa com as categorias em `PlateProfileCrossRef`.
- [x] Criar testes unitários para a pré-alimentação da busca ARASAAC e para a persistência da relação placa-categoria no ato da criação.

## Refatoração 12: Customização e Toggles do Modo de Uso no Menu Centralizado (Spec 2.3, 3.1, 3.3 e Plan 7.1, 7.3, 7.5)
- [ ] Adicionar ao `PreferencesManager` e `UserPreferences` os estados persistidos para exibição opcional da barra de busca e de categorias no Modo de Uso (`showSearchInUseMode` e `showCategoriesInUseMode`).
- [ ] Adicionar no `dialog_main_menu.xml`, aninhados ao seletor de modo, os dois switches de personalização do Modo de Uso.
- [ ] Adicionar o contêiner de categorias na tela inicial (`activity_main.xml`).
- [ ] Implementar a lógica reativa de visibilidade na `MainActivity` para que a barra de busca e as categorias respeitem as preferências ativas durante o Modo de Uso.
- [ ] Criar testes unitários e de UI para validar o comportamento reativo dos novos toggles e a retenção de estado nas preferências.
