# 💎 DeluxeBounties

<p align="center">
  <img src="https://img.shields.io/badge/Software-NukkitPetteriM1Edition-7B2CBF?style=for-the-badge&logo=minecraft&logoColor=white">
  <img src="https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white">
  <img src="https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white">
  <img src="https://img.shields.io/badge/Database-SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white">
  <img src="https://img.shields.io/badge/Forms-FormConstructor-6C3FC5?style=for-the-badge">
  <img src="https://img.shields.io/badge/Economy-EconomyAPI-2E8B57?style=for-the-badge">
</p>

<p align="center">
  <b>Sistema de recompensas PvP para servidores NukkitPetteriM1Edition.</b><br>
  Coloca recompensas sobre otros jugadores y recibe el dinero acumulado al eliminarlos.
</p>

---

## 📖 Descripción

**DeluxeBounties** es un plugin de recompensas diseñado para servidores que utilizan **NukkitPetteriM1Edition**. Los jugadores pueden invertir dinero para establecer recompensas sobre otros usuarios, consultar objetivos activos y competir por convertirse en los mejores cazadores.

Utiliza **EconomyAPI** para las operaciones económicas, **FormConstructor** para sus interfaces y **SQLite** para almacenar la información.

## ✨ Características

- Creación de recompensas sobre otros jugadores.
- Recompensas anónimas.
- Reclamación automática al eliminar al objetivo.
- Recompensas acumulables de distintos jugadores.
- Cancelación y reembolso del 100 % de la aportación.
- Menús interactivos mediante FormConstructor.
- Consulta de recompensas activas.
- Consulta de aportaciones propias.
- Ranking de cazadores desde la interfaz.
- Historial personal de operaciones.
- Almacenamiento persistente mediante SQLite.
- Validaciones de importes, saldo y límites de recompensas activas.

## 🧩 Dependencias

| Dependencia | Uso | Requerida |
|---|---|---|
| NukkitPetteriM1Edition | API del servidor | Sí |
| FormConstructor 3.1.0 | Formularios e interfaces | Sí |
| EconomyAPI 2.0.7 | Operaciones económicas | Para funciones económicas |
| SQLite JDBC | Base de datos | Incluida mediante Maven |

**Importante:** EconomyAPI está declarada como dependencia opcional. Sin ella, el plugin puede iniciar, pero las operaciones económicas estarán desactivadas.

## 📥 Instalación

1. Descarga o compila `DeluxeBounties-1.0.0.jar`.
2. Comprueba que utilizas NukkitPetteriM1Edition.
3. Instala FormConstructor.
4. Instala EconomyAPI para habilitar las operaciones económicas.
5. Copia el JAR en la carpeta `plugins/`.
6. Reinicia el servidor.

Los archivos de configuración y lenguaje se generan automáticamente durante el primer inicio.

## 🎮 Comandos

| Comando | Descripción |
|---|---|
| `/bounty` | Abre el menú principal. |
| `/bounty <jugador> <cantidad>` | Coloca una recompensa. |
| `/bounty <jugador> <cantidad> anonimo` | Coloca una recompensa anónima. |
| `/bounty list <jugador>` | Consulta las recompensas activas de un jugador. |
| `/bounty cancel <id>` | Cancela una recompensa propia. |
| `/bounties` | Alias del comando principal. |

El subcomando `/bounty top` todavía no está implementado; el ranking se consulta desde la interfaz.

## 🔐 Permisos

| Permiso | Descripción |
|---|---|
| `deluxebounties.use` | Permite utilizar el comando y abrir el menú. |

## ⚙️ Configuración

Archivo: `plugins/DeluxeBounties/config.yml`

```yaml
settings:
  prefix: "&8[&5DeluxeBounties&8] "
  language: "es_ES"

database:
  type: SQLITE
  file: "bounties.db"

economy:
  provider: EconomyAPI
  currency-symbol: "$"

bounty:
  minimum-amount: 100
  maximum-amount: 10000000
  maximum-active-per-player: 10

  allow-anonymous: true
  allow-cancellation: true
  cancellation-refund-percent: 100

  claim-mode: ALL

antifarm:
  enabled: true
  kill-cooldown-seconds: 150
  prevent-self-bounty: true
  prevent-self-kill: true
  prevent-environmental-kills: true

notifications:
  broadcast-bounty-claimed: true
  notify-target: true
  notify-hunter: true

leaderboard:
  enabled: true
  entries: 10
```

### Opciones principales

| Opción | Descripción |
|---|---|
| `bounty.minimum-amount` | Importe mínimo para crear una recompensa. |
| `bounty.maximum-amount` | Importe máximo permitido. |
| `bounty.maximum-active-per-player` | Máximo de recompensas activas por creador. |
| `bounty.allow-anonymous` | Habilita recompensas anónimas. |
| `bounty.allow-cancellation` | Permite cancelar recompensas propias. |
| `bounty.cancellation-refund-percent` | Porcentaje de reembolso configurado. |
| `bounty.claim-mode` | Modo de reclamación configurado. |
| `antifarm.kill-cooldown-seconds` | Tiempo de enfriamiento configurado para el control anti-farm. |
| `leaderboard.entries` | Cantidad de entradas del ranking. |

> Algunas opciones están preparadas en la configuración para su uso y ampliación. Su presencia en el archivo no implica que todas tengan ya un comportamiento configurable completo.

## 🗄️ Almacenamiento

DeluxeBounties utiliza SQLite. La base de datos se crea automáticamente.

```yaml
database:
  type: SQLITE
  file: "bounties.db"
```

Se recomienda realizar copias de seguridad periódicas del archivo de base de datos antes de actualizar el plugin.

## 🛠️ Compilación

### Requisitos

- Java Development Kit 25.
- Maven.
- NukkitPetteriM1Edition.
- Los JAR locales de las dependencias.

### Dependencias locales

El proyecto espera estos archivos dentro de `libs/`:

```text
libs/
├── server.jar
├── FormConstructor-3.1.0.jar
└── economyapi-2.0.7.jar
```

### Generar el plugin

Desde la raíz del proyecto:

```bash
mvn clean package
```

El archivo compilado se genera en:

```text
target/DeluxeBounties-1.0.0.jar
```

Maven utiliza Shade para incluir SQLite JDBC en el JAR final.

## 🗺️ Estado del proyecto

DeluxeBounties se encuentra en desarrollo activo. Las funciones y opciones pueden ampliarse en futuras versiones.

## 👨‍💻 Autor

Desarrollado por **BrunoJaegerK** para el ecosistema de servidores de Minecraft Bedrock.

- GitHub: [@BrunoJaegerK](https://github.com/BrunoJaegerK)
- Proyecto: [DeluxeBounties](https://github.com/BrunoJaegerK/DeluxeBounties)

---

## 📄 Licencia

Este proyecto está bajo la licencia GNU General Public License v3.0.

Consulta el archivo [LICENSE](LICENSE) para más información.

<p align="center">
  <b>DeluxeBounties</b> · Recompensa a tus objetivos. Conviértete en el mejor cazador.
</p>
