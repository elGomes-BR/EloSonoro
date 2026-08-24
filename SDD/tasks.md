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

