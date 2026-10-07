# Sistema de Inventario de Mercadería (JavaFX)

Aplicación de **escritorio en Java** para controlar el inventario de mercadería de un almacén o tienda.

Guarda **todo** en un **repositorio de ficheros `.json` organizados por carpetas** y permite **migrar
posteriormente a una base de datos MySQL** con un solo botón, conservando los identificadores y las
relaciones entre datos.

---

## 1. Características

| Módulo | Qué hace |
|---|---|
| **Productos** | Alta, edición, baja y búsqueda (por nombre, código o descripción) con filtros por categoría |
| **Control de stock** | Entradas, salidas y ajustes con historial completo: stock anterior → resultante, motivo y usuario |
| **Categorías** | CRUD con recuento de productos; no deja borrar una categoría en uso |
| **Proveedores** | CRUD con contacto, teléfono, email y dirección |
| **Código de barras** | Generación automática **EAN-13 válido** (o Code-128 para códigos internos), imagen PNG y visualización |
| **Reportes** | **PDF** (OpenPDF) y **Excel** (Apache POI): inventario valorizado, movimientos por fechas, stock bajo y **etiquetas imprimibles con código de barras** |
| **Usuarios** | Múltiples usuarios con roles (`ADMIN` / `OPERARIO`), contraseñas con **SHA-256 + sal**, activar/desactivar |
| **Migración** | Copia JSON → MySQL, crea la base de datos y las tablas automáticamente, verifica los contados y permite volver a JSON |

Además:

* Panel de **resumen** con KPIs (productos activos, unidades, valor del inventario, stock bajo, movimientos de hoy).
* Interfaz con **iconos vectoriales en todos los botones** (SVG dibujados en código, sin librerías
  externas) y paleta de colores renovada: menú lateral en azul oscuro, acciones primarias en azul,
  PDF en rojo y Excel en verde, con colores de estado para stock bajo y alertas.
* La aplicación **arranca en modo JSON** y, si MySQL no está disponible, **vuelve a JSON automáticamente** mostrando un aviso: nunca se pierde el trabajo.
* Escrituras JSON **atómicas** (archivo temporal + renombre) para no corromper los datos en caso de corte.

---

## 2. Requisitos

* **JDK 17 o superior** (probado con **JDK 24**)
* **Maven 3.9** (probado con 3.9.9)
* **MySQL 8.x** — *solo* si quieres migrar la base de datos (opcional)

---

## 3. Cómo ejecutar

### Opción A — con Maven (recomendada para desarrollo)

```bash
mvn javafx:run
```

### Opción B — jar ejecutable con todas las dependencias

```bash
mvn package
java -jar target/inventario-mercaderia-1.0.0-all.jar
```

### Primer arranque

En la carpeta desde la que se lance el programa se crean automáticamente:

```
config.properties          ← configuración (modo, MySQL, carpeta de datos)
data/                      ← REPOSITORIO JSON
├── productos.json
├── categorias.json
├── proveedores.json
├── movimientos.json
├── usuarios.json
├── codigos_barras/        ← PNG de los códigos generados
└── informes/              ← PDF y Excel exportados
```

Se cargan además datos de ejemplo (10 productos, 5 categorías, 3 proveedores) y dos usuarios:

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | Administrador |
| `operario` | `operario123` | Operario |

> Cambia estas contraseñas en cuanto despliegues el sistema.

---

## 4. Migración a MySQL

1. **Instala y arranca MySQL 8** (`localhost:3306` por defecto).
2. Ajusta `config.properties` si tus credenciales no son `root` / `root`:

   ```properties
   modo=json
   carpetaDatos=data
   mysql.url=jdbc:mysql://localhost:3306/inventario?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
   mysql.usuario=root
   mysql.password=tu_password
   ```

3. Entra con un usuario **administrador** y pulsa **«Migrar JSON → MySQL»** en el menú lateral.

   La aplicación se encarga de todo:
   * crea la base de datos `inventario` si no existe (`CREATE DATABASE IF NOT EXISTS`);
   * crea las tablas (`src/main/resources/db/schema.sql`);
   * copia categorías → proveedores → usuarios → productos → movimientos **conservando los IDs**;
   * verifica que los contados coinciden antes de cambiar de modo;
   * guarda `modo=mysql` en `config.properties`.

4. Si algo falla, **no se cambia el modo**: la aplicación sigue en JSON y los ficheros originales
   quedan intactos (son a su vez la copia de seguridad).

5. El botón pasa a **«Volver a modo JSON»**, por si necesitas consultar de nuevo los ficheros
   (aviso: los ficheros no recogen los cambios hechos durante la etapa en MySQL).

