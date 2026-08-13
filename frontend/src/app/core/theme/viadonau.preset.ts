/**
 * Viadonau brand preset for PrimeNG.
 *
 * Anchors the corporate identity (Leitfarbe / Pendantfarbe / Auszeichnungsfarbe Cyan)
 * defined in viadonau's CD-Manual into the PrimeNG token system. Built on top of Aura.
 *
 * Source colors (viadonau CD-Manual, page 14):
 *   Leitfarbe   #4691af  RGB  70 145 175  (water blue, primary identity color)
 *   Pendant     #003c50  RGB   0  60  80  (deep navy, paired with Leitfarbe for depth)
 *   Cyan        #00a0e1  RGB   0 160 225  (Auszeichnungsfarbe, accent / spotlight only)
 *
 * Ramps are generated in OKLCH so neighbouring steps share the same hue and chroma intent.
 * Token "500" anchors to the literal brand hex; surrounding steps interpolate lightness.
 *
 * AA contrast note:
 *   Leitfarbe #4691af on #ffffff = 3.36:1 — large-text only.
 *   The semantic `primary.color` token is therefore mapped to `{primary.600}` (≈4.6:1 on
 *   white) so primary-action button labels and small primary-coloured copy meet WCAG AA.
 *   The literal brand hex (#4691af) is retained as `{primary.500}` and used for the logo
 *   asset, chart-line strokes and non-text accents where contrast is not body-text relevant.
 */
import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

export const ViadonauPreset = definePreset(Aura, {
  primitive: {
    leitblau: {
      50: 'oklch(97% 0.012 233)',
      100: 'oklch(93% 0.024 233)',
      200: 'oklch(87% 0.044 233)',
      300: 'oklch(80% 0.058 233)',
      400: 'oklch(72% 0.07 233)',
      500: '#4691af',
      600: 'oklch(53% 0.072 233)',
      700: 'oklch(45% 0.066 234)',
      800: 'oklch(37% 0.058 234)',
      900: 'oklch(30% 0.05 234)',
      950: 'oklch(22% 0.045 234)',
    },
    pendant: {
      0: '#ffffff',
      50: 'oklch(97.8% 0.005 233)',
      100: 'oklch(95% 0.008 233)',
      200: 'oklch(90% 0.012 233)',
      300: 'oklch(83% 0.016 233)',
      400: 'oklch(70% 0.02 233)',
      500: 'oklch(56% 0.024 234)',
      600: 'oklch(45% 0.03 234)',
      700: 'oklch(36% 0.04 234)',
      800: '#003c50',
      900: 'oklch(20% 0.05 234)',
      950: 'oklch(13% 0.04 234)',
    },
    cyanAccent: {
      500: '#00a0e1',
    },
  },
  semantic: {
    primary: {
      50: '{leitblau.50}',
      100: '{leitblau.100}',
      200: '{leitblau.200}',
      300: '{leitblau.300}',
      400: '{leitblau.400}',
      500: '{leitblau.500}',
      600: '{leitblau.600}',
      700: '{leitblau.700}',
      800: '{leitblau.800}',
      900: '{leitblau.900}',
      950: '{leitblau.950}',
    },
    focusRing: {
      width: '2px',
      style: 'solid',
      color: '{primary.500}',
      offset: '2px',
    },
    colorScheme: {
      light: {
        surface: {
          0: '#ffffff',
          50: '{pendant.50}',
          100: '{pendant.100}',
          200: '{pendant.200}',
          300: '{pendant.300}',
          400: '{pendant.400}',
          500: '{pendant.500}',
          600: '{pendant.600}',
          700: '{pendant.700}',
          800: '{pendant.800}',
          900: '{pendant.900}',
          950: '{pendant.950}',
        },
        primary: {
          color: '{primary.600}',
          contrastColor: '#ffffff',
          hoverColor: '{primary.700}',
          activeColor: '{primary.800}',
        },
        highlight: {
          background: '{primary.50}',
          focusBackground: '{primary.100}',
          color: '{primary.700}',
          focusColor: '{primary.800}',
        },
        text: {
          color: '{surface.800}',
          hoverColor: '{surface.900}',
          mutedColor: '{surface.600}',
          hoverMutedColor: '{surface.700}',
        },
      },
      dark: {
        surface: {
          0: '#ffffff',
          50: '{pendant.50}',
          100: '{pendant.100}',
          200: '{pendant.200}',
          300: '{pendant.300}',
          400: '{pendant.400}',
          500: '{pendant.500}',
          600: '{pendant.600}',
          700: '{pendant.700}',
          800: '{pendant.800}',
          900: '{pendant.900}',
          950: '{pendant.950}',
        },
        primary: {
          color: '{primary.400}',
          contrastColor: '{surface.950}',
          hoverColor: '{primary.300}',
          activeColor: '{primary.200}',
        },
        highlight: {
          background: 'color-mix(in oklch, {primary.400}, transparent 82%)',
          focusBackground: 'color-mix(in oklch, {primary.400}, transparent 72%)',
          color: '{surface.0}',
          focusColor: '{surface.0}',
        },
        text: {
          color: '{surface.0}',
          hoverColor: '{surface.0}',
          mutedColor: '{surface.400}',
          hoverMutedColor: '{surface.300}',
        },
      },
    },
  },
});
