import React, { useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { Logo } from './Logo';

export const Layout: React.FC = () => {
  const [menuAbierto, setMenuAbierto] = useState(false);

  const enlacesNav = [
    { to: '/', texto: 'Inicio' },
    { to: '/cotiza', texto: 'Cotiza' },
    { to: '/pre-registro', texto: 'Pre-registra' },
    { to: '/rastreo', texto: 'Rastrea tu Envío' },
    { to: '/billetera', texto: 'Billetera Digital' },
    { to: '/agencias', texto: 'Nuestras Agencias' },
  ];

  const claseEnlace = ({ isActive }: { isActive: boolean }) =>
    `text-sm font-semibold transition-colors duration-150 px-3 py-2 rounded-lg ${
      isActive
        ? 'text-rojo bg-rojo-claro/50'
        : 'text-azul-noche-mutado hover:text-rojo hover:bg-slate-100'
    }`;

  const claseEnlaceMovil = ({ isActive }: { isActive: boolean }) =>
    `block text-base font-semibold px-4 py-3 rounded-xl transition-colors ${
      isActive ? 'bg-rojo text-white' : 'text-azul-noche hover:bg-slate-100'
    }`;

  return (
    <div className="flex flex-col min-h-screen">
      {/* Cabecera (oculta al imprimir) */}
      <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200 shadow-xs print:hidden">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 h-20 flex items-center justify-between">
          <Link
            to="/"
            className="flex items-center gap-2 focus-visible:outline-rojo rounded-lg"
            aria-label="Ir a la página de inicio de Expreso Marvisur"
            onClick={() => setMenuAbierto(false)}
          >
            <Logo tamano="md" />
          </Link>

          {/* Navegación de escritorio */}
          <nav className="hidden md:flex items-center gap-1.5" aria-label="Navegación principal">
            {enlacesNav.map((enlace) => (
              <NavLink key={enlace.to} to={enlace.to} className={claseEnlace}>
                {enlace.texto}
              </NavLink>
            ))}
          </nav>

          {/* Botón hamburguesa móvil */}
          <button
            type="button"
            className="md:hidden p-2 rounded-lg text-azul-noche hover:bg-slate-100 focus-visible:outline-rojo"
            aria-expanded={menuAbierto}
            aria-controls="menu-movil"
            aria-label={menuAbierto ? 'Cerrar menú' : 'Abrir menú de navegación'}
            onClick={() => setMenuAbierto(!menuAbierto)}
          >
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              {menuAbierto ? (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              ) : (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              )}
            </svg>
          </button>
        </div>

        {/* Menú desplegable móvil */}
        {menuAbierto && (
          <nav
            id="menu-movil"
            className="md:hidden border-b border-slate-200 bg-white px-4 pt-2 pb-6 space-y-1 shadow-lg animate-fadeIn"
            aria-label="Navegación móvil"
          >
            {enlacesNav.map((enlace) => (
              <NavLink
                key={enlace.to}
                to={enlace.to}
                className={claseEnlaceMovil}
                onClick={() => setMenuAbierto(false)}
              >
                {enlace.texto}
              </NavLink>
            ))}
          </nav>
        )}
      </header>

      {/* Contenido principal */}
      <main className="flex-1 w-full max-w-6xl mx-auto px-4 sm:px-6 py-6 sm:py-10">
        <Outlet />
      </main>

      {/* Pie de página (oculto al imprimir) */}
      <footer className="bg-azul-noche text-slate-300 py-10 mt-auto border-t border-slate-800 print:hidden">
        <div className="max-w-6xl mx-auto px-4 sm:px-6">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
            <div className="space-y-3">
              <Logo tamano="sm" />
              <p className="text-xs text-slate-400 leading-relaxed">
                Logística y transporte de encomiendas interprovinciales con tecnología Doble QR de máxima confianza en el Perú.
              </p>
            </div>

            <div>
              <h4 className="text-sm font-semibold text-white mb-3 uppercase tracking-wider">
                Servicios al Cliente
              </h4>
              <ul className="space-y-2 text-xs">
                <li>
                  <Link to="/cotiza" className="hover:text-white transition-colors">
                    Cotización de envíos
                  </Link>
                </li>
                <li>
                  <Link to="/pre-registro" className="hover:text-white transition-colors">
                    Pre-registro Código Express
                  </Link>
                </li>
                <li>
                  <Link to="/rastreo" className="hover:text-white transition-colors">
                    Rastreo en tiempo real
                  </Link>
                </li>
                <li>
                  <Link to="/billetera" className="hover:text-white transition-colors">
                    Billetera Digital con QR2
                  </Link>
                </li>
                <li>
                  <Link to="/agencias" className="hover:text-white transition-colors">
                    Red de agencias
                  </Link>
                </li>
              </ul>
            </div>

            <div>
              <h4 className="text-sm font-semibold text-white mb-3 uppercase tracking-wider">
                Seguridad y Garantía
              </h4>
              <ul className="space-y-2 text-xs text-slate-400">
                <li>Validación Cruzada QR1 + QR2</li>
                <li>Protección contra suplantación</li>
                <li>Fricción Cero sin contraseñas</li>
                <li>Trazabilidad descentralizada</li>
              </ul>
            </div>

            <div>
              <h4 className="text-sm font-semibold text-white mb-3 uppercase tracking-wider">
                Atención y Central
              </h4>
              <p className="text-xs text-slate-400 mb-2">
                Central de Encomiendas: Lima, Arequipa, Huancayo y todo el Perú.
              </p>
              <p className="text-xs font-mono text-rojo-claro">
                soporte@marvisur.pe
              </p>
            </div>
          </div>

          <div className="pt-6 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-slate-500">
            <p>© {new Date().getFullYear()} Expreso Marvisur S.A. — Sistema de Gestión Logística con Doble QR (SGL-DQR).</p>
            <p className="text-[11px]">Plataforma optimizada para dispositivos móviles (Fricción Cero).</p>
          </div>
        </div>
      </footer>
    </div>
  );
};
