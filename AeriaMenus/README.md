🌟 AeriaMenus v1.0.0

O AeriaMenus é a solução definitiva "All-in-One" para gerenciamento, interatividade e customização visual de lobbies, hubs e redes de servidores de Minecraft profissionais.

Desenvolvido com foco extremo em performance, estabilidade e estética moderna, este plugin unifica diversas mecânicas essenciais em uma única engine otimizada para a API moderna do Paper (Minecraft 1.21+), permitindo que você substitua múltiplos plugins legados por um único software leve.

🚀 Funcionalidades Principais

💎 Menus Interativos Ilimitados: Crie quantos inventários virtuais desejar com suporte completo a cabeças customizadas (Base64), Custom Model Data, encantamentos invisíveis e muito mais.

🛡️ Aparência por Permissão (Dynamic Look): Mude instantaneamente a aparência de um item (ícone, nome, descrição) caso o jogador não possua uma permissão específica. Útil para separar de forma inteligente recompensas liberadas de bloqueadas!

💰 Sistema de Lojas Nativo (Integração Vault): Defina custos monetários para cliques nos menus. O plugin verifica o saldo, desconta o dinheiro via Vault, reproduz sons e gerencia mensagens de erro de forma automatizada.

📊 Scoreboard Lateral Anti-Flicker (AeriaBoard): Painel lateral totalmente imune a travamentos ou piscadas na tela. Oculta automaticamente os números vermelhos tradicionais (Paper 1.20.6+) e atualiza as linhas suavemente.

🎨 Visual Moderno & Gradientes: Suporte completo a gradientes RGB avançados via tag <gradient:#HEX1:#HEX2>Texto</gradient> e cores hexadecimais tradicionais &#HEX no chat, menus, scoreboard, tablist e bossbar.

🚪 Efeitos de Entrada Espetaculares: Mensagens de entrada/saída customizadas no chat, somadas a títulos gigantes na tela (titles), subtítulos suaves e efeitos sonoros imersivos.

⚙️ Engine Sequencial de Ações (Click Actions): Encadeie múltiplas ações em um único clique (sons, mensagens, abrir outros menus, executar comandos pelo console/jogador, conectar a servidores BungeeCord e alternar a visibilidade de jogadores).

🕶️ Relógio Mágico (Ocultar Jogadores): Mecânica nativa para que jogadores possam ocultar e revelar todos os outros usuários do lobby para reduzir lag visual.

🛑 Proteção de Lobbies Integrada: Bloqueio global contra quebra e colocação de blocos, queda de itens, perda de fome e recebimento de dano (com bypass automático para Administradores com OP).

🌀 Salvamento Automático do Void: Teleporte instantâneo e seguro de volta ao spawn caso algum jogador caia no void, excelente para lobbies flutuantes.

📑 Anti-Erros YAML e Watcher de Ficheiros: Captura erros de sintaxe nos arquivos de configuração e avisa o console com clareza sem travar o plugin. Conta também com um recarregador automático a cada 30 segundos!

📦 Instalação e Requisitos

Requisitos Técnicos

Servidor: PaperMC (ou forks compatíveis) rodando na versão 1.21 ou superior.

Java: Versão 21 ou superior instalada na máquina de hospedagem.

Dependências (Soft-Depends)

PlaceholderAPI (Opcional): Necessário para processar variáveis dinâmicas em tempo real nas mensagens, itens e scoreboard.

Vault (Opcional): Requerido caso queira ativar o módulo de custos financeiros nos menus.

Como Instalar

Coloque o arquivo .jar gerado na pasta plugins/ do seu servidor.

Inicie ou reinicie o servidor para que as pastas de fábrica sejam criadas.

Edite as configurações em plugins/AeriaMenus/config.yml e as mensagens em messages.yml.

Use o comando /am reload para atualizar o cache do plugin no servidor.

🛠️ Comandos e Permissões

Comando

Descrição

Permissão Recomendada

/aeriamenus ou /am

Exibe a central de ajuda interativa do plugin.

Livre para todos

