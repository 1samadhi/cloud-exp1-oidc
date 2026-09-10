# Presentacion — Evaluacion Parcial N.º 2

Dos formatos del mismo mazo: 14 laminas en 16:9 mas una final de apoyo para el
presentador, que **no se proyecta**.

| Archivo | Para que |
|---|---|
| `Pedidos360-presentacion.pptx` | Editable en PowerPoint, LibreOffice o Google Slides |
| `Pedidos360-presentacion.pdf`  | Proyectar sin depender del software instalado |

Las laminas marcadas **DEMOSTRACION** son las que exige la pauta mostrar en vivo
en la plataforma cloud. La lamina es solo el encabezado: lo que se muestra es la
consola o la aplicacion.

| Lamina | Indicador de la pauta | Peso |
|--------|------------------------|------|
| 4, 5   | Tenant en el IDaaS con usuarios, y aplicacion registrada | 10% + 10% |
| 6      | El frontend inicia sesion con OIDC y consume la API por el gateway | 10% |
| 7      | Authorization Code con PKCE | 15% |
| 8      | Instancia del API Manager y su configuracion hacia el backend | 13% |
| 9      | CORS | 7% |
| 10     | El API Manager valida JWT: acepta y rechaza | 20% |
| 10, 13 | Evidencia del funcionamiento de cada ruta | 15% |
| 12     | Backend y frontend desplegados, activos e integrados | — |

## Regenerar el PPTX

```bash
python3 -m venv /tmp/pptx && /tmp/pptx/bin/pip install python-pptx
/tmp/pptx/bin/python construir-pptx.py
```

Las laminas se construyen nativas y no convertidas desde el PDF, asi que el
texto queda editable. `construir-pptx.py` interpreta `**negrita**`, `` `codigo` ``
y `~cursiva~`; la cursiva no usa guion bajo a proposito, porque identificadores
como `code_challenge` lo llevan dentro.

`diagrama.png` se obtiene del SVG con:

```bash
chromium --headless --screenshot=diagrama.png --window-size=1440,700 \
  --hide-scrollbars file://$PWD/../informe/diagrama.svg
```

## Regenerar el PDF

```bash
cd docs/presentacion
python3 - <<'PY'
import pathlib
html = pathlib.Path('presentacion.html').read_text()
html = html.replace('DIAGRAMA', pathlib.Path('../informe/diagrama.svg').read_text())
html = html.replace('<link rel="stylesheet" href="estilo.css">',
                    '<style>' + pathlib.Path('estilo.css').read_text() + '</style>')
pathlib.Path('armado.html').write_text(html)
PY

chromium --headless --disable-gpu --no-sandbox \
  --print-to-pdf="$PWD/Pedidos360-presentacion.pdf" \
  --no-pdf-header-footer "file://$PWD/armado.html"
rm armado.html
```

El diagrama es el mismo de `docs/informe/`, asi que ambos documentos no se pueden
contradecir.

## Antes de presentar

El laboratorio de AWS asigna una IP publica nueva en cada arranque, y el API
Gateway guarda esa direccion en sus integraciones:

```bash
./scripts/actualizar-api-gateway.sh j37oj1wn16 i-0314ddd12fadeb125
IP_EC2=<ip> ./scripts/probar-endpoints.sh <stage>
```

Sin ese primer comando la demostracion en vivo responde 503.
