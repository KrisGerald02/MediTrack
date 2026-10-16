<h1 align="center">💊 MediTrack</h1>
<p align="center">
  <strong>Aplicación móvil para organizar tratamientos, recordatorios e inventario de medicamentos.</strong><br />
  Tus dosis, horarios y progreso en un solo lugar. 🩷
</p>

---

<a id="proyecto"></a>
## 🌸 Sobre el proyecto

**MediTrack** es una aplicación Android que ayuda a pacientes, deportistas y usuarios generales a organizar medicinas y suplementos. Permite registrar tratamientos, seleccionar horarios, recibir recordatorios insistentes y controlar las existencias disponibles.

El proyecto combina **salud, organización y accesibilidad** con una interfaz adaptable, soporte para texto grande y diferentes paletas de colores. Toda la información se conserva localmente para que la aplicación funcione sin conexión.

<a id="funciones"></a>
## 🩺 ¿Qué puedes encontrar?

- **Plan diario:** tomas de hoy y atrasadas por menos de 24 horas, con bloqueo anticipado y estado “No tomada” al vencer.
- **Tratamientos personalizados:** medicinas o suplementos, cantidad de pastillas por toma, horas específicas o frecuencia como “cada 4 horas”, e instrucciones.
- **Duración controlada:** tratamientos permanentes o por cantidad de días, con fecha inicial, fecha final calculada y suspensión automática al concluir.
- **Recordatorios persistentes:** cada tratamiento permite configurar cuántos minutos deben pasar antes de repetir la notificación mientras la dosis continúe pendiente.
- **Inventario inteligente:** existencias por tratamiento, reposición acumulativa, pausa de alarmas al llegar a cero y registro de períodos sin stock.
- **Alertas de stock:** avisos de nivel medio, bajo o agotado que pueden marcarse como recibidos.
- **Actividad e historial:** dashboard mensual, tomas realizadas, pospuestas y no tomadas, además de períodos sin existencias.
- **Perfil personalizable:** saludo según la hora, nombre del usuario y selección de uso durante el onboarding.
- **Temas de color:** siete paletas generales y siete colores independientes para identificar tratamientos.
- **Tratamientos inactivos:** cancelar alarmas sin borrar el tratamiento ni su historial, con vistas separadas de activos e inactivos.
- **Diseño adaptable:** navegación optimizada para celulares y pantallas amplias, compatible con tamaños de letra grandes.

<a id="herramientas"></a>
<details open>
  <summary><h2>🛠️ Herramientas</h2></summary>

<h3>📱 Desarrollo Android</h3>
<p>
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Room-3DDC84?logo=android&logoColor=white" alt="Room" />
  <img src="https://img.shields.io/badge/Material%203-6750A4?logo=materialdesign&logoColor=white" alt="Material 3" />
</p>

<h3>🏗️ Arquitectura</h3>

```text
app/src/main/java/com/example/meditrack/
├── core/
│   ├── data/           # Room, entidades, DAO y repositorio
│   ├── navigation/     # Navegación y destinos
│   ├── notifications/  # Alarmas, notificaciones y receivers
│   ├── presentation/   # ViewModel compartido
│   └── ui/             # Tema y componentes reutilizables
└── feature/
    ├── activity/       # Dashboard e historial
    ├── alerts/         # Alertas de stock
    ├── home/           # Plan de hoy
    ├── inventory/      # Control de existencias
    ├── medications/    # Tratamientos y formulario
    ├── onboarding/     # Configuración inicial
    └── settings/       # Perfil y temas
```
</details>

<a id="ejecutar"></a>
## 🚀 Ejecutar el proyecto

1. Clona el repositorio y ábrelo en Android Studio.
2. Espera a que finalice la sincronización de Gradle.
3. Ejecuta el módulo `app` en Android 7.0 o posterior.
4. En Android 13 o posterior, acepta el permiso de notificaciones.

Para generar el APK desde PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

<a id="equipo"></a>
## 🩷 Desarrollo

| Integrante | Rol |
| --- | --- |
| **KrisGerald02** | Desarrollo y diseño de la aplicación |

---

<p align="center">
  <strong>MediTrack · Cuida tu rutina</strong><br />
  💊 Cada dosis cuenta. 🩷
</p>
