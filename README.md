# Registro Medico 👵👴

**Registro Medico** es una aplicación móvil para Android diseñada para coordinar las labores de cuidado y acompañamiento de adultos mayores (padres o abuelos) dentro del núcleo familiar. La aplicación permite organizar turnos, llevar una bitácora médica compartida en tiempo real y gestionar la información de la familia sin necesidad de servidores externos.

---

## 🚀 Arquitectura y Tecnologías

El proyecto sigue estrictamente las reglas oficiales del stack tecnológico y arquitectura limpia:

- **Lenguaje:** Kotlin
- **Interfaz de Usuario (UI):** Jetpack Compose, Material 3 (con colores personalizados del sistema *Bento/Avena*).
- **Arquitectura:** Clean Architecture estructurada en capas (`data`, `domain`, `presentation`, `core`, `services`, `di`) con MVVM (Model-View-ViewModel), Repository Pattern y flujos reactivos basados en `StateFlow` e inmunidad de estados de UI.
- **Base de Datos y Backend:** Firebase (Firestore como fuente de verdad en tiempo real).
- **Persistencia Local:** Jetpack DataStore para preferencias y sesión activa.

---

## 🌟 Características Principales

### 1. Registro e Ingreso Sencillo
- Creación de una "Familia" o unión a una existente mediante un código único de la Familia.
- Autenticación rápida por PIN de seguridad personalizado para cada miembro.

### 2. Mi Turno (Labores Activas)
- Panel personalizado que muestra las tareas de cuidado asignadas o sugeridas al usuario actual.
- Opción de confirmar el turno de forma explícita.
- Entrega de testigo (cierre de turno) ingresando notas médicas y observaciones sobre el estado del paciente.

### 3. Agenda de Cuidado (Pista)
- Cronograma en tiempo real de todos los eventos programados (Citas Médicas, Exámenes, Recogida de Medicamentos, etc.).
- Filtrado inteligente: los turnos completados o cancelados se ocultan automáticamente de la agenda activa.
- Regla de negocio: los turnos ya confirmados por un hermano no pueden ser editados ni eliminados.

### 4. Bitácora Médica (Testigo)
- Feed social e informativo de todos los reportes médicos registrados al cerrar cada turno.
- Integración nativa para compartir el reporte médico completo en WhatsApp con un solo toque.

### 5. Historial de Cuidado (Archivo)
- Archivo histórico de todos los turnos completados o cancelados para consulta futura.

### 6. Ajustes y Preferencias (Ajustes)
- Visualización de datos de perfil y rol (Admin vs. Hermano).
- Persistencia del modo oscuro / modo claro de forma reactiva a nivel de aplicación mediante DataStore.
- **Configuración Familiar (Admin):** Panel deslizante nativo (`ModalBottomSheet`) para editar los nombres de los adultos mayores y agregar o eliminar miembros/cuidadores de la familia dinámicamente.

---

## 📁 Estructura del Proyecto

El código está organizado siguiendo la estructura obligatoria de módulos de Clean Architecture:

```text
app/
 └── src/main/java/com/xd/misviejos/
      ├── core/
      │    ├── datastore/      # Manejo de sesión y modo oscuro (DataStore)
      │    ├── designsystem/   # Definición de temas, tipografías y colores (Bento)
      │    └── navigation/     # Definiciones de rutas y pestañas (TabNav)
      ├── data/
      │    └── repository/     # Implementaciones de repositorios con Firestore
      ├── domain/
      │    ├── model/          # Modelos de dominio inmutables (Turno, Familia, Usuario)
      │    └── repository/     # Interfaces de los repositorios
      ├── feature/
      │    ├── onboarding/     # UI y lógica de ingreso y registro
      │    └── timeline/       # Pantallas principales (MiTurno, Agenda, Bitácora, Ajustes)
      └── MainActivity.kt      # Chasis y controlador principal de navegación
```

---

## 🛠️ Instalación y Configuración

1. **Requisitos Previos:**
   - Android Studio Koala o superior.
   - Java 17 / Gradle 8+.
   - Un proyecto de Firebase configurado.

2. **Configuración de Firebase:**
   - Descarga el archivo `google-services.json` desde la consola de Firebase.
   - Coloca el archivo en la carpeta `app/` del proyecto.
   - Asegúrate de habilitar **Cloud Firestore** en tu consola de Firebase.

3. **Compilación y Ejecución:**
   - Sincroniza el proyecto con Gradle.
   - Ejecuta la aplicación en un emulador o dispositivo real ejecutando:
     ```bash
     ./gradlew assembleDebug
     ```
