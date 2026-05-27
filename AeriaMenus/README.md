# 🌟 Wiki AeriaMenus

Bem-vindo à documentação oficial do **AeriaMenus**! 
O AeriaMenus é a solução definitiva "All-in-One" para gerir o Lobby, Hub ou BungeeCord da sua rede de servidores. Desenvolvido com foco extremo em performance, substitui dezenas de plugins antigos por uma única "engine" poderosa.

---

## 📌 Índice
1. [Instalação e Dependências](#1-instalação-e-dependências)
2. [Comandos e Permissões](#2-comandos-e-permissões)
3. [Como Criar Menus](#3-como-criar-menus)
4. [Ações de Clique (Click Actions)](#4-ações-de-clique)
5. [Sistema Avançado: Economia e Permissões Visuais](#5-sistema-avançado-economia-e-permissões-visuais)

---

## 1. Instalação e Dependências

Para que o AeriaMenus funcione a 100% no seu servidor, certifique-se de que cumpre os seguintes requisitos:

* **Versão do Servidor:** PaperMC 1.21 (ou forks otimizados).
* **PlaceholderAPI (Opcional, mas Recomendado):** Para utilizar variáveis como `%player_name%` ou `%server_online%`.
* **Vault (Opcional):** Apenas necessário se quiser utilizar as funções de cobrar/dar dinheiro nos menus.

**Como Instalar:**
1. Coloque o ficheiro `AeriaMenus-1.0.0.jar` na pasta `plugins` do seu servidor.
2. Inicie o servidor para gerar as pastas.
3. Edite os ficheiros em `plugins/AeriaMenus/`.
4. Utilize o comando `/am reload` no jogo para aplicar as alterações.

---

## 2. Comandos e Permissões

O AeriaMenus é focado em segurança e performance. Apenas administradores precisam de permissões especiais.

| Comando | Descrição | Permissão |
| :--- | :--- | :--- |
| `/am` | Abre a central de ajuda do plugin. | *Nenhuma* |
| `/am reload` | Recarrega as configurações, mensagens e todos os menus. | `OP` (Operador) |

*(Nota: Os jogadores podem abrir menus específicos através de comandos personalizados definidos por si nos ficheiros dos menus, sem precisarem de permissões extra, a menos que as configure).*

---

## 3. Como Criar Menus

O AeriaMenus suporta **menus infinitos**. Para criar um menu novo, basta criar um ficheiro `.yml` dentro da pasta `plugins/AeriaMenus/menus/` (exemplo: `vip.yml`, `jogos.yml`).

### Estrutura Base de um Menu:
```yaml
titulo: "&8O Meu Menu Personalizado"
linhas: 3 # De 1 a 6 linhas (9 a 54 slots)

# (Opcional) Comandos que abrem este menu pelo chat:
comando:
  - "abrir_menu"
  - "atalho"

itens:
  # Identificador do item (pode ser qualquer nome)
  meu_item_1:
    slot: 13
    material: "DIAMOND"
    nome: "&#00d4ff&lDIAMANTE BRILHANTE"
    lore:
      - "&7Isto é uma descrição."
    acoes:
      - "mensagem: &aClicas-te no diamante!"
      - "som: ENTITY_PLAYER_LEVELUP"