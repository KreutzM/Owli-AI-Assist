# Model Assets (TFLite) – Download & Pflege

Dieses Repo erwartet das TFLite-Modell **lokal** unter:

- `app/src/main/assets/models/efficientdet_lite2_int8.tflite`
- Labels: `app/src/main/assets/models/labels.txt`

> Hinweis: Das Modell ist typischerweise **nicht** im Git eingecheckt (Größe/Lizenz).

## Download

### Windows (PowerShell)
```powershell
New-Item -ItemType Directory -Force -Path "app\src\main\assets\models" | Out-Null
Invoke-WebRequest -Uri "https://tfhub.dev/tensorflow/lite-model/efficientdet/lite2/detection/metadata/1?lite-format=tflite" `
  -OutFile "app\src\main\assets\models\efficientdet_lite2_int8.tflite"
```

### WSL / Git Bash
```bash
./getModel.sh
```

## Verifikation (schnell)
- App starten → Statusanzeige muss **RealDetector** (nicht FakeDetector) zeigen.
- Diagnostics: Detektionsanzahl plausibel > 0 in realen Szenen.

## Modellwechsel
Wenn ein anderes Modell genutzt werden soll:
- Pfad/Name konsistent halten oder Code/Settings entsprechend anpassen
- Labels (`labels.txt`) müssen zum Modell passen
- Änderungen dokumentieren (README + ChangeLog)