/am reload

Recarrega completamente todas as configurações, mensagens e menus.

aeriamenus.admin

/am abrir <menu> [jogador]

Abre um menu específico para você ou outro jogador. (Ideal para NPCs ou consola externa)

aeriamenus.admin

📂 Visão Geral dos Arquivos de Configuração

1. Configuração Geral (config.yml)

Controla todos os módulos ativos, regras de proteção do spawn, as mensagens da barra rotativa (ActionBar), barra de chefes (BossBar), painel lateral (Scoreboard) e cabeçalho da lista de jogadores (Tablist).

modulos:
  dar-itens-ao-entrar: true  
  ativar-protecoes: true     
  ativar-scoreboard: true    
  ativar-bossbar: true
  ativar-chat: true          
  ativar-clima-sempre-dia: true
  ativar-mensagens-entrada: true
  ativar-efeitos-entrada: true
  usar-placeholderapi: true
  ativar-tablist-animada: true
  ativar-actionbar-rotativa: true
  usar-vault: true

spawn:
  teleportar-ao-entrar: true
  altura-void: 0.0
  local:
    mundo: "world"
    x: 0.0
    y: 100.0
    z: 0.0
    yaw: 0.0
    pitch: 0.0

bossbar:
  titulo: "&#00d4ff&lAERIA MENUS &8| &fBem-vindo, &a%player_name%"
  cor: "BLUE" 
  estilo: "SOLID" 
  progresso: 1.0

scoreboard:
  titulo: "<gradient:#00d4ff:#00ff55>&lAERIA MENUS</gradient>"
  linhas:
    - "&8&m                                   "
    - "&fPerfil:"
    - " &8• &7Jogador: &#00ff55%player_name%"
    - " &8• &7Cargo: &f%vault_rank%"
    - " &8• &7Moedas: &a$%vault_eco_balance_formatted%"
    - ""
    - "&fServidor:"
    - " &8• &7Online: &#ffaa00%server_online%&8/&7%server_max_players%"
    - " &8• &7Ping: &a%player_ping%ms"
    - ""
    - "&#00d4ffloja.aeriamenus.com"
    - "&8&m                                   "


2. Mensagens e Chat (messages.yml)

Responsável pela formatação do chat global e pelas mensagens/efeitos exibidos na entrada e na saída dos jogadores da rede.

chat:
  formato: "&#00d4ff[&bLobby&#00d4ff] &f%player_name% &8» &7%message%"

mensagens-entrada:
  entrou: "&8[&a+&8] &7%player_name% entrou no servidor."
  saiu: "&8[&c-&8] &7%player_name% saiu do servidor."

efeitos-entrada:
  titulo: "<gradient:#00d4ff:#00ff55>&lAERIA MENUS</gradient>"
  subtitulo: "&7Bem-vindo(a) ao servidor, &f%player_name%&7!"
  som: "ENTITY_PLAYER_LEVELUP"


3. Exemplo de Menu Customizado (menus/principal.yml)

Veja como é simples e limpa a estrutura de criação de menus. Note a utilização da aparência alternativa caso o jogador não tenha o VIP ativo:

titulo: "&8Menu AeriaMenus"
linhas: 3

comando:
  - "principal"
  - "jogos"
  - "menu"

