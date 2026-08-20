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
- [ ] Criar `LocalFileManager` para copiar e salvar arquivos no `Context.filesDir` com renomeação via UUID.
- [ ] Implementar `AudioPlayerManager` utilizando `SoundPool` para garantia de latência < 200ms.
- [ ] Implementar `TtsManager` integrando `TextToSpeech` nativo do Android.
- [ ] Integrar `AudioPlayerManager` e `TtsManager` no fluxo condicional (se áudio local falhar, aciona TTS).

## Fase 4: Integração Externa (ARASAAC)
- [ ] Definir modelos de dados JSON da API ARASAAC.
- [ ] Criar interface Retrofit para os endpoints de busca de pictogramas.
- [ ] Implementar repositório e rotina de download do `InputStream` e salvamento via `LocalFileManager`.

## Fase 5: Repositórios e Casos de Uso
- [ ] Implementar repositório central unindo Room e LocalFileManager para salvar Placas (garantindo persistência física antes do banco).
- [ ] Implementar repositório de Perfis com a lógica de associação de placas no ato da criação.
- [ ] Implementar rotina de coleta de métricas ocultas (gravação no `TelemetryEntity`).

## Fase 6: Camada de Interface de Usuário (UI)
- [ ] Criar ViewModels para controle de estado via `StateFlow`.
- [ ] Implementar `ModeState` (Modo Edição vs Modo de Uso).
- [ ] Implementar UI da prancha principal com `RecyclerView`/`GridLayoutManager`.
- [ ] Adicionar controle dinâmico de `spanCount` (2, 4, 8, 16) via SharedPreferences/DataStore.
- [ ] Implementar telas/diálogos de criação e edição bloqueando submissões com campos vazios (validação estrita do `spec.md`).
- [ ] Vincular cliques na placa à execução de áudio/TTS.

## Fase 7: Sistema de Sincronização
- [ ] Criar rotina utilitária `BackupManager` com `ZipOutputStream`.
- [ ] Implementar empacotamento completo (arquivos físicos + `.db`, `-wal`, `-shm`).
- [ ] Implementar desempacotamento e reestabilização do banco de dados Room.
- [ ] Conectar funções de backup à interface do usuário.