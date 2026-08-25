# Specifications - EloSonoro
Especificações detalhadas das funcionalidades.

## 1. Entidades de Domínio

### 1.1 Placas (Plates)
* **Definição:** Unidade fundamental de comunicação.
* **Atributos Obrigatórios:** Nome. O valor do nome deve ser gravado obrigatoriamente formatado em minúsculo com a primeira letra capitalizada (ex: "Água", "Quero brincar"), prevenindo que o sintetizador de voz (TTS) soletre palavras que estejam totalmente em maiúsculas.
* **Atributos Opcionais:** Imagem (URI local gerada via Galeria, Câmera ou API ARASAAC), Áudio gravado (URI local).
* **Independência:** Uma placa existe de forma independente no sistema, sem obrigatoriedade de vínculo a categorias.

### 1.2 Categorias/Perfis (Profiles)
* **Definição:** Agrupadores lógicos de placas (ex: Pessoas, Ações, Locais).
* **Relacionamento:** Múltiplas placas para múltiplas categorias (N:M). Uma placa pode pertencer a zero, uma ou várias categorias simultaneamente.
* **Regra de Deleção:** A exclusão de uma categoria desfaz o vínculo, mas **não** exclui as placas associadas a ela. O efeito cascata é estritamente proibido nesta operação.

## 2. Comportamentos do Sistema

### 2.1 Lógica de Reprodução Sonora
Fluxo condicional executado ao tocar em uma placa:
1. O sistema verifica a existência de um arquivo de áudio local válido associado à placa.
2. Se existir, reproduz o arquivo de áudio.
3. Se não existir (ou o arquivo for inválido), o sistema aciona o Text-to-Speech (TTS) nativo do Android utilizando a string do campo "Nome" da placa.

### 2.2 Gerenciamento de Imagens Externas e Sugestão Gramatical
* Integração com API ARASAAC para pesquisa de pictogramas. O sistema deve baixar a imagem, salvá-la fisicamente e registrar a URI no banco de dados, em conformidade com as regras de persistência.
* **Sugestão Automática de Nome:** Ao selecionar um pictograma da API ARASAAC, o sistema deve alterar automaticamente o campo "Nome" da placa no formulário de cadastro/edição para o termo sugerido pela API (formatado com a primeira letra capitalizada), garantindo maior corretude gramatical e agilidade.

### 2.3 Pesquisa Inteligente
* O sistema deve prover uma barra de busca capaz de filtrar e localizar placas rapidamente pelo atributo "Nome" na visualização principal.

### 2.4 Cadastro de Placas
* **Entradas Exigidas:** Nome (Texto, obrigatório).
* **Entradas Opcionais:** Imagem (URI), Áudio de Voz (URI).
* **Regras de Validação e Formatação:** O sistema deve bloquear a operação de salvar e emitir um alerta visual se o campo "Nome" estiver vazio. Ao salvar o cadastro ou edição, o nome é sanitizado e gravado em minúsculo com a primeira letra capitalizada para evitar soletração pelo TTS.
* **Processamento I/O no Cadastro:** Ao confirmar o cadastro, se houver mídia associada (capturada pela câmera, selecionada da galeria ou baixada da API), o sistema deve obrigatoriamente copiar o arquivo para o diretório interno privado do aplicativo antes de persistir o registro no banco de dados.
* **Edição de Placas:** Ao editar uma placa, deve ser possivel alterar o nome da placa, bem como a imagem e o áudio associados a ela. Se a mídia for alterada, o sistema deve obrigatoriamente remover o arquivo de mídia antigo e copiar o novo arquivo de mídia para o diretório interno privado do aplicativo antes de persistir o registro no banco de dados.
* **Remoção de Placas:** O sistema deve permitir a remoção de placas e, ao remover uma placa, deve obrigatoriamente remover os arquivos de mídia associados a ela.

### 2.5 Cadastro e Edição de Perfis (Categorias)
* **Entradas Exigidas:** Nome da Categoria (Texto, obrigatório).
* **Regras de Validação:** O campo "Nome" não pode ser vazio e deve ser único no banco de dados. Tentativas de criar ou renomear uma categoria com nome idêntico a uma já existente devem ser bloqueadas com erro explícito.
* **Listagem Global de Vínculos com Filtro de Busca:** A interface de criação e edição de perfis deve listar obrigatoriamente **todas as placas cadastradas no sistema** (independentemente da categoria atualmente selecionada na prancha principal). A interface deve conter uma barra/campo de busca para filtrar placas em tempo real por "Nome", mantendo preservado o estado de seleção das placas (marcadas/desmarcadas) durante a filtragem.
* **Atalho para Nova Placa:** A interface de criação e edição de perfil deve conter um atalho direto ("Adicionar Nova Placa") permitindo cadastrar uma nova placa no sistema sem sair do fluxo. Ao confirmar o cadastro da placa, ela é salva e automaticamente vinculada e marcada na lista do perfil em edição.
* **Edição de Perfis (Categorias):** Deve ser possível editar os perfis existentes. A interface deve permitir a alteração do nome da categoria, bem como a adição e remoção de placas associadas a ela.
* **Deleção de Perfis (Categorias):** Deve ser possível deletar os perfis existentes. Ao deletar um perfil, as placas associadas a ele devem ser desvinculadas do perfil, mas não devem ser deletadas do banco de dados (proibição estrita de efeito cascata nas placas).

## 3. Estados de Interface e Acessibilidade

### 3.1 Modo Edição
* **Estado Ativado:** Permite criação, edição, deleção de placas e categorias, além do gerenciamento de vínculos.
* **Estado Desativado (Modo de Uso):** Interface bloqueada para modificações. Ações de deleção e edição de placas ou categorias são totalmente desabilitadas para evitar alterações acidentais pelo usuário final.

### 3.2 Grade de Acessibilidade Motora
* A interface (UI) da prancha de comunicação deve possuir controle de densidade visual.
* A interface deve exibir uma grade de exibição flexível. Os layouts permitidos por tela são estritamente: 2 colunas ou 4 colunas, com rolagem vertical.

## 4. Integração e Sincronização

### 4.1 Exportação/Importação e Backup
* O sistema deve possuir rotina de empacotamento que agrupa os registros do banco de dados (Room) e os arquivos de mídia físicos associados.
* Este pacote deve ser transferível entre diferentes dispositivos para garantir sincronização pedagógica (professor-aluno) e atuar como mecanismo de backup estruturado.