# 💎 DeluxeBounties

<p align="center">
  <img src="https://img.shields.io/badge/Software-NukkitPetteriM1Edition-7B2CBF?style=for-the-badge&logo=minecraft&logoColor=white">
  <img src="https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white">
  <img src="https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white">
  <img src="https://img.shields.io/badge/Database-H2-4479A1?style=for-the-badge">
  <img src="https://img.shields.io/badge/Forms-FormConstructor-6C3FC5?style=for-the-badge">
  <img src="https://img.shields.io/badge/Economy-EconomyAPI%20%7C%20EconomyExpandNK-2E8B57?style=for-the-badge">
</p>

<p align="center">
  <b>Sistema de recompensas PvP para servidores NukkitPetteriM1Edition.</b><br>
  Coloca recompensas sobre otros jugadores y recibe el dinero acumulado al eliminarlos.
</p>

---

## 📖 Descripción

**DeluxeBounties** es un plugin de recompensas diseñado para servidores que utilizan **NukkitPetteriM1Edition**. Los jugadores pueden invertir dinero para establecer recompensas sobre otros usuarios, consultar objetivos activos y competir por convertirse en los mejores cazadores.

Utiliza **EconomyAPI** o **EconomyExpandNK** como proveedor económico configurable, **FormConstructor** para sus interfaces y **H2 Database** para almacenar la información de forma persistente.

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
- Almacenamiento persistente mediante H2.
- Base de datos independiente y reubicada para evitar conflictos con otros plugins.
- Validaciones de importes, saldo y límites de recompensas activas.

## 🧩 Dependencias

| Dependencia | Uso | Requerida |
|---|---|---|
| NukkitPetteriM1Edition | API del servidor | Sí |
| FormConstructor 3.1.0 | Formularios e interfaces | Sí |
| EconomyAPI 2.0.7 | Economía principal | Si se selecciona EconomyAPI |
| EconomyExpandNK | Monedas secundarias | Si se selecciona EconomyExpandNK |
| H2 Database 2.1.214 | Almacenamiento de datos | Incluida en el plugin |

**Importante:** EconomyAPI y EconomyExpandNK son dependencias opcionales. El proveedor elegido en `config.yml` debe estar instalado y habilitado; si no está disponible, el plugin inicia con las operaciones económicas desactivadas.

H2 se incluye directamente dentro del JAR mediante Maven Shade, por lo que no necesitas instalarlo por separado en el servidor.

## 📥 Instalación

1. Descarga o compila `DeluxeBounties-1.0.0.jar`.
2. Comprueba que utilizas NukkitPetteriM1Edition.
3. Instala FormConstructor.
4. Instala el proveedor económico que hayas seleccionado en `config.yml`: EconomyAPI o EconomyExpandNK.
5. Copia el JAR en la carpeta `plugins/`.
6. Reinicia el servidor.

Los archivos de configuración, idioma y base de datos se generan automáticamente durante el primer inicio.

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
  type: H2
  file: "bounties"

economy:
  # Proveedores: EconomyAPI o EconomyExpandNK
  provider: EconomyAPI

  # Solo se utiliza con EconomyExpandNK
  currency-id: crystals

  # Símbolo utilizado con EconomyAPI
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

### Economía y selección de moneda

DeluxeBounties conserva **EconomyAPI** como proveedor predeterminado. Para utilizar una moneda secundaria, instala EconomyExpandNK y cambia la configuración:

```yaml
economy:
  provider: EconomyExpandNK
  currency-id: crystals
  currency-symbol: "$"
```

- `provider: EconomyAPI`: utiliza el saldo principal de EconomyAPI, sin cambios respecto a versiones anteriores.
- `provider: EconomyExpandNK`: utiliza el proveedor de monedas secundarias.
- `currency-id`: ID exacto de una moneda habilitada en `plugins/EconomyExpandNK/currencies.yml`, por ejemplo `crystals` o `tokens`.
- `currency-symbol`: símbolo mostrado cuando se utiliza EconomyAPI. Con EconomyExpandNK se toma el símbolo configurado para la moneda seleccionada.

