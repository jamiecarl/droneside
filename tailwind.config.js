/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{css,xml,html,vue,svelte,ts,tsx}'],
  // use the .ns-dark class to control dark mode (applied by NativeScript) - since 'media' (default) is not supported.
  darkMode: ['class', '.ns-dark'],
  theme: {
    extend: {
      colors: {
        'ds-base': '#0A0B0D',
        'ds-surface': '#14171C',
        'ds-surface2': '#0E1116',
        'ds-line': '#1F242B',
        'ds-line-soft': '#16191E',
        'ds-cyan': '#00D4FF',
        'ds-cyan-surface': '#0E2027',
        'ds-cyan-border': '#12414E',
        'ds-holeshot': '#FFB020',
        'ds-dnf': '#FF5C7A',
        'ds-ink': '#F2F4F7',
        'ds-ink2': '#E7EAEE',
        'ds-muted': '#9AA1AC',
        'ds-muted2': '#8B939E',
        'ds-faint': '#3B424B',
      },
    },
  },
  plugins: [],
  corePlugins: {
    preflight: false, // disables browser-specific resets
  },
};
