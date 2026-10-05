import daisyui from "daisyui";

/** @type {import('tailwindcss').Config} */
const config = {
  darkMode: ["class", "[data-theme='night']"],
  content: [
    "./app/**/*.{js,ts,jsx,tsx}",
    "./pages/**/*.{js,ts,jsx,tsx}",
    "./components/**/*.{js,ts,jsx,tsx}"
  ],
  theme: {
    extend: {
      colors: {
        spat: {
          blue: "#0b3d74",
          navy: "#081c30",
          sky: "#3b82f6",
          mist: "#edf4ff",
        },
      },
    },
  },
  plugins: [daisyui],
  daisyui: {
    themes: ["light", "dark"],
    darkTheme: "dark",
    base: true,
    styled: true,
    utils: true,
  },
};

export default config;