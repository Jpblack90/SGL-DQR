/**
 * Lee el comprobante desde la dirección (`/rastreo?codigo=MV-...`), que es lo que
 * codifica el QR impreso en el ticket del cliente. Solo admite letras, números y
 * guiones; el documento de identidad nunca viaja en la dirección.
 */
export const leerComprobanteDeUrl = (busqueda: string): string => {
  const valor = new URLSearchParams(busqueda).get('codigo') ?? '';
  const limpio = valor.trim().toUpperCase();
  return /^[A-Z0-9-]{1,40}$/.test(limpio) ? limpio : '';
};
