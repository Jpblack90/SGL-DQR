import React, { useState } from 'react';

interface LogoProps {
  className?: string;
  tamano?: 'sm' | 'md' | 'lg';
}

export const Logo: React.FC<LogoProps> = ({ className = '', tamano = 'md' }) => {
  const [errorImagen, setErrorImagen] = useState(false);

  const dimensiones = {
    sm: { img: 'h-8', box: 'w-8 h-8 text-base', text: 'text-lg' },
    md: { img: 'h-10', box: 'w-10 h-10 text-xl', text: 'text-xl' },
    lg: { img: 'h-14', box: 'w-14 h-14 text-2xl', text: 'text-2xl' },
  }[tamano];

  if (!errorImagen) {
    return (
      <div className={`flex items-center gap-2 ${className}`}>
        <img
          src="/logo-marvisur.png"
          alt="Expreso Marvisur"
          className={`${dimensiones.img} w-auto object-contain`}
          onError={() => setErrorImagen(true)}
        />
      </div>
    );
  }

  // Placeholder accesible con isotipo cuadrado rojo con la letra M y texto MARVISUR
  return (
    <div className={`flex items-center gap-2.5 ${className}`} aria-label="Logo Expreso Marvisur">
      <div
        className={`${dimensiones.box} bg-rojo text-white font-black rounded-lg flex items-center justify-center shadow-sm select-none`}
        aria-hidden="true"
      >
        M
      </div>
      <div className="flex flex-col leading-tight">
        <span className={`font-black tracking-wider text-rojo ${dimensiones.text}`}>
          MARVISUR
        </span>
        <span className="text-[10px] tracking-widest text-azul-noche uppercase font-semibold">
          Expreso
        </span>
      </div>
    </div>
  );
};