itens:
  survival:
    slot: 11
    material: "GRASS_BLOCK"
    nome: "&#00ff55&lSURVIVAL"
    lore:
      - "&7Clique para entrar no servidor Survival."
    acoes:
      - "fechar"
      - "mensagem: &aConectando ao Survival..."
      - "som: ENTITY_EXPERIENCE_ORB_PICKUP"
      - "servidor: survival"

  recompensa_vip:
    slot: 13
    
    # --- [ESTADO 1] APARÊNCIA SE TIVER PERMISSÃO ---
    material: "DIAMOND"
    encantamentos:
      - "unbreaking:1"
    flags:
      - "HIDE_ENCHANTS"
    nome: "&#00d4ff&lKIT VIP DIÁRIO &8• &a[LIBERADO]"
    lore:
      - "&7Sua assinatura VIP está ativa!"
      - "&7Clique aqui para resgatar seus prêmios."

    # --- [ESTADO 2] APARÊNCIA DINÂMICA (CASO NÃO POSSUA A PERMISSÃO) ---
    se-nao-tiver-permissao: "aeria.vip"
    material-alternativo: "MINECART"
    nome-alternativo: "&#ff4444&lKIT VIP DIÁRIO &8• &c[BLOQUEADO]"
    lore-alternativa:
      - "&7Este item requer vantagens de Rank."
      - ""
      - "&cVocê não possui este nível de acesso!"
      - "&eAdquira vantagens em nossa loja."

    permissao: "aeria.vip"
    mensagem-erro: "&c&lERRO! &cVocê precisa ter o rank VIP para coletar esta recompensa."
    som-erro: "ENTITY_VILLAGER_NO"

    acoes:
      - "fechar"
      - "mensagem: &aVocê resgatou seu Kit VIP com sucesso!"
      - "som: ENTITY_PLAYER_LEVELUP"
      - "consola: give %player% diamond 5"

  comprar_vida:
    slot: 15
    material: "APPLE"
    nome: "&#ff4444&lMAÇÃ DA VIDA"
    lore:
      - "&7Compre uma maçã dourada por $500 moedas!"
      - ""
      - "&aClique para comprar."
    
    custo: 500.0
    mensagem-erro-dinheiro: "&cVocê não tem moedas suficientes! Custa $500."
    som-erro: "ENTITY_VILLAGER_NO"

    acoes:
      - "mensagem: &aCompra efetuada! Foram descontados $500 da sua conta."
      - "som: ENTITY_EXPERIENCE_ORB_PICKUP"
      - "consola: give %player% golden_apple 1"


⚡ Lista Completa de Ações de Clique

Qualquer ação configurada no campo acoes: de um item executa sequencialmente comandos específicos. Você pode usar:

fechar

O que faz: Fecha a janela do inventário atual do jogador.

mensagem: <conteúdo>

O que faz: Envia uma mensagem privada colorida para o jogador.

Exemplo: - "mensagem: &aVocê abriu o menu!"

comando: <comando_sem_barra>

O que faz: Força o jogador a digitar o comando especificado no chat (requer que ele tenha permissão para o comando).

Exemplo: - "comando: spawn"

consola: <comando_sem_barra>

O que faz: Executa um comando através do Console do Servidor (com poder máximo de Administrador). Substitui %player% pelo nome do jogador.

Exemplo: - "consola: give %player% diamond 1"

dinheiro: <dar/retirar> <quantidade>

O que faz: Modifica o saldo da carteira do jogador via Vault.

Exemplo: - "dinheiro: dar 250"

menu: <nome_do_arquivo>

O que faz: Abre instantaneamente outro arquivo .yml de menu da pasta menus/.

Exemplo: - "menu: principal"

servidor: <nome>

O que faz: Conecta o jogador a outro servidor conectado na proxy (BungeeCord/Velocity).

Exemplo: - "servidor: survival"

som: <NOME_DO_SOM_NATIVO>

O que faz: Reproduz um som do Minecraft na localização do jogador.

Exemplo: - "som: ENTITY_EXPERIENCE_ORB_PICKUP"

especial: alternar_visibilidade

O que faz: Esconde ou exibe os outros jogadores no Lobby para quem clicou no item.

🔒 Auditoria e Logs do Console

Com foco em gerenciamento seguro, o AeriaMenus registra eventos críticos de auditoria de cliques diretamente no terminal do console do seu servidor. Isso permite que os administradores monitorem facilmente o uso e evitem exploits:

[AeriaMenus] [AUDITORIA] O jogador Anne acionou o evento: consola: give Anne golden_apple 1
[AeriaMenus] [AUDITORIA] O jogador Anne acionou o evento: mensagem: &aCompra efetuada! Foram descontados $500 da sua conta.


AeriaMenus — Elevando o nível técnico e visual da sua rede de Minecraft.