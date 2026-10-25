import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import QRCode from 'qrcode';
import { cotizarTarifa, crearPreRegistro, listarSucursales } from '../api/cliente';
import {
  ApiError,
  CrearPreRegistroPublicoRequest,
  RespuestaPreRegistroPublicoDto,
  SucursalPublicaDto,
  TipoDocumento,
} from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Cargando } from '../componentes/Cargando';

interface LocationStatePreRegistro {
  idSucursalOrigen?: string;
  idSucursalDestino?: string;
  pesoEstimado?: number;
  volumenM3?: number;
  montoCotizado?: number;
}

function formatearFechaHora(isoString: string): string {
  try {
    const f = new Date(isoString);
    if (isNaN(f.getTime())) return isoString;
    return new Intl.DateTimeFormat('es-PE', {
      dateStyle: 'full',
      timeStyle: 'short',
    }).format(f);
  } catch {
    return isoString;
  }
}

export const PreRegistro: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const stateInicial = (location.state as LocationStatePreRegistro) || {};

  // Paso actual: 1 = Envío, 2 = Personas, 3 = Confirmación, 4 = Éxito
  const [paso, setPaso] = useState<number>(1);

  // Catálogo de agencias
  const [sucursales, setSucursales] = useState<SucursalPublicaDto[]>([]);
  const [cargandoSucursales, setCargandoSucursales] = useState<boolean>(true);

  // Paso 1: Envío
  const [idOrigen, setIdOrigen] = useState<string>(stateInicial.idSucursalOrigen || '');
  const [idDestino, setIdDestino] = useState<string>(stateInicial.idSucursalDestino || '');
  const [pesoEstimado, setPesoEstimado] = useState<string>(
    stateInicial.pesoEstimado ? String(stateInicial.pesoEstimado) : ''
  );
  const [montoCotizado, setMontoCotizado] = useState<number | null>(
    stateInicial.montoCotizado ?? null
  );
  const [cargandoCotizacion, setCargandoCotizacion] = useState<boolean>(false);

  // Paso 2: Remitente
  const [remTipoDoc, setRemTipoDoc] = useState<TipoDocumento>('DNI');
  const [remNumDoc, setRemNumDoc] = useState<string>('');
  const [remNombres, setRemNombres] = useState<string>('');
  const [remTelefono, setRemTelefono] = useState<string>('');
  const [remCorreo, setRemCorreo] = useState<string>('');

  // Paso 2: Destinatario
  const [destTipoDoc, setDestTipoDoc] = useState<TipoDocumento>('DNI');
  const [destNumDoc, setDestNumDoc] = useState<string>('');
  const [destNombres, setDestNombres] = useState<string>('');
  const [destTelefono, setDestTelefono] = useState<string>('');
  const [destCorreo, setDestCorreo] = useState<string>('');

  // Paso 3: Aceptación
  const [aceptoTerminos, setAceptoTerminos] = useState<boolean>(false);

  // Errores por campo (locales y backend)
  const [erroresCampos, setErroresCampos] = useState<Record<string, string>>({});
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const [reintentoSegundos, setReintentoSegundos] = useState<number | null>(null);
  const [enviando, setEnviando] = useState<boolean>(false);

  // Paso 4: Éxito
  const [resultado, setResultado] = useState<RespuestaPreRegistroPublicoDto | null>(null);
  const [qrDataUrl, setQrDataUrl] = useState<string>('');
  const [copiado, setCopiado] = useState<boolean>(false);

  // Cargar catálogo de sucursales
  useEffect(() => {
    document.title = 'Código Express · Expreso Marvisur';
    let activo = true;
    async function cargar() {
      setCargandoSucursales(true);
      try {
        const datos = await listarSucursales();
        if (activo) {
          setSucursales(datos);
          setIdDestino((prev) => prev || (datos.length > 0 ? datos[0].id : ''));
          setIdOrigen((prev) => prev || (datos.length > 1 ? datos[1].id : ''));
        }
      } catch (err) {
        if (activo) {
          setErrorGeneral(err instanceof ApiError ? err.message : 'Error al cargar agencias.');
        }
      } finally {
        if (activo) setCargandoSucursales(false);
      }
    }
    cargar();
    return () => {
      activo = false;
    };
  }, []);

  // Generar QR en base64 cuando se genera el código express
  useEffect(() => {
    if (resultado?.codigoExpress) {
      QRCode.toDataURL(resultado.codigoExpress, {
        width: 240,
        margin: 2,
        color: {
          dark: '#0F172A',
          light: '#FFFFFF',
        },
      })
        .then((url) => setQrDataUrl(url))
        .catch((err) => {
          console.error('Error al generar código QR del pre-registro:', err);
        });
    }
  }, [resultado?.codigoExpress]);

  // Cotizar en vivo en el Paso 1 si hay peso y destino
  const handleCotizarEnVivo = async () => {
    const pNum = parseFloat(pesoEstimado);
    if (!idDestino || isNaN(pNum) || pNum <= 0) return;

    setCargandoCotizacion(true);
    try {
      const resp = await cotizarTarifa({
        idSucursalOrigen: idOrigen || idDestino,
        idSucursalDestino: idDestino,
        pesoKg: pNum,
      });
      setMontoCotizado(resp.montoEstimado);
    } catch {
      // Si falla la cotización estimada en vivo, no bloquea el flujo
    } finally {
      setCargandoCotizacion(false);
    }
  };

  // Validaciones del Paso 1
  const validarPaso1 = (): boolean => {
    const errs: Record<string, string> = {};
    if (!idDestino.trim()) {
      errs.idSucursalDestino = 'Selecciona la agencia de destino.';
    }
    const pNum = parseFloat(pesoEstimado);
    if (!pesoEstimado.trim()) {
      errs.pesoEstimado = 'Ingresa el peso estimado del paquete.';
    } else if (isNaN(pNum) || pNum <= 0) {
      errs.pesoEstimado = 'El peso debe ser mayor a 0 kg.';
    }
    setErroresCampos(errs);
    return Object.keys(errs).length === 0;
  };

  // Validaciones del Paso 2
  const validarPaso2 = (): boolean => {
    const errs: Record<string, string> = {};

    // Remitente
    const remNum = remNumDoc.trim();
    if (!remNum) {
      errs['remitente.numeroDocumento'] = 'El documento del remitente es obligatorio.';
    } else if (remTipoDoc === 'DNI' && !/^\d{8}$/.test(remNum)) {
      errs['remitente.numeroDocumento'] = 'El DNI debe tener exactamente 8 dígitos.';
    } else if (remTipoDoc === 'RUC' && !/^\d{11}$/.test(remNum)) {
      errs['remitente.numeroDocumento'] = 'El RUC debe tener exactamente 11 dígitos.';
    }

    if (!remNombres.trim() || remNombres.trim().length < 3) {
      errs['remitente.nombresRazonSocial'] = 'Ingresa los nombres y apellidos completos o razón social.';
    }

    const remTel = remTelefono.trim();
    if (!remTel) {
      errs['remitente.telefono'] = 'El teléfono de contacto es obligatorio.';
    } else if (!/^\d{9}$/.test(remTel)) {
      errs['remitente.telefono'] = 'El teléfono debe tener 9 dígitos.';
    }

    if (remCorreo.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(remCorreo.trim())) {
      errs['remitente.correo'] = 'Ingresa un correo electrónico válido.';
    }

    // Destinatario
    const destNum = destNumDoc.trim();
    if (!destNum) {
      errs['destinatario.numeroDocumento'] = 'El documento del destinatario es obligatorio.';
    } else if (destTipoDoc === 'DNI' && !/^\d{8}$/.test(destNum)) {
      errs['destinatario.numeroDocumento'] = 'El DNI debe tener exactamente 8 dígitos.';
    } else if (destTipoDoc === 'RUC' && !/^\d{11}$/.test(destNum)) {
      errs['destinatario.numeroDocumento'] = 'El RUC debe tener exactamente 11 dígitos.';
    }

    if (!destNombres.trim() || destNombres.trim().length < 3) {
      errs['destinatario.nombresRazonSocial'] = 'Ingresa los nombres y apellidos del destinatario.';
    }

    const destTel = destTelefono.trim();
    if (!destTel) {
      errs['destinatario.telefono'] = 'El teléfono del destinatario es obligatorio.';
    } else if (!/^\d{9}$/.test(destTel)) {
      errs['destinatario.telefono'] = 'El teléfono debe tener 9 dígitos.';
    }

    if (destCorreo.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(destCorreo.trim())) {
      errs['destinatario.correo'] = 'Ingresa un correo electrónico válido.';
    }

    setErroresCampos(errs);
    return Object.keys(errs).length === 0;
  };

  // Validaciones del Paso 3 y envío
  const handleConfirmarYEnviar = async () => {
    if (!aceptoTerminos) {
      setErroresCampos({
        terminos: 'Debes aceptar el uso de datos personales para procesar el pre-registro.',
      });
      return;
    }

    setEnviando(true);
    setErrorGeneral(null);
    setErroresCampos({});
    setReintentoSegundos(null);

    const payload: CrearPreRegistroPublicoRequest = {
      remitente: {
        tipoDocumento: remTipoDoc,
        numeroDocumento: remNumDoc.trim(),
        nombresRazonSocial: remNombres.trim(),
        telefono: remTelefono.trim(),
        correo: remCorreo.trim() || undefined,
      },
      destinatario: {
        tipoDocumento: destTipoDoc,
        numeroDocumento: destNumDoc.trim(),
        nombresRazonSocial: destNombres.trim(),
        telefono: destTelefono.trim(),
        correo: destCorreo.trim() || undefined,
      },
      idSucursalDestino: idDestino,
      pesoEstimado: parseFloat(pesoEstimado),
    };

    try {
      const resp = await crearPreRegistro(payload);
      setResultado(resp);
      setPaso(4);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorGeneral(err.message);
        if (err.campos) {
          setErroresCampos(err.campos);
          // Si hay errores de campos específicos del paso 2, regresar al paso 2
          const hayErroresPaso2 = Object.keys(err.campos).some(
            (k) => k.startsWith('remitente') || k.startsWith('destinatario')
          );
          if (hayErroresPaso2) {
            setPaso(2);
          }
        }
        if (err.status === 429 && err.retryAfterSeconds) {
          setReintentoSegundos(err.retryAfterSeconds);
        }
      } else {
        setErrorGeneral('Ocurrió un error inesperado al procesar el pre-registro.');
      }
    } finally {
      setEnviando(false);
    }
  };

  const copiarCodigo = async () => {
    if (!resultado?.codigoExpress) return;
    try {
      await navigator.clipboard.writeText(resultado.codigoExpress);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 3000);
    } catch {
      // Fallback manual si clipboard API no está permitida
    }
  };

  const compartirWhatsApp = () => {
    if (!resultado) return;
    // Texto estricto: solo código y vigencia, CERO datos personales
    const vigenciaTexto = formatearFechaHora(resultado.fechaExpiracion);
    const mensaje = encodeURIComponent(
      `Mi Código Express de Expreso Marvisur es: ${resultado.codigoExpress}\nVálido hasta: ${vigenciaTexto}\nPreséntalo en la ventanilla de la agencia para acelerar el depósito de tu encomienda.`
    );
    window.open(`https://wa.me/?text=${mensaje}`, '_blank', 'noopener,noreferrer');
  };

  const nombreDestinoSeleccionado =
    sucursales.find((s) => s.id === idDestino)?.nombreAgencia || idDestino;

  return (
    <div className="max-w-3xl mx-auto space-y-8">
      {/* Cabecera general (oculta al imprimir) */}
      <div className="text-center space-y-2 print:hidden">
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-rojo-claro text-rojo">
          Fricción Cero en Ventanilla
        </span>
        <h1 className="text-2xl sm:text-4xl font-extrabold text-azul-noche tracking-tight">
          Pre-registro «Código Express»
        </h1>
        <p className="text-sm sm:text-base text-slate-600 max-w-xl mx-auto">
          Genera tu código digital anticipado para depositar tu encomienda en ventanilla sin dictar datos personales.
        </p>
      </div>

      {/* Indicador de 3 pasos (oculto en pantalla de éxito y al imprimir) */}
      {paso < 4 && (
        <div className="print:hidden">
          <div className="flex items-center justify-between max-w-md mx-auto px-4">
            {/* Paso 1 */}
            <div className="flex flex-col items-center">
              <div
                className={`w-9 h-9 rounded-full flex items-center justify-center font-bold text-sm transition-colors ${
                  paso === 1
                    ? 'bg-rojo text-white shadow-md shadow-rojo/30 ring-4 ring-rojo/20'
                    : paso > 1
                    ? 'bg-emerald-500 text-white'
                    : 'bg-slate-200 text-slate-600'
                }`}
              >
                {paso > 1 ? '✓' : '1'}
              </div>
              <span className="text-xs font-semibold text-slate-600 mt-1">Envío</span>
            </div>

            <div
              className={`flex-1 h-1 mx-2 rounded-full transition-colors ${
                paso > 1 ? 'bg-emerald-500' : 'bg-slate-200'
              }`}
            />

            {/* Paso 2 */}
            <div className="flex flex-col items-center">
              <div
                className={`w-9 h-9 rounded-full flex items-center justify-center font-bold text-sm transition-colors ${
                  paso === 2
                    ? 'bg-rojo text-white shadow-md shadow-rojo/30 ring-4 ring-rojo/20'
                    : paso > 2
                    ? 'bg-emerald-500 text-white'
                    : 'bg-slate-200 text-slate-600'
                }`}
              >
                {paso > 2 ? '✓' : '2'}
              </div>
              <span className="text-xs font-semibold text-slate-600 mt-1">Personas</span>
            </div>

            <div
              className={`flex-1 h-1 mx-2 rounded-full transition-colors ${
                paso > 2 ? 'bg-emerald-500' : 'bg-slate-200'
              }`}
            />

            {/* Paso 3 */}
            <div className="flex flex-col items-center">
              <div
                className={`w-9 h-9 rounded-full flex items-center justify-center font-bold text-sm transition-colors ${
                  paso === 3
                    ? 'bg-rojo text-white shadow-md shadow-rojo/30 ring-4 ring-rojo/20'
                    : 'bg-slate-200 text-slate-600'
                }`}
              >
                3
              </div>
              <span className="text-xs font-semibold text-slate-600 mt-1">Confirmación</span>
            </div>
          </div>
        </div>
      )}

      {/* Alerta de Error General de API */}
      {errorGeneral && (
        <Alerta
          tipo={reintentoSegundos ? 'advertencia' : 'error'}
          titulo={reintentoSegundos ? 'Límite de Consultas' : 'No se pudo procesar el pre-registro'}
          mensaje={errorGeneral}
        />
      )}

      {/* ===================================================================== */}
      {/* PASO 1: DATOS DEL ENVÍO                                               */}
      {/* ===================================================================== */}
      {paso === 1 && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 sm:p-8 space-y-6">
          <h2 className="text-lg font-bold text-azul-noche flex items-center gap-2">
            <span className="w-6 h-6 rounded-md bg-rojo-claro text-rojo flex items-center justify-center text-xs">
              1
            </span>
            Destino y Peso del Paquete
          </h2>

          {cargandoSucursales ? (
            <Cargando texto="Cargando agencias autorizadas..." />
          ) : (
            <div className="space-y-4">
              <div>
                <label htmlFor="idDestino" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
                  Agencia de Destino <span className="text-rojo">*</span>
                </label>
                <select
                  id="idDestino"
                  value={idDestino}
                  onChange={(e) => {
                    setIdDestino(e.target.value);
                    setMontoCotizado(null);
                  }}
                  className={`w-full rounded-xl border px-3.5 py-3 text-sm text-azul-noche bg-white focus:outline-none focus:ring-2 focus:ring-rojo/30 ${
                    erroresCampos.idSucursalDestino ? 'border-rojo bg-rojo-claro/20' : 'border-slate-300'
                  }`}
                >
                  <option value="">Selecciona la agencia donde se recogerá...</option>
                  {sucursales.map((s) => (
                    <option key={`dest-${s.id}`} value={s.id}>
                      {s.nombreAgencia} — {s.ciudad} ({s.direccion})
                    </option>
                  ))}
                </select>
                {erroresCampos.idSucursalDestino && (
                  <p className="mt-1 text-xs text-rojo font-medium">{erroresCampos.idSucursalDestino}</p>
                )}
              </div>

              <div>
                <label htmlFor="pesoEstimado" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
                  Peso Estimado (kg) <span className="text-rojo">*</span>
                </label>
                <div className="relative">
                  <input
                    id="pesoEstimado"
                    type="number"
                    step="0.1"
                    min="0.1"
                    placeholder="Ej. 5.0"
                    value={pesoEstimado}
                    onChange={(e) => {
                      setPesoEstimado(e.target.value);
                      setMontoCotizado(null);
                    }}
                    className={`w-full rounded-xl border px-3.5 py-3 text-sm text-azul-noche placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-rojo/30 ${
                      erroresCampos.pesoEstimado ? 'border-rojo bg-rojo-claro/20' : 'border-slate-300'
                    }`}
                  />
                  <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400 pointer-events-none">
                    kg
                  </span>
                </div>
                {erroresCampos.pesoEstimado && (
                  <p className="mt-1 text-xs text-rojo font-medium">{erroresCampos.pesoEstimado}</p>
                )}
              </div>

              {/* Cotización en vivo */}
              <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                <div>
                  <span className="text-xs uppercase font-bold text-slate-500 block">
                    Cotización Estimada
                  </span>
                  {cargandoCotizacion ? (
                    <span className="text-sm font-semibold text-slate-600">Calculando...</span>
                  ) : montoCotizado !== null ? (
                    <p className="text-xl font-extrabold text-azul-noche">
                      S/ {montoCotizado.toFixed(2)}
                    </p>
                  ) : (
                    <p className="text-xs text-slate-500">
                      Calcula la tarifa referencial para este peso y destino.
                    </p>
                  )}
                </div>
                <button
                  type="button"
                  onClick={handleCotizarEnVivo}
                  disabled={cargandoCotizacion || !pesoEstimado}
                  className="px-4 py-2 rounded-lg text-xs font-bold text-azul-noche bg-white border border-slate-300 hover:bg-slate-100 transition-colors disabled:opacity-50"
                >
                  Recalcular Tarifa
                </button>
              </div>

              <div className="pt-4 flex justify-end">
                <button
                  type="button"
                  onClick={() => {
                    if (validarPaso1()) {
                      setPaso(2);
                    }
                  }}
                  className="w-full sm:w-auto px-8 py-3.5 rounded-xl font-bold text-white bg-rojo hover:bg-rojo-profundo shadow-md shadow-rojo/20 transition-all focus-visible:outline-rojo"
                >
                  Siguiente: Personas →
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ===================================================================== */}
      {/* PASO 2: REMITENTE Y DESTINATARIO                                      */}
      {/* ===================================================================== */}
      {paso === 2 && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 sm:p-8 space-y-8">
          {/* Sección Remitente */}
          <div className="space-y-4">
            <h2 className="text-lg font-bold text-azul-noche flex items-center gap-2">
              <span className="w-6 h-6 rounded-md bg-rojo-claro text-rojo flex items-center justify-center text-xs">
                2A
              </span>
              Datos del Remitente (Quien Envía)
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label htmlFor="remTipoDoc" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                  Tipo Doc. <span className="text-rojo">*</span>
                </label>
                <select
                  id="remTipoDoc"
                  value={remTipoDoc}
                  onChange={(e) => setRemTipoDoc(e.target.value as TipoDocumento)}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm bg-white"
                >
                  <option value="DNI">DNI (8 dígitos)</option>
                  <option value="RUC">RUC (11 dígitos)</option>
                </select>
              </div>

              <div className="sm:col-span-2">
                <label htmlFor="remNumDoc" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                  Número de Documento <span className="text-rojo">*</span>
                </label>
                <input
                  id="remNumDoc"
                  type="text"
                  maxLength={remTipoDoc === 'DNI' ? 8 : 11}
                  placeholder={remTipoDoc === 'DNI' ? '8 dígitos' : '11 dígitos'}
                  value={remNumDoc}
                  onChange={(e) => setRemNumDoc(e.target.value.replace(/\D/g, ''))}
                  className={`w-full rounded-xl border px-3.5 py-2.5 text-sm text-azul-noche ${
                    erroresCampos['remitente.numeroDocumento'] ? 'border-rojo bg-rojo-claro/20' : 'border-slate-300'
                  }`}
                />
                {erroresCampos['remitente.numeroDocumento'] && (
                  <p className="mt-1 text-xs text-rojo font-medium">
                    {erroresCampos['remitente.numeroDocumento']}
                  </p>
                )}
              </div>
            </div>

            <div>
              <label htmlFor="remNombres" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                Nombres y Apellidos / Razón Social <span className="text-rojo">*</span>
              </label>
              <input
                id="remNombres"
                type="text"
                placeholder="Ej. Juan Pérez Soto"
                value={remNombres}
                onChange={(e) => setRemNombres(e.target.value)}
                className={`w-full rounded-xl border px-3.5 py-2.5 text-sm text-azul-noche ${
                  erroresCampos['remitente.nombresRazonSocial'] ? 'border-rojo bg-rojo-claro/20' : 'border-slate-300'
                }`}
              />
              {erroresCampos['remitente.nombresRazonSocial'] && (
                <p className="mt-1 text-xs text-rojo font-medium">
                  {erroresCampos['remitente.nombresRazonSocial']}
                </p>
              )}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label htmlFor="remTelefono" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                  Teléfono Móvil (9 dígitos) <span className="text-rojo">*</span>
                </label>
                <input
                  id="remTelefono"
                  type="tel"
                  maxLength={9}
                  placeholder="987654321"
                  value={remTelefono}
                  onChange={(e) => setRemTelefono(e.target.value.replace(/\D/g, ''))}
                  className={`w-full rounded-xl border px-3.5 py-2.5 text-sm text-azul-noche ${
                    erroresCampos['remitente.telefono'] ? 'border-rojo bg-rojo-claro/20' : 'border-slate-300'
                  }`}
                />
                {erroresCampos['remitente.telefono'] && (
                  <p className="mt-1 text-xs text-rojo font-medium">
                    {erroresCampos['remitente.telefono']}
                  </p>
                )}
              </div>

              <div>
                <label htmlFor="remCorreo" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                  Correo Electrónico (Opcional)
                </label>
                <input
                  id="remCorreo"
                  type="email"
                  placeholder="correo@ejemplo.com"
                  value={remCorreo}
                  onChange={(e) => setRemCorreo(e.target.value)}
                  className={`w-full rounded-xl border px-3.5 py-2.5 text-sm text-azul-noche ${
                    erroresCampos['remitente.correo'] ? 'border-rojo bg-rojo-claro/20' : 'border-slate-300'
                  }`}
                />
                {erroresCampos['remitente.correo'] && (
                  <p className="mt-1 text-xs text-rojo font-medium">
                    {erroresCampos['remitente.correo']}
                  </p>
                )}
              </div>
            </div>
          </div>

          <hr className="border-slate-200" />

          {/* Sección Destinatario */}
          <div className="space-y-4">
            <h2 className="text-lg font-bold text-azul-noche flex items-center gap-2">
              <span className="w-6 h-6 rounded-md bg-rojo-claro text-rojo flex items-center justify-center text-xs">
                2B
              </span>
              Datos del Destinatario (Quien Recoge)
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label htmlFor="destTipoDoc" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                  Tipo Doc. <span className="text-rojo">*</span>
                </label>
                <select
                  id="destTipoDoc"
                  value={destTipoDoc}
                  onChange={(e) => setDestTipoDoc(e.target.value as TipoDocumento)}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm bg-white"
                >
                  <option value="DNI">DNI (8 dígitos)</option>
                  <option value="RUC">RUC (11 dígitos)</option>
                </select>
              </div>

              <div className="sm:col-span-2">
                <label htmlFor="destNumDoc" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                  Número de Documento <span className="text-rojo">*</span>
                </label>
                <input
                  id="destNumDoc"
                  type="text"
                  maxLength={destTipoDoc === 'DNI' ? 8 : 11}
                  placeholder={destTipoDoc === 'DNI' ? '8 dígitos' : '11 dígitos'}
                  value={destNumDoc}
                  onChange={(e) => setDestNumDoc(e.target.value.replace(/\D/g, ''))}
                  className={`w-full rounded-xl border px-3.5 py-2.5 text-sm text-azul-noche ${
                    erroresCampos['destinatario.numeroDocumento'] ? 'border-rojo bg-rojo-claro/20' : 'border-slate-300'
                  }`}
                />
                {erroresCampos['destinatario.numeroDocumento'] && (
                  <p className="mt-1 text-xs text-rojo font-medium">
                    {erroresCampos['destinatario.numeroDocumento']}
                  </p>
                )}
              </div>
            </div>

            <div>
              <label htmlFor="destNombres" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                Nombres y Apellidos / Razón Social <span className="text-rojo">*</span>
              </label>
              <input
                id="destNombres"
                type="text"
                placeholder="Ej. Ana Lucía Gómez"
                value={destNombres}
                onChange={(e) => setDestNombres(e.target.value)}
                className={`w-full rounded-xl border px-3.5 py-2.5 text-sm text-azul-noche ${
                  erroresCampos['destinatario.nombresRazonSocial'] ? 'border-rojo bg-rojo-claro/20' : 'border-slate-300'
                }`}
              />
              {erroresCampos['destinatario.nombresRazonSocial'] && (
                <p className="mt-1 text-xs text-rojo font-medium">
                  {erroresCampos['destinatario.nombresRazonSocial']}
                </p>
              )}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label htmlFor="destTelefono" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                  Teléfono Móvil (9 dígitos) <span className="text-rojo">*</span>
                </label>
                <input
                  id="destTelefono"
                  type="tel"
                  maxLength={9}
                  placeholder="912345678"
                  value={destTelefono}
                  onChange={(e) => setDestTelefono(e.target.value.replace(/\D/g, ''))}
                  className={`w-full rounded-xl border px-3.5 py-2.5 text-sm text-azul-noche ${
                    erroresCampos['destinatario.telefono'] ? 'border-rojo bg-rojo-claro/20' : 'border-slate-300'
                  }`}
                />
                {erroresCampos['destinatario.telefono'] && (
                  <p className="mt-1 text-xs text-rojo font-medium">
                    {erroresCampos['destinatario.telefono']}
                  </p>
                )}
              </div>

              <div>
                <label htmlFor="destCorreo" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                  Correo Electrónico (Opcional)
                </label>
                <input
                  id="destCorreo"
                  type="email"
                  placeholder="destinatario@ejemplo.com"
                  value={destCorreo}
                  onChange={(e) => setDestCorreo(e.target.value)}
                  className={`w-full rounded-xl border px-3.5 py-2.5 text-sm text-azul-noche ${
                    erroresCampos['destinatario.correo'] ? 'border-rojo bg-rojo-claro/20' : 'border-slate-300'
                  }`}
                />
                {erroresCampos['destinatario.correo'] && (
                  <p className="mt-1 text-xs text-rojo font-medium">
                    {erroresCampos['destinatario.correo']}
                  </p>
                )}
              </div>
            </div>
          </div>

          {/* Botones de navegación del paso 2 */}
          <div className="pt-4 flex flex-col sm:flex-row items-center justify-between gap-3 border-t border-slate-100">
            <button
              type="button"
              onClick={() => setPaso(1)}
              className="w-full sm:w-auto px-6 py-3 rounded-xl font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 transition-colors"
            >
              ← Volver al Envío
            </button>
            <button
              type="button"
              onClick={() => {
                if (validarPaso2()) {
                  setPaso(3);
                }
              }}
              className="w-full sm:w-auto px-8 py-3.5 rounded-xl font-bold text-white bg-rojo hover:bg-rojo-profundo shadow-md shadow-rojo/20 transition-all focus-visible:outline-rojo"
            >
              Revisar y Confirmar →
            </button>
          </div>
        </div>
      )}

      {/* ===================================================================== */}
      {/* PASO 3: CONFIRMACIÓN Y ENVÍO                                          */}
      {/* ===================================================================== */}
      {paso === 3 && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 sm:p-8 space-y-6">
          <h2 className="text-lg font-bold text-azul-noche flex items-center gap-2">
            <span className="w-6 h-6 rounded-md bg-rojo-claro text-rojo flex items-center justify-center text-xs">
              3
            </span>
            Resumen del Pre-registro
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
            {/* Destino y paquete */}
            <div className="bg-slate-50 rounded-xl p-4 space-y-1">
              <span className="text-xs uppercase font-bold text-slate-500 block">Destino y Paquete</span>
              <p className="font-bold text-azul-noche">{nombreDestinoSeleccionado}</p>
              <p className="text-xs text-slate-600">Peso estimado: <strong>{pesoEstimado} kg</strong></p>
              {montoCotizado !== null && (
                <p className="text-xs text-emerald-700 font-bold pt-1">
                  Monto referencial: S/ {montoCotizado.toFixed(2)}
                </p>
              )}
            </div>

            {/* Personas */}
            <div className="bg-slate-50 rounded-xl p-4 space-y-2">
              <div>
                <span className="text-xs uppercase font-bold text-slate-500 block">Remitente</span>
                <p className="font-semibold text-azul-noche">{remNombres}</p>
                <p className="text-xs text-slate-600">{remTipoDoc}: {remNumDoc} · Tel: {remTelefono}</p>
              </div>
              <div className="pt-2 border-t border-slate-200">
                <span className="text-xs uppercase font-bold text-slate-500 block">Destinatario</span>
                <p className="font-semibold text-azul-noche">{destNombres}</p>
                <p className="text-xs text-slate-600">{destTipoDoc}: {destNumDoc} · Tel: {destTelefono}</p>
              </div>
            </div>
          </div>

          {/* Casilla obligatoria de uso de datos */}
          <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 space-y-2">
            <label className="flex items-start gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={aceptoTerminos}
                onChange={(e) => {
                  setAceptoTerminos(e.target.checked);
                  setErroresCampos({});
                }}
                className="mt-1 w-4 h-4 text-rojo rounded border-slate-300 focus:ring-rojo"
              />
              <span className="text-xs sm:text-sm text-slate-700 leading-relaxed">
                Acepto el uso de mis datos personales ({remTipoDoc}: {remNumDoc}) y los del destinatario para la emisión del pre-registro, trazabilidad y prestación del servicio de transporte de encomiendas por parte de Expreso Marvisur.
              </span>
            </label>
            {erroresCampos.terminos && (
              <p className="text-xs text-rojo font-bold pl-7">{erroresCampos.terminos}</p>
            )}
          </div>

          {/* Botones de acción del paso 3 */}
          <div className="pt-4 flex flex-col sm:flex-row items-center justify-between gap-3 border-t border-slate-100">
            <button
              type="button"
              disabled={enviando}
              onClick={() => setPaso(2)}
              className="w-full sm:w-auto px-6 py-3 rounded-xl font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 transition-colors disabled:opacity-50"
            >
              ← Modificar Datos
            </button>
            <button
              type="button"
              disabled={enviando}
              onClick={handleConfirmarYEnviar}
              className="w-full sm:w-auto px-8 py-3.5 rounded-xl font-bold text-white bg-rojo hover:bg-rojo-profundo shadow-lg shadow-rojo/25 transition-all disabled:opacity-50 focus-visible:outline-rojo"
            >
              {enviando ? 'Generando Código Express...' : 'Confirmar y Generar Código Express'}
            </button>
          </div>
        </div>
      )}

      {/* ===================================================================== */}
      {/* PASO 4: PANTALLA DE ÉXITO                                             */}
      {/* ===================================================================== */}
      {paso === 4 && resultado && (
        <div className="space-y-6 animate-fadeIn">
          {/* Tarjeta imprimible de éxito */}
          <div className="bg-white rounded-3xl border border-slate-200 shadow-xl overflow-hidden print:border-none print:shadow-none">
            {/* Cabecera */}
            <div className="bg-gradient-to-r from-azul-noche to-azul-noche-suave text-white p-6 sm:p-8 text-center space-y-2">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-emerald-500/20 text-emerald-300 border border-emerald-400/30">
                ✓ Pre-registro Exitoso
              </span>
              <h2 className="text-xl sm:text-3xl font-black text-white">
                Tu Código Express
              </h2>
              <p className="text-xs sm:text-sm text-slate-300">
                Presenta este código o el código QR en la ventanilla para admitir tu envío al instante.
              </p>
            </div>

            <div className="p-6 sm:p-10 space-y-8 text-center">
              {/* Código Express en grande */}
              <div className="bg-slate-50 border-2 border-dashed border-slate-300 rounded-2xl p-6 max-w-md mx-auto space-y-3">
                <span className="text-[11px] uppercase tracking-widest text-slate-500 font-bold block">
                  Código de Admisión Rápida
                </span>
                <p className="text-3xl sm:text-4xl font-mono font-black text-azul-noche tracking-wider">
                  {resultado.codigoExpress}
                </p>
                <button
                  type="button"
                  onClick={copiarCodigo}
                  className="print:hidden px-4 py-2 rounded-lg text-xs font-bold text-rojo bg-rojo-claro hover:bg-rojo/20 transition-colors inline-flex items-center gap-1.5"
                >
                  {copiado ? '✓ ¡Código copiado!' : '📋 Copiar Código'}
                </button>
              </div>

              {/* QR Code */}
              {qrDataUrl && (
                <div className="flex flex-col items-center justify-center space-y-2">
                  <div className="p-3 bg-white rounded-2xl border border-slate-200 shadow-sm inline-block">
                    <img
                      src={qrDataUrl}
                      alt={`Código QR para ${resultado.codigoExpress}`}
                      className="w-48 h-48 sm:w-56 sm:h-56 object-contain"
                    />
                  </div>
                  <span className="text-xs text-slate-500 font-medium">
                    Escaneo óptico directo en ventanilla
                  </span>
                </div>
              )}

              {/* Datos de resumen */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 max-w-xl mx-auto text-left text-xs bg-slate-50 p-4 rounded-xl border border-slate-100">
                <div>
                  <span className="text-slate-400 font-bold uppercase block">Destino</span>
                  <p className="font-bold text-azul-noche mt-0.5">{resultado.sucursalDestino}</p>
                </div>
                <div>
                  <span className="text-slate-400 font-bold uppercase block">Monto Cotizado</span>
                  <p className="font-bold text-emerald-700 mt-0.5">
                    S/ {resultado.montoCotizado.toFixed(2)}
                  </p>
                </div>
                <div>
                  <span className="text-slate-400 font-bold uppercase block">Válido Hasta</span>
                  <p className="font-bold text-azul-noche mt-0.5">
                    {formatearFechaHora(resultado.fechaExpiracion)}
                  </p>
                </div>
              </div>

              {/* Instrucciones y advertencias */}
              <div className="space-y-3 max-w-xl mx-auto text-xs text-slate-600 bg-amber-50/70 border border-amber-200/80 rounded-xl p-4 text-left">
                <p className="font-bold text-amber-900 flex items-center gap-1.5">
                  ⚠️ Información Importante para Ventanilla:
                </p>
                <ul className="list-disc pl-5 space-y-1 text-amber-800">
                  <li>
                    <strong>Preséntalo en la agencia para registrar tu envío; no incluye el pago.</strong>
                  </li>
                  <li>
                    El pre-registro no es el envío: el peso y el contenido se confirman en ventanilla.
                  </li>
                  <li>
                    Si el código no se utiliza antes de su vencimiento (48 horas), expirará automáticamente.
                  </li>
                </ul>
              </div>

              {/* Botones de acción (ocultos al imprimir) */}
              <div className="print:hidden pt-4 flex flex-col sm:flex-row items-center justify-center gap-3">
                <button
                  type="button"
                  onClick={() => window.print()}
                  className="w-full sm:w-auto px-6 py-3 rounded-xl font-bold text-azul-noche bg-white border border-slate-300 hover:bg-slate-50 transition-colors shadow-sm inline-flex items-center justify-center gap-2"
                >
                  🖨️ Imprimir Comprobante
                </button>
                <button
                  type="button"
                  onClick={compartirWhatsApp}
                  className="w-full sm:w-auto px-6 py-3 rounded-xl font-bold text-white bg-emerald-600 hover:bg-emerald-700 transition-colors shadow-md shadow-emerald-600/20 inline-flex items-center justify-center gap-2"
                >
                  💬 Compartir por WhatsApp
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setResultado(null);
                    setPaso(1);
                    setPesoEstimado('');
                    setMontoCotizado(null);
                    setAceptoTerminos(false);
                    navigate('/pre-registro', { replace: true, state: {} });
                  }}
                  className="w-full sm:w-auto px-6 py-3 rounded-xl font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 transition-colors inline-flex items-center justify-center"
                >
                  Nuevo Pre-registro
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