El proveedor seleccionado se utiliza para cobrar las nuevas recompensas, pagar al cazador, reembolsar cancelaciones y revertir operaciones fallidas. Los importes respetan la precisión decimal de la moneda secundaria seleccionada.

**Importante:** mantén el mismo proveedor y el mismo `currency-id` mientras existan recompensas activas o registros históricos que quieras interpretar correctamente. El esquema actual no guarda un ID de moneda individual por recompensa; cambiar el proveedor puede hacer que recompensas pendientes se paguen en otra moneda y que los importes históricos se muestren con el símbolo nuevo.

#### Dependencia de ejecución

EconomyExpandNK es una dependencia opcional en tiempo de ejecución. No necesitas añadir su JAR a `libs/` ni incluir sus clases dentro de DeluxeBounties. Instala el plugin por separado y DeluxeBounties accederá a su API pública cuando se seleccione como proveedor.

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

## 🗄️ Almacenamiento H2

DeluxeBounties utiliza **H2 Database 2.1.214** como sistema de almacenamiento.

La base de datos se crea automáticamente en:

```text
plugins/
└── DeluxeBounties/
    ├── config.yml
    ├── lang.yml
    └── bounties.mv.db
```

### Ventajas

- Base de datos integrada en el plugin.
- No requiere instalar un servidor de base de datos adicional.
- Almacenamiento persistente de recompensas, historial y estadísticas.
- H2 se incluye dentro del JAR mediante Maven Shade.
- Paquete H2 reubicado para reducir conflictos con otros plugins.

**Copias de seguridad:** se recomienda realizar copias periódicas del archivo `bounties.mv.db` con el servidor detenido.

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

Maven utiliza **Maven Shade 3.6.2** para incluir H2 y reubicar sus paquetes dentro del JAR final.

## 🗺️ Estado del proyecto

DeluxeBounties se encuentra en desarrollo activo. Las funciones y opciones pueden ampliarse en futuras versiones.

## 👨‍💻 Autor

Desarrollado por **BrunoJaegerK** para el ecosistema de servidores de Minecraft Bedrock.

- GitHub: [@BrunoJaegerK](https://github.com/BrunoJaegerK)
- Proyecto: [DeluxeBounties](https://github.com/BrunoJaegerK/DeluxeBounties)

## 📄 Licencia

Este proyecto está bajo la licencia GNU General Public License v3.0.

Consulta el archivo [LICENSE](LICENSE) para más información.

---

<p align="center">
  <b>DeluxeBounties</b> · Recompensa a tus objetivos. Conviértete en el mejor cazador.
</p>

## Selección de moneda por recompensa

Al abrir la interfaz de creación, cada jugador puede elegir la moneda que desea aportar:

- **EconomyAPI**: moneda principal, disponible con la instalación habitual.
- **EconomyExpandNK**: aparecen automáticamente todas las monedas habilitadas en `currencies.yml` cuando el plugin está instalado.

EconomyExpandNK es opcional. Si no está instalado, DeluxeBounties mantiene el funcionamiento con EconomyAPI y no muestra monedas secundarias. No es necesario configurar un ID de moneda global para utilizar el desplegable.

Cada recompensa guarda su proveedor y su ID de moneda. Por eso, las recompensas creadas con monedas diferentes pueden coexistir; los pagos al cazador, reembolsos por cancelación y operaciones de recuperación utilizan la moneda original. Las recompensas antiguas de la base de datos se migran automáticamente como EconomyAPI.

La creación por comando `/bounty <jugador> <cantidad>` conserva su compatibilidad y utiliza el proveedor predeterminado de `config.yml`; para elegir cualquier moneda, utiliza la interfaz.

**Importante:** no deshabilites ni elimines una moneda de EconomyExpandNK mientras existan recompensas activas asociadas a ella. El proveedor y el ID deben seguir disponibles para poder pagar o reembolsar esas aportaciones.