**Nota sobre los IDs:** las tablas usan IDs numéricos generados por la aplicación (sin
`AUTO_INCREMENT`) precisamente para que la migración sea 1:1. Si en MySQL insertas filas a mano
sin ID, usa siempre un `id` libre.

---

## 5. Arquitectura

```
src/main/java/com/inventario/
├── Aplicacion.java            Punto de entrada JavaFX (arranque, navegación)
├── Lanzador.java              main para "java -jar"
├── AppContexto.java            Contenedor de configuración y servicios
├── config/ConfigApp.java       Lectura/escritura de config.properties
├── modelo/                     Entidades: Producto, Categoria, Proveedor,
│                               Movimiento, Usuario, TipoMovimiento, RolUsuario
├── repositorio/                PATRÓN REPOSITORIO
│   ├── BaseJsonRepo.java       Escritura atómica sobre un fichero JSON
│   ├── *RepoJson.java          Implementación fichero  (data/*.json)
│   ├── *RepoMysql.java         Implementación JDBC     (MySQL)
│   └── FactoriaRepositorios.java  Entrega el juego activo (json o mysql)
├── servicio/
│   ├── ServicioInventario.java    CRUD + lógica de stock
│   ├── ServicioAutenticacion.java Login y usuarios
│   ├── ServicioCodigoBarras.java  EAN-13 / Code-128 (ZXing)
│   ├── ServicioReportes.java      PDF y Excel
│   └── ServicioMigracion.java     JSON → MySQL
├── seguridad/Sesion.java       Usuario con sesión abierta
├── db/ConexionMySQL.java       Conexión, creación de BD y esquema
├── datos/DatosSemilla.java     Datos de ejemplo del primer arranque
├── excepcion/ExcepcionNegocio.java
├── util/                       UtilHash (SHA-256), UtilFechas, UtilTextos
└── ui/                         Controladores + diálogos (JavaFX)
    └── Icono.java              Iconos SVG reutilizables desde los FXML
```

`src/main/resources/`
```
fxml/        login, principal, resumen, productos, categorias,
             proveedores, stock, reportes, usuarios
styles.css   Hoja de estilos completa
config/      config.properties por defecto
db/          schema.sql de MySQL
```

### La idea clave: un único contrato, dos almacenes

Los controladores y los servicios **nunca** saben de dónde vienen los datos: siempre hablan con la
interfaz (`ProductoRepo`, `CategoriaRepo`, …) y la `FactoriaRepositorios` entrega la implementación
JSON o MySQL según `config.properties`. Por eso migrar no obliga a reiniciar ni a tocar la interfaz.

---

## 6. Reglas de negocio

* El **código de barras es único**; si es un EAN-13 se valida su dígito de control.
* El **stock solo cambia** mediante movimientos: editar un producto no altera las unidades.
* Movimientos:
  * **Entrada** → suma (compras, devoluciones).
  * **Salida** → resta y **se rechaza** si no hay stock suficiente.
  * **Ajuste** → fija el stock al **valor realmente contado** (inventario físico); el movimiento
    guarda la diferencia.
* Un producto con historial **no se borra**: se da de baja (conserva los movimientos).
* Solo un **administrador** gestiona usuarios y la migración a MySQL.
* Debe existir siempre al menos un administrador activo.

---

## 7. Solución de problemas

| Problema | Solución |
|---|---|
| *«No se pudo conectar con MySQL»* | Revisa `mysql.url`, `mysql.usuario` y `mysql.password`; comprueba que el servidor está arrancado (puerto 3306). |
| *Acceso denegado al crear la base de datos* | Otorga permisos al usuario o créala a mano: `CREATE DATABASE inventario CHARACTER SET utf8mb4;` |
| *Aviso `Unsupported JavaFX configuration: classes were loaded from 'unnamed module'`* | Es solo un aviso: la app funciona desde el classpath. Para eliminarlo, empaca la app como módulo JPMS. |
| *Aviso de Log4j de Apache POI* | Cosmético; POI solo escribe los mensajes de aviso en consola. |
| *«JavaFX runtime components are missing»* | Usar `com.inventario.Lanzador` como clase principal (ya es el `Main-Class` del jar empaquetado) o ejecutar `mvn javafx:run`. |
| El programa no encuentra los datos | Se ejecuta en la carpeta correcta: los ficheros `data/` se crean junto al sitio desde donde lanzas el comando. |

---

## 8. Pruebas

La verificación automática (carga de las 9 vistas FXML, CRUD, movimientos con control de stock,
generación de código de barras, los 7 informes PDF/Excel y la migración completa a una base de datos
en modo MySQL) se ejecutó con éxito sobre este código.
