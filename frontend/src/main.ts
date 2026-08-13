import { bootstrapApplication } from '@angular/platform-browser';

import { App } from './app/app';
import { createAppConfig } from './app/app.config';
import { loadRuntimeConfig } from './app/core/config/runtime-config';
import { initializeDocumentTheme } from './app/core/theme/theme.service';
import { renderBootstrapError } from './bootstrap-error';

initializeDocumentTheme(document);

loadRuntimeConfig()
  .then((runtimeConfig) => bootstrapApplication(App, createAppConfig(runtimeConfig)))
  .catch((error) => {
    console.error(error);
    renderBootstrapError(error);
  });
