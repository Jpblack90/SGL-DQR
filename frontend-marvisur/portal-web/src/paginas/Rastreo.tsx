import React, { useState } from 'react';
import { consultarRastreo, descargarComprobantePdf } from '../api/cliente';
import { ApiError, RastreoPublicoDto, obtenerEtiquetaEstado } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Cargando } from '../componentes/Cargando';
import { LineaTiempo } from '../componentes/LineaTiempo';

export const Rastreo: React.FC = () => {
  const [comprobante, setComprobante] = useState('');
  const [documento, setDocumento] = useState('');
  const [erroresValidacion, setErroresValidacion] = useState<{ comprobante?: string; documento?: string }>({});

  const [cargando, setCargando] = useState(false);
  const [errorApi, setErrorApi] = useState<string | null>(null);
  const [reintentoSegundos, setReintentoSegundos] = useState<number | null>(null);
  const [resultado, setResultado] = useState<RastreoPublicoDto | null>(null);
  const [descargandoPdf, setDescargandoPdf] = useState(false);
  const [errorPdf, setErrorPdf] = useState<string | null>(null);

  const validarFormulario = (): boolean => {
    const errores: { comprobante?: string; documento?: string } = {};

    if (!comprobante.trim()) {
      errores.comprobante = 'El número de comprobante es obligatorio (ej. MV-DEMO-001).';
    }

    const docLimpio = documento.trim();
    if (!docLimpio) {
      errores.documento = 'El número de documento de identidad es obligatorio.';
    } else if (!/^\d{8}$|^\d{11}$/.test(docLimpio)) {
      errores.documento = 'Ingrese un DNI válido de 8 dígitos o un RUC de 11 dígitos numéricos.';
    }

    setErroresValidacion(errores);
    return Object.keys(errores).length === 0;
  };

  const manejarEnvio = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorApi(null);
    setReintentoSegundos(null);

    if (!validarFormulario()) {
      return;
    }

    setCargando(true);
    setResultado(null);

    try {
      // Consulta en vivo. NOTA: Por estricto requisito de privacidad,
      // NUNCA se persiste en localStorage.
      const datos = await consultarRastreo(comprobante, documento);
      setResultado(datos);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorApi(err.message);
        if (err.retryAfterSeconds) {
          setReintentoSegundos(err.retryAfterSeconds);
        }
      } else {
        setErrorApi('Ocurrió un error inesperado al consultar el seguimiento. Intente nuevamente.');
      }
    } finally {
      setCargando(false);
    }
  };

  const manejarDescargaPdf = async () => {
    if (!resultado) return;
    setErrorPdf(null);
    setDescargandoPdf(true);

    try {
      const blob = await descargarComprobantePdf(comprobante, documento);
      const urlBlob = window.URL.createObjectURL(blob);
      const enlace = document.createElement('a');
      enlace.href = urlBlob;
      enlace.download = `comprobante-${resultado.codigoComprobante}.pdf`;
      document.body.appendChild(enlace);
      enlace.click();
      document.body.removeChild(enlace);
      window.URL.revokeObjectURL(urlBlob);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorPdf(err.message);
      } else {
        setErrorPdf('No se pudo descargar el comprobante. Verifique su conexión e intente nuevamente.');
      }
    } finally {
      setDescargandoPdf(false);
    }
  };

  const limpiarBusqueda = () => {
    setErrorPdf(null);
    setDescargandoPdf(false);
    setComprobante('');
    setDocumento('');
    setErroresValidacion({});
    setErrorApi(null);
    setResultado(null);
  };

  return (
    <div className="max-w-3xl mx-auto space-y-8">
      {/* Encabezado */}
      <div className="text-center space-y-2">
        <h1 className="text-2xl sm:text-3xl font-extrabold text-azul-noche">
          Rastreo de Encomiendas
        </h1>
        <p className="text-sm text-slate-600 max-w-lg mx-auto">
          Ingresa el número de tu comprobante y el DNI o RUC del remitente o destinatario para verificar el estado de tu carga.
        </p>
      </div>

      {/* Formulario de búsqueda */}
      <div className="bg-white rounded-2xl p-6 sm:p-8 border border-slate-200 shadow-sm">
        <form onSubmit={manejarEnvio} noValidate className="space-y-5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label htmlFor="input-comprobante" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
                N° de Comprobante / Boleta *
              </label>
              <input
                id="input-comprobante"
                type="text"
                value={comprobante}
                onChange={(e) => {
                  setComprobante(e.target.value);
                  if (erroresValidacion.comprobante) {
                    setErroresValidacion((prev) => ({ ...prev, comprobante: undefined }));
                  }
                }}
                placeholder="Ej. MV-DEMO-001"
                disabled={cargando}
                className={`w-full px-4 py-3 rounded-xl border text-sm transition-colors ${
                  erroresValidacion.comprobante
                    ? 'border-red-400 bg-red-50/50 focus:border-red-500'
                    : 'border-slate-300 focus:border-rojo'
                }`}
                aria-invalid={!!erroresValidacion.comprobante}
                aria-describedby={erroresValidacion.comprobante ? 'error-comprobante' : undefined}
              />
              {erroresValidacion.comprobante && (
                <p id="error-comprobante" className="text-xs text-red-600 mt-1.5 font-medium">
                  {erroresValidacion.comprobante}
                </p>
              )}
            </div>

            <div>
              <label htmlFor="input-documento" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
                DNI o RUC (Remitente / Destinatario) *
              </label>
              <input
                id="input-documento"
                type="text"
                value={documento}
                maxLength={11}
                onChange={(e) => {
                  setDocumento(e.target.value.replace(/\D/g, ''));
                  if (erroresValidacion.documento) {
                    setErroresValidacion((prev) => ({ ...prev, documento: undefined }));
                  }
                }}
                placeholder="8 dígitos (DNI) o 11 (RUC)"
                disabled={cargando}
                className={`w-full px-4 py-3 rounded-xl border text-sm transition-colors ${
                  erroresValidacion.documento
                    ? 'border-red-400 bg-red-50/50 focus:border-red-500'
                    : 'border-slate-300 focus:border-rojo'
                }`}
                aria-invalid={!!erroresValidacion.documento}
                aria-describedby={erroresValidacion.documento ? 'error-documento' : undefined}
              />
              {erroresValidacion.documento && (
                <p id="error-documento" className="text-xs text-red-600 mt-1.5 font-medium">
                  {erroresValidacion.documento}
                </p>
              )}
            </div>
          </div>

          <div className="flex flex-col sm:flex-row items-center gap-3 pt-2">
            <button
              type="submit"
              disabled={cargando}
              className="w-full sm:flex-1 py-3.5 px-6 rounded-xl font-bold text-white bg-rojo hover:bg-rojo-profundo shadow-md shadow-rojo/20 transition-all disabled:opacity-50 disabled:cursor-not-allowed focus-visible:outline-rojo"
            >
              {cargando ? 'Buscando encomienda...' : 'Consultar Estado'}
            </button>

            {resultado && (
              <button
                type="button"
                onClick={limpiarBusqueda}
                className="w-full sm:w-auto py-3.5 px-5 rounded-xl font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 transition-colors text-sm"
              >
                Nueva Búsqueda
              </button>
            )}
          </div>
        </form>
      </div>

      {/* Estado de carga */}
      {cargando && <Cargando texto="Localizando tu encomienda en el sistema..." />}

      {/* Estado de error */}
      {errorApi && (
        <Alerta
          tipo={reintentoSegundos ? 'advertencia' : 'error'}
          titulo={reintentoSegundos ? 'Límite de Consultas' : 'No se pudo completar la búsqueda'}
          mensaje={
            <div>
              <p>{errorApi}</p>
              {reintentoSegundos && (
                <p className="mt-1 text-xs opacity-90">
                  Protección de privacidad activa. Espere el tiempo indicado para volver a consultar.
                </p>
              )}
            </div>
          }
        />
      )}

      {/* Resultado del rastreo */}
      {resultado && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden animate-fadeIn">
          {/* Cabecera del resultado */}
          <div className="bg-gradient-to-r from-azul-noche to-azul-noche-suave p-6 text-white flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div>
              <span className="text-[11px] uppercase tracking-widest text-slate-300 font-semibold block">
                Comprobante Oficial
              </span>
              <h2 className="text-xl sm:text-2xl font-black tracking-wide text-white">
                {resultado.codigoComprobante}
              </h2>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              {/* Badge de Estado Logístico */}
              <span className="px-3 py-1 rounded-full text-xs font-bold tracking-wide uppercase bg-white/10 text-white border border-white/20">
                {obtenerEtiquetaEstado(resultado.estadoLogistico)}
              </span>

              {/* Badge de Estado de Pago */}
              <span
                className={`px-3 py-1 rounded-full text-xs font-bold tracking-wide uppercase ${
                  resultado.estadoPago === 'PAGADO'
                    ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-400/30'
                    : 'bg-amber-500/20 text-amber-300 border border-amber-400/30'
                }`}
              >
                {resultado.estadoPago === 'PAGADO' ? 'PAGADO' : 'PAGO PENDIENTE'}
              </span>

              {/* Botón Descargar comprobante (PDF) */}
              <button
                type="button"
                onClick={manejarDescargaPdf}
                disabled={descargandoPdf}
                className="px-3.5 py-1.5 rounded-full text-xs font-bold tracking-wide bg-white text-azul-noche hover:bg-slate-100 transition-colors shadow-sm disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1.5"
              >
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                {descargandoPdf ? 'Descargando...' : 'Descargar comprobante (PDF)'}
              </button>
            </div>
          </div>

          {errorPdf && (
            <div className="p-4 bg-red-50 border-b border-red-200">
              <Alerta tipo="error" titulo="Error al descargar comprobante" mensaje={errorPdf} />
            </div>
          )}

          <div className="p-6 sm:p-8 space-y-6">
            {/* Aviso si el pago está PENDIENTE */}
            {resultado.estadoPago === 'PENDIENTE' && (
              <Alerta
                tipo="advertencia"
                titulo="Pago Contra Entrega Pendiente"
                mensaje={
                  <span>
                    El envío tiene un importe pendiente de <strong>S/. {resultado.montoTotal.toFixed(2)}</strong>. Recuerde que el cobro se realiza en la ventanilla de la agencia de destino antes de autorizar la entrega física.
                  </span>
                }
              />
            )}

            {/* Datos de origen, destino y personas */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pb-6 border-b border-slate-100 text-sm">
              <div className="bg-slate-50 rounded-xl p-4 space-y-1">
                <span className="text-xs uppercase font-bold text-slate-500 block">Origen</span>
                <p className="font-bold text-azul-noche">{resultado.ciudadOrigen}</p>
                <p className="text-xs text-slate-600">{resultado.sucursalOrigen}</p>
                <p className="text-xs text-slate-500 pt-1">
                  <span className="font-medium text-slate-700">Remitente:</span> {resultado.nombreRemitenteEnmascarado}
                </p>
              </div>

              <div className="bg-slate-50 rounded-xl p-4 space-y-1">
                <span className="text-xs uppercase font-bold text-slate-500 block">Destino</span>
                <p className="font-bold text-azul-noche">{resultado.ciudadDestino}</p>
                <p className="text-xs text-slate-600">{resultado.sucursalDestino}</p>
                <p className="text-xs text-slate-500 pt-1">
                  <span className="font-medium text-slate-700">Destinatario:</span> {resultado.nombreDestinatarioEnmascarado}
                </p>
              </div>
            </div>

            {/* Línea de tiempo vertical */}
            <div>
              <h3 className="text-sm font-bold text-azul-noche uppercase tracking-wider mb-4">
                Línea de Tiempo del Envío
              </h3>
              <LineaTiempo eventos={resultado.lineaDeTiempo} />
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
