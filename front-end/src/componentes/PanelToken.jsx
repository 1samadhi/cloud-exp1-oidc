import { leerClaims } from '../servicios/api'

/** Muestra el token vigente y sus claims decodificados, para la evidencia. */
export function PanelToken({ token, origen }) {
  if (!token) return null
  const claims = leerClaims(token)

  return (
    <section className="tarjeta">
      <h3>Token vigente</h3>
      <p className="nota">Emitido por: <strong>{origen}</strong></p>
      <textarea readOnly rows={4} value={token} />
      {claims && (
        <>
          <h4>Claims</h4>
          <pre>{JSON.stringify(claims, null, 2)}</pre>
        </>
      )}
    </section>
  )
}
