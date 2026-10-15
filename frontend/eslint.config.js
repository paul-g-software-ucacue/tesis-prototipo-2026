// ESLint del frontend (B00-H06, D-29, ADR-019): reglas recomendadas de JavaScript, TypeScript con
// tipos, React y accesibilidad (jsx-a11y). El token vive solo en memoria (D-51), así que el
// almacenamiento del navegador está prohibido en src/.
import js from '@eslint/js';
import prettier from 'eslint-config-prettier';
import jsxA11y from 'eslint-plugin-jsx-a11y';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import globals from 'globals';
import tseslint from 'typescript-eslint';

const almacenamiento =
  'El token vive solo en memoria (D-51): no se usa el almacenamiento del navegador.';

export default tseslint.config(
  { ignores: ['dist', 'coverage', 'playwright-report', 'test-results', 'src/api/generado'] },
  js.configs.recommended,
  ...tseslint.configs.recommendedTypeChecked,
  {
    languageOptions: {
      parserOptions: { projectService: true, tsconfigRootDir: import.meta.dirname },
    },
  },
  {
    files: ['src/**/*.{ts,tsx}'],
    languageOptions: { globals: globals.browser },
    plugins: { 'react-hooks': reactHooks, 'react-refresh': reactRefresh },
    extends: [jsxA11y.flatConfigs.recommended],
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['error', { allowConstantExport: true }],
      'no-console': 'error',
      'no-restricted-globals': [
        'error',
        { name: 'localStorage', message: almacenamiento },
        { name: 'sessionStorage', message: almacenamiento },
      ],
      'no-restricted-properties': [
        'error',
        { object: 'window', property: 'localStorage', message: almacenamiento },
        { object: 'window', property: 'sessionStorage', message: almacenamiento },
        { object: 'document', property: 'cookie', message: almacenamiento },
      ],
    },
  },
  {
    // Las pruebas leen el almacenamiento del navegador para comprobar que está vacío (SC-008).
    files: ['src/**/*.test.{ts,tsx}', 'src/pruebas/**'],
    rules: {
      'react-refresh/only-export-components': 'off',
      'no-restricted-globals': 'off',
      'no-restricted-properties': 'off',
    },
  },
  // Los archivos JavaScript (esta configuración y los scripts) no se revisan con tipos.
  { files: ['**/*.{js,mjs}'], extends: [tseslint.configs.disableTypeChecked] },
  {
    files: ['*.config.{ts,js}', 'e2e/**', 'scripts/**'],
    languageOptions: { globals: globals.node },
  },
  prettier,
);
